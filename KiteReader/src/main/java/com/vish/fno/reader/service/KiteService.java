package com.vish.fno.reader.service;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.*;
import com.zerodhatech.ticker.OnTicks;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.vish.fno.reader.util.KiteUtils.*;
import static com.vish.fno.reader.util.OrderUtils.createMarketOrderWithParameters;

@Slf4j
@SuppressWarnings({"PMD.RedundantFieldInitializer", "PMD.LooseCoupling"})
public class KiteService {
    @Getter(AccessLevel.PACKAGE)
    private final KiteConnect kiteSdk;
    private final String apiSecret;
    private final boolean placeOrders;
    private final InstrumentCache instrumentCache;
    private final HistoricalDataService historicalDataService;
    @Getter
    private boolean initialised = false;
    private final boolean connectToWebSocket;
    private KiteWebSocket kiteWebSocket;
    @Setter
    private OnTicks onTickerArrivalListener;
    private final ArrayList<Long> webSocketTokensToSubscribe;

    public KiteService(String apiSecret,
                       String apiKey,
                       String userId,
                       List<String> nifty100Symbols,
                       boolean placeOrders,
                       boolean connectToWebSocket) {
        this.kiteSdk = kiteSdk(apiKey, userId);
        this.apiSecret = apiSecret;
        this.placeOrders = placeOrders;
        this.connectToWebSocket = connectToWebSocket;
        this.instrumentCache = new InstrumentCache(nifty100Symbols, this);
        this.historicalDataService = new HistoricalDataService(this, this.instrumentCache);
        this.webSocketTokensToSubscribe = new ArrayList<>();
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

    public Long getInstrument(String symbol) {
        return instrumentCache.getInstrument(symbol);
    }

    public String getSymbol(long token) {
        return instrumentCache.getSymbol(token);
    }

    public void authenticate(String requestToken) {
        try {
            User user = kiteSdk.generateSession(requestToken, apiSecret);
            kiteSdk.setAccessToken(user.accessToken);
            kiteSdk.setPublicToken(user.publicToken);
            addSessionExpiryHook();
            Margin margins = kiteSdk.getMargins("equity");
            log.info("available_cash={}", margins.available.cash);
            log.info("utilised_debits={}", margins.utilised.debits);
            initialised = true;
            initialiseWebSocket();
        } catch (KiteException | IOException e) {
            log.error("Error while Initialising KiteService", e);
        }
    }

    public void appendWebSocketSymbolsList(List<String> symbols, boolean addFutures) {

        if(!connectToWebSocket) {
            return;
        }

        // Adding futures of the symbols as well
        ArrayList<String> allSymbols = new ArrayList<>();
        if(addFutures) {
            for (String symbol : symbols) {
                Optional<String> futureTradingSymbol = OptionPriceUtils.getNextExpiryFutureSymbol(symbol, instrumentCache.getInstruments());
                futureTradingSymbol.ifPresent(allSymbols::add);
            }
        }

        allSymbols.addAll(symbols);
        List<Long> webSocketsToAdd = allSymbols
                .stream()
                .map(this::getInstrument)
                .filter(t -> !webSocketTokensToSubscribe.contains(t))
                .collect(Collectors.toCollection(ArrayList::new));

        if(!webSocketsToAdd.isEmpty()) {
            log.info("Appending symbols: {} to websocket token list", allSymbols);
            webSocketTokensToSubscribe.addAll(webSocketsToAdd);
            if(this.kiteWebSocket != null) {
                this.kiteWebSocket.subscribe(webSocketTokensToSubscribe);
            }
        }
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

    public void logOpenOrders() {
        List<Order> orders = getOrders();
        log.info("Loaded orders from Kite server : {}", getFormattedObject(orders));
    }

    public void logOpenPositions() {
        Map<String, List<Position>> orders = getPositions();
        log.info("Loaded positions from Kite server : {}", getFormattedObject(orders));
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
                    ke.code, ke.message, getFormattedOrderParams(orderParams), ke);
        } catch (JSONException | IOException e) {
            log.error("Error occurred while placing order", e);
        }
        return order;
    }

    public boolean buyOrder(String symbol, int orderSize, String tag, boolean isPlaceOrder) {
        log.info("Creating buy order with quantity : {}, symbol : {} , isPlaceOrder: {}", orderSize, symbol, isPlaceOrder);
        return placeOrder(symbol, orderSize, tag, Constants.TRANSACTION_TYPE_BUY, isPlaceOrder);
    }

    public boolean sellOrder(String symbol, double price, int orderSize, String tag, boolean isPlaceOrder) {
        log.info("Creating sell order with quantity : {}, symbol : {}, price : {} ", orderSize, symbol, price);
        return placeOrder(symbol, orderSize, tag, Constants.TRANSACTION_TYPE_SELL, isPlaceOrder);
    }

    private List<Order> getOrders() {
        try {
            return this.kiteSdk.getOrders();
        } catch (KiteException e) {
            log.error("Failed to get orders, error code: {}, error message: {}", e.code, e.message, e);
        } catch (IOException e) {
            log.error("Failed to get orders, error: {}", e.getMessage(), e);
        }
        return List.of();
    }

    private Map<String, List<Position>> getPositions() {
        try {
            return this.kiteSdk.getPositions();
        } catch (KiteException e) {
            log.error("Failed to get positions, error code: {}, error message: {}", e.code, e.message, e);
        } catch (IOException e) {
            log.error("Failed to get positions, error: {}", e.getMessage(), e);
        }
        return Map.of();
    }

    private boolean placeOrder(String symbol, int orderSize, String tag, String transactionType, boolean isPlaceOrder) {
        if(!isInitialised()) {
            log.warn("Not placing order as the kite service is not initialized yet.");
            return false;
        }

        if (!isPlaceOrder) {
            log.warn("Not placing orders as it is not enabled or allowed currently");
            return true;
        }

        if (!placeOrders) {
            log.warn("Not placing orders as it is turned off by configuration");
            return true;
        }

        try {
            OrderParams orderParams = createMarketOrderWithParameters(symbol, orderSize, transactionType, tag);
            Order order = kiteSdk.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            log.debug("order placed successfully with id: {}", order.orderId);
        } catch (KiteException e) {
            log.error("KiteException occurred while placing order, code: {}, message: {}", e.code, e.message);
            return false;
        } catch (JSONException | IOException e) {
            log.error("Error occurred while placing order", e);
            return false;
        }
        return true;
    }

    private KiteConnect kiteSdk(String apiKey, String userId) {
        KiteConnect kiteConnect = new KiteConnect(apiKey, true);
        kiteConnect.setUserId(userId);
        kiteConnect.setSessionExpiryHook(() -> log.info("session expired"));
        return kiteConnect;
    }

    private void initialiseWebSocket() {
        if(connectToWebSocket) {
            log.debug("Initialising websocket...");
            this.kiteWebSocket = new KiteWebSocket(kiteSdk, onTickerArrivalListener);
            if(!webSocketTokensToSubscribe.isEmpty()) {
                log.info("Adding websocketTokens");
                this.kiteWebSocket.subscribe(webSocketTokensToSubscribe);
            }
        }
    }

    public void appendIndexITMOptions() {
        if(connectToWebSocket) {
            List<String> indicesITMOptionSymbols = getITMIndexSymbols();
            appendWebSocketSymbolsList(indicesITMOptionSymbols, false);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private List<String> getITMIndexSymbols() {
        List<String> indexOptionSymbols = new ArrayList<>();
        try {
            HistoricalData niftyData = getEntireDayHistoricalData(getOpeningTime(), getClosingTime(), NIFTY_50, "minute");
            HistoricalData bnfData = getEntireDayHistoricalData(getOpeningTime(), getClosingTime(), NIFTY_BANK, "minute");
            getITMOptionSymbols(niftyData, indexOptionSymbols, NIFTY_50);
            getITMOptionSymbols(bnfData, indexOptionSymbols, NIFTY_BANK);
        } catch (Exception e) {
            log.error("Failed to get the ITM option symbols", e);
        }
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
}
