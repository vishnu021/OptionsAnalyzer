package com.vish.fno.reader.service;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import static com.vish.fno.reader.util.OrderUtils.createOrderWithParameters;

@Slf4j
public class KiteService {
    @Getter(AccessLevel.PACKAGE)
    private final KiteConnect kiteSdk;
    private final String apiSecret;
    private final boolean placeOrders;
    @Getter
    private boolean initialised = false;
    private boolean connectToWebSocket = false;
    @Getter
    private KiteWebSocket kiteWebSocket;

    public KiteService(String apiSecret, String apiKey, String userId, boolean placeOrders, boolean connectToWebSocket) {
        this.kiteSdk = kiteSdk(apiKey, userId);
        this.apiSecret = apiSecret;
        this.placeOrders = placeOrders;
        this.connectToWebSocket = connectToWebSocket;
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
            if(connectToWebSocket) {
                this.kiteWebSocket = new KiteWebSocket(kiteSdk);
            }
        } catch (Exception | KiteException e) {
            log.error("Error while Initialising KiteService", e);
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

    public boolean buyOrder(String symbol, double price, int orderSize, String tag) {
        log.info("Creating buy order with quantity : {}, symbol : {}, price : {} ", orderSize, symbol, price);
        return placeOrder(symbol, price, orderSize, tag, Constants.TRANSACTION_TYPE_BUY);
    }

    public boolean sellOrder(String symbol, double price, int orderSize, String tag) {
        log.info("Creating sell order with quantity : {}, symbol : {}, price : {} ", orderSize, symbol, price);
        return placeOrder(symbol, price, orderSize, tag, Constants.TRANSACTION_TYPE_SELL);
    }

    private boolean placeOrder(String symbol, double price, int orderSize, String tag, String transactionType) {
        if (!placeOrders) {
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
