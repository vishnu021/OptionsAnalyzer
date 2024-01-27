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
import java.util.ArrayList;
import java.util.List;

import static com.vish.fno.reader.util.OrderUtils.createOrderWithParameters;

@Slf4j
@SuppressWarnings({"PMD.RedundantFieldInitializer", "PMD.LooseCoupling"})
public class KiteService {
    @Getter(AccessLevel.PACKAGE)
    private final KiteConnect kiteSdk;
    private final String apiSecret;
    private final boolean placeOrders;
    @Getter
    private boolean initialised = false;
    private final boolean connectToWebSocket;
    private KiteWebSocket kiteWebSocket;

    @Setter
    private OnTicks onTickerArrivalListener;
    private final ArrayList<Long> webSocketTokensToSubscribe;

    public KiteService(String apiSecret, String apiKey, String userId, boolean placeOrders, boolean connectToWebSocket) {
        this.kiteSdk = kiteSdk(apiKey, userId);
        this.apiSecret = apiSecret;
        this.placeOrders = placeOrders;
        this.connectToWebSocket = connectToWebSocket;
        this.webSocketTokensToSubscribe = new ArrayList<>();
    }

    private KiteConnect kiteSdk(String apiKey, String userId) {
        KiteConnect kiteConnect = new KiteConnect(apiKey, true);
        kiteConnect.setUserId(userId);
        kiteConnect.setSessionExpiryHook(() -> log.info("session expired"));
        return kiteConnect;
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

    public void appendWebSocketTokensList(ArrayList<Long> newTokens) {
        List<Long> webSocketsToAdd = newTokens.stream().filter(t -> !webSocketTokensToSubscribe.contains(t)).toList();

        if(!webSocketsToAdd.isEmpty()) {
            log.info("Appending instruments: {} to websocket token list", newTokens);
            webSocketTokensToSubscribe.addAll(webSocketsToAdd);
            if(this.kiteWebSocket != null) {
                this.kiteWebSocket.subscribe(webSocketTokensToSubscribe);
            }
        }
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

    private void addSessionExpiryHook() {
        kiteSdk.setSessionExpiryHook(() -> log.info("kite session expired"));
    }

    public List<Instrument> getAllInstruments() {
        List<Instrument> instruments = null;
        try {
            instruments = kiteSdk.getInstruments();
            log.info("Loaded instrument cache from Kite server");
        } catch (JSONException | IOException | KiteException e) {
            log.error("Failed to load instruments from Kite server", e);
        }
        return instruments;
    }

    public boolean buyOrder(String symbol, double price, int orderSize, String tag, boolean isPlaceOrder) {
        log.info("Creating buy order with quantity : {}, symbol : {}, price : {} , isPlaceOrder: {}", orderSize, symbol, price, isPlaceOrder);
        return placeOrder(symbol, price, orderSize, tag, Constants.TRANSACTION_TYPE_BUY, isPlaceOrder);
    }

    // TODO: verify there is an existing order before placing a sell order
    public boolean sellOrder(String symbol, double price, int orderSize, String tag, boolean isPlaceOrder) {
        log.info("Creating sell order with quantity : {}, symbol : {}, price : {} ", orderSize, symbol, price);
        return placeOrder(symbol, price, orderSize, tag, Constants.TRANSACTION_TYPE_SELL, isPlaceOrder);
    }

    private boolean placeOrder(String symbol, double price, int orderSize, String tag, String transactionType, boolean isPlaceOrder) {
        if (!(placeOrders && isPlaceOrder)) {
            log.error("Not placing orders as it is turned off by configuration");
            return true;
        }

        try {
            OrderParams orderParams = createOrderWithParameters(symbol, price, orderSize, transactionType, tag);
            Order order = kiteSdk.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            log.debug("order id : {}", order.orderId);
        } catch (JSONException | IOException | KiteException e) {
            log.error("Error occurred while placing order", e);
            return false;
        }
        return true;
    }
}
