package com.vish.fno.reader.service;

import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.util.JsonUtils;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.*;
import com.zerodhatech.ticker.OnOrderUpdate;
import com.zerodhatech.ticker.OnTicks;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;

import static com.vish.fno.reader.util.OrderUtils.createMarketOrderWithParameters;
import static com.vish.fno.util.Constants.NIFTY_50;
import static com.vish.fno.util.Constants.NIFTY_BANK;
import static com.vish.fno.util.Constants.MINUTE;
import static com.vish.fno.util.JsonUtils.getFormattedObject;
import static com.vish.fno.util.TimeUtils.getClosingTime;
import static com.vish.fno.util.TimeUtils.getOpeningTime;

@Slf4j
@SuppressWarnings({"PMD.LooseCoupling", "PMD.TooManyStaticImports"})
public class KiteService {

    private static final String EQUITY = "equity";

    @Getter(AccessLevel.PACKAGE)
    private final KiteConnect kiteSdk;
    private final String apiSecret;
    private final boolean placeOrders;
    private final InstrumentCache instrumentCache;
    private final HistoricalDataService historicalDataService;
    private final KiteWebSocket kiteWebSocket;
    @Getter
    private boolean initialised;

    public KiteService(String apiSecret,
                       String apiKey,
                       String userId,
                       List<String> nifty100Symbols,
                       boolean placeOrders,
                       boolean connectToWebSocket) {
        this.kiteSdk = kiteSdk(apiKey, userId);
        this.apiSecret = apiSecret;
        this.placeOrders = placeOrders;
        this.instrumentCache = new InstrumentCache(nifty100Symbols, this);
        this.historicalDataService = new HistoricalDataService(this, this.instrumentCache);
        this.kiteWebSocket = new KiteWebSocket(connectToWebSocket, instrumentCache);
    }

    public void authenticate(String requestToken) {
        try {
            User user = kiteSdk.generateSession(requestToken, apiSecret);
            kiteSdk.setAccessToken(user.accessToken);
            kiteSdk.setPublicToken(user.publicToken);
            addSessionExpiryHook();
            kiteWebSocket.initialize(kiteSdk);
            Margin margins = kiteSdk.getMargins(EQUITY);
            log.info("available_cash={}", margins.available.cash);
            log.info("utilised_debits={}", margins.utilised.debits);
            initialised = true;
        } catch (KiteException | IOException e) {
            log.error("Error while Initialising KiteService", e);
        }
    }

    public HistoricalData getEntireDayHistoricalData(Date fromDate, Date toDate, String symbol, String interval) {
        return historicalDataService.getEntireDayHistoricalData(fromDate, toDate, symbol, interval);
    }

    public HistoricalData getHistoricalData(Date from, Date to, String symbol, String interval, boolean continuous) {
        return historicalDataService.getHistoricalData(from, to, symbol, interval, continuous);
    }

    public String getITMStock(String indexSymbol, double price, boolean isCall) {
        return OptionPriceUtils.getITMStock(indexSymbol, price, isCall, instrumentCache.getInstruments());
    }

    public void setOnTickerArrivalListener(OnTicks onTickerArrivalListener) {
        if(onTickerArrivalListener == null) {
            return;
        }
        this.kiteWebSocket.setOnTickerArrivalListener(onTickerArrivalListener);
    }

    public void setOnOrderUpdateListener(OnOrderUpdate onOrderUpdateListener) {
        if(onOrderUpdateListener == null) {
            return;
        }
        this.kiteWebSocket.setOnOrderUpdateListener(onOrderUpdateListener);
    }

    public Long getInstrument(String symbol) {
        return instrumentCache.getInstrument(symbol);
    }

    public String getSymbol(long token) {
        return instrumentCache.getSymbol(token);
    }

    public void appendWebSocketSymbolsList(List<String> symbols, boolean addFutures) {
        kiteWebSocket.appendWebSocketSymbolsList(symbols, addFutures);
    }

    public List<Map<String, String>> getFilteredInstruments() {
        return instrumentCache.getAllInstruments();
    }

    List<Instrument> getAllInstruments() {
        List<Instrument> instruments = null;
        try {
            instruments = kiteSdk.getInstruments();
            log.info("Loaded instrument cache from Kite server");
        } catch (JSONException | IOException | KiteException e) {
            log.error("Failed to load instruments from Kite server", e);
        }
        return instruments;
    }

    public boolean isExpiryDayForOption(String optionSymbol, Date date) {
        return instrumentCache.isExpiryDayForOption(optionSymbol, date);
    }

    public Order placeOptionOrder(OrderParams orderParams) {
        Order order = null;
        try {
            log.info("placing order with params : {}", orderParams);
            order = kiteSdk.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            log.info("order id: {}", order.orderId);
        } catch (KiteException ke) {
            log.error("KiteException occurred while placing order, code: {}, message: {}, order: {}",
                    ke.code, ke.message, getFormattedObject(orderParams), ke);
        } catch (JSONException | IOException e) {
            log.error("Error occurred while placing order", e);
        }
        return order;
    }

    public Optional<KiteOpenOrder> buyOrder(String symbol, int orderSize, String tag, boolean isPlaceOrder) {
        log.info("Creating buy order with quantity : {}, symbol : {} , isPlaceOrder: {}", orderSize, symbol, isPlaceOrder);
        return placeOrder(symbol, orderSize, tag, Constants.TRANSACTION_TYPE_BUY, isPlaceOrder);
    }

    // TODO: verify there is an existing order before placing a sell order
    public Optional<KiteOpenOrder> sellOrder(String symbol, double price, int orderSize, String tag, boolean isPlaceOrder) {
        log.info("Creating sell order with quantity : {}, symbol : {}, price : {} ", orderSize, symbol, price);
        logExistingOrdersAndPositions(symbol, tag);
        return placeOrder(symbol, orderSize, tag, Constants.TRANSACTION_TYPE_SELL, isPlaceOrder);
    }

    public List<Order> getOrders() {
        try {
            return this.kiteSdk.getOrders();
        } catch (KiteException e) {
            log.error("Failed to get orders, error code: {}, error message: {}", e.code, e.message, e);
        } catch (IOException e) {
            log.error("Failed to get orders, error: {}", e.getMessage(), e);
        }
        return List.of();
    }

    public Map<String, List<Position>> getPositions() {
        try {
            return this.kiteSdk.getPositions();
        } catch (KiteException e) {
            log.error("Failed to get positions, error code: {}, error message: {}", e.code, e.message, e);
        } catch (IOException e) {
            log.error("Failed to get positions, error: {}", e.getMessage(), e);
        }
        return Map.of();
    }

    // TODO: Return optional of order or order.id
    // or send error code with failure reason to wait(kite not initialised) or trigger a different type of order(MARKET/LIMIT)
    private Optional<KiteOpenOrder> placeOrder(String symbol, int orderSize, String tag, String transactionType, boolean isPlaceOrder) {
        if(!isInitialised()) {
            log.warn("Not placing order as the kite service is not initialized yet.");
            return Optional.of(buildUnsuccessfulKiteOrder());
        }

        if (!isPlaceOrder) {
            log.warn("Not placing orders as it is not enabled or allowed currently");
            return Optional.of(buildSuccessfulKiteTestOrder());
        }

        if (!placeOrders) {
            log.warn("Not placing orders as it is turned off by configuration");
            return Optional.of(buildSuccessfulKiteTestOrder());
        }
        Order order;
        try {
            OrderParams orderParams = createMarketOrderWithParameters(symbol, orderSize, transactionType, tag);
            order = kiteSdk.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            log.debug("order placed successfully with id: {}", order.orderId);
        } catch (KiteException e) {
            log.error("KiteException occurred while placing order, code: {}, message: {}", e.code, e.message);
            return Optional.of(KiteOpenOrder.builder()
                    .exceptionCode(e.code)
                    .exceptionMessage(e.message)
                    .isOrderPlaced(false)
                    .build());
        } catch (JSONException | IOException e) {
            log.error("Error occurred while placing order", e);
            return Optional.of(buildUnsuccessfulKiteOrder());
        }
        return Optional.of(KiteOpenOrder.builder()
                .order(order)
                .isOrderPlaced(true)
                .build());
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public boolean appendIndexITMOptions() {
        if(kiteWebSocket.isConnectToWebSocket()) {
            try {
                List<String> indicesITMOptionSymbols = getITMIndexSymbols();
                appendWebSocketSymbolsList(indicesITMOptionSymbols, false);
            } catch (Exception e) {
                log.error("Failed to get the ITM option symbols", e);
                return false;
            }
        }
        return true;
    }

    @NotNull
    private KiteOpenOrder buildUnsuccessfulKiteOrder() {
        return KiteOpenOrder.builder().isOrderPlaced(false).build();
    }

    @NotNull
    private KiteOpenOrder buildSuccessfulKiteTestOrder() {
        return KiteOpenOrder.builder().isOrderPlaced(true).build();
    }


    private KiteConnect kiteSdk(String apiKey, String userId) {
        KiteConnect kiteConnect = new KiteConnect(apiKey, true);
        kiteConnect.setUserId(userId);
        kiteConnect.setSessionExpiryHook(() -> log.info("session expired"));
        return kiteConnect;
    }
    private List<String> getITMIndexSymbols() {
        List<String> indexOptionSymbols = new ArrayList<>();
        HistoricalData niftyData = getEntireDayHistoricalData(getOpeningTime(), getClosingTime(), NIFTY_50, MINUTE);
        HistoricalData bnfData = getEntireDayHistoricalData(getOpeningTime(), getClosingTime(), NIFTY_BANK, MINUTE);
        getITMOptionSymbols(niftyData, indexOptionSymbols, NIFTY_50);
        getITMOptionSymbols(bnfData, indexOptionSymbols, NIFTY_BANK);
        return indexOptionSymbols;
    }

    private void getITMOptionSymbols(HistoricalData data, List<String> indicesOptionSymbols, String index) {
        double openPrice = data.dataArrayList.get(0).open;
        indicesOptionSymbols.add(getITMStock(index, openPrice, true));
        indicesOptionSymbols.add(getITMStock(index, openPrice, false));
    }

    private void addSessionExpiryHook() {
        kiteSdk.setSessionExpiryHook(() -> log.info("kite session expired"));
    }

    private void logExistingOrdersAndPositions(String symbol, String tag) {
        List<String> orders = getOrders()
                .stream()
                .filter(o -> o.tradingSymbol.equals(symbol))
                .filter(o -> o.tag.equals(tag))
                .map(JsonUtils::getFormattedObject)
                .toList();

        List<String> netPositions = getPositions().get("net")
                .stream()
                .filter(o -> o.tradingSymbol.equals(symbol))
                .map(JsonUtils::getFormattedObject)
                .toList();

        List<String> dayPositions = getPositions().get("day")
                .stream()
                .filter(o -> o.tradingSymbol.equals(symbol))
                .map(JsonUtils::getFormattedObject)
                .toList();

        log.info("Existing orders for same symbol: {}", orders);
        log.info("Existing netPositions for same symbol: {}", netPositions);
        log.info("Existing dayPositions for same symbol: {}", dayPositions);
    }
}
