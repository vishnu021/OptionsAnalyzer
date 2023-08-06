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

@Slf4j
public class KiteService {
    @Getter(AccessLevel.PACKAGE)
    private final KiteConnect kiteSdk;
    private final String apiSecret;
    @Getter
    private boolean initialised = false;
    private final boolean placeOrders;

    public KiteService(String apiSecret, String apiKey, String userId, boolean placeOrders) {
        this.kiteSdk = kiteSdk(apiKey, userId);
        this.apiSecret = apiSecret;
        this.placeOrders = placeOrders;
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

            Margin margins = kiteSdk.getMargins("equity");
            log.info("available_cash={}", margins.available.cash);
            log.info("utilised_debits={}", margins.utilised.debits);
            initialised = true;
        } catch (Exception | KiteException e) {
            log.error("Error while Initialising KiteService", e);
        }
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
            log.error("Error occurred while placing order",e);
            return false;
        }
        return true;
    }

    private OrderParams createOrderWithParameters(String symbol, double price, int orderSize,
                                                        String transactionType, String tag) {
        OrderParams orderParams = new OrderParams();
        orderParams.quantity = orderSize;
        orderParams.orderType = Constants.ORDER_TYPE_MARKET;
        orderParams.tradingsymbol = symbol;
        orderParams.product = Constants.PRODUCT_MIS;
        orderParams.exchange = Constants.EXCHANGE_NFO;
        orderParams.validity = Constants.VALIDITY_DAY;
        orderParams.transactionType = transactionType;
        orderParams.price = price;
        orderParams.triggerPrice = 0.0;
        orderParams.tag = tag;
        return orderParams;
    }
}
