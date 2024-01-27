package com.vish.fno.reader.service;

import com.vish.fno.reader.exception.InitialisationException;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnTicks;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
@SuppressWarnings({"PMD.RedundantFieldInitializer", "PMD.LooseCoupling"})
public class KiteWebSocket {
    private final KiteTicker tickerProvider;
    private final ArrayList<Long> defaultTokens;

    public KiteWebSocket(KiteConnect kiteSdk, OnTicks onTickerArrivalListener) {
        this.tickerProvider = new KiteTicker(kiteSdk.getAccessToken(), kiteSdk.getApiKey());
        this.defaultTokens = new ArrayList<>();
        this.defaultTokens.add(256265L);
        this.defaultTokens.add(260105L);
        initialize(onTickerArrivalListener);
    }

    private void initialize(OnTicks onTickerArrivalListener) {
        addWebSocketListeners(defaultTokens, onTickerArrivalListener);
        tickerProvider.connect();
        boolean isConnected = tickerProvider.isConnectionOpen();
        log.info("isConnected : {}", isConnected);

        /* set mode is used to set mode in which you need tick for list of tokens.
         * Ticker allows three modes, modeFull, modeQuote, modeLTP.
         * For getting only last traded price, use modeLTP
         * For getting last traded price, last traded quantity, average price, volume traded today, total sell quantity and total buy quantity, open, high, low, close, change, use modeQuote
         * For getting all data with depth, use modeFull*/
        tickerProvider.setMode(defaultTokens, KiteTicker.modeLTP);
    }


    private void addWebSocketListeners(ArrayList<Long> tokens, OnTicks onTickerArrivalListener) {
        tickerProvider.setOnConnectedListener(() -> {
            /* Subscribe ticks for token.
             * By default, all tokens are subscribed for modeQuote.
             * */
            tickerProvider.subscribe(tokens);
            tickerProvider.setMode(tokens, KiteTicker.modeFull);
        });

        tickerProvider.setOnDisconnectedListener(() -> log.info("disconnected"));

        /* Set listener to get order updates.*/
        tickerProvider.setOnOrderUpdateListener(order -> log.info("order update {}", order.orderId));


        tickerProvider.setOnTickerArrivalListener(onTickerArrivalListener);

        tickerProvider.setTryReconnection(true);
        try {
            tickerProvider.setMaximumRetries(10);
            tickerProvider.setMaximumRetryInterval(30);
        } catch (KiteException e) {
            log.error("Exception while setting retries", e);
            throw new InitialisationException("Error initializing kite web socket", e);
        }
    }

    public void addListener(OnTicks onTickerArrivalListener) {
        tickerProvider.setOnTickerArrivalListener(onTickerArrivalListener);
    }

    public void subscribe(ArrayList<Long> tokens) {
        /* Subscribe ticks for token.
         * By default, all tokens are subscribed for modeQuote.
         * */
        log.info("Subscribing : {}", tokens);
        tickerProvider.subscribe(tokens);
        tickerProvider.setMode(tokens, KiteTicker.modeFull);
    }

    public void unsubscribe(ArrayList<Long> tokens) {
        log.info("Unsubscribing : {}", tokens);
        tickerProvider.unsubscribe(tokens);
    }

    @PreDestroy
    public void disconnect() {
        log.info("Disconnecting...");
        tickerProvider.disconnect();
    }
}
