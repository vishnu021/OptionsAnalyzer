package com.vish.fno.manage.orderflow;

import com.google.common.annotations.VisibleForTesting;
import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.helper.EntryVerifier;
import com.vish.fno.manage.helper.IndexEntryVerifier;
import com.vish.fno.manage.helper.OrderCache;
import com.vish.fno.manage.helper.TickMapper;
import com.vish.fno.model.Ticker;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.IndexOrderRequest;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.helper.OrderManagerUtils;
import com.vish.fno.util.orderflow.TargetAndStopLossStrategy;
import com.zerodhatech.ticker.OnOrderUpdate;
import com.zerodhatech.ticker.OnTicks;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.PrematureDeclaration"})
public class OrderHandler {
    private final KiteService kiteService;
    private final OrderConfiguration orderConfiguration;
    private final FileUtils fileUtils;
    private final TimeProvider timeProvider;
    private final TargetAndStopLossStrategy targetAndStopLossStrategy;
    @Getter
    private final OrderCache orderCache;

    @PostConstruct
    public void initialiseWebSocket() {
        final OnTicks onTickerArrivalListener = ticks -> {
            if (!ticks.isEmpty()) {
                List<Ticker> tickers = TickMapper.mapTicks(ticks);
                handleTicks(tickers);
            }
        };
        final OnOrderUpdate onOrderUpdateListener = (order) -> orderCache.onOrderUpdate(order);
        kiteService.setOnTickerArrivalListener(onTickerArrivalListener);
        kiteService.setOnOrderUpdateListener(onOrderUpdateListener);
        final List<String> initialSymbols = Arrays.stream(orderConfiguration.getWebSocketDefaultSymbols()).toList();
        kiteService.appendWebSocketSymbolsList(initialSymbols, true);
    }

    @VisibleForTesting
    public void handleTicks(List<Ticker> ticks) {
        for(Ticker tick: ticks) {
            try {
                final String tickSymbol = kiteService.getSymbol(tick.getInstrumentToken());
                exitOrder(tick, tickSymbol);
                orderCache.checkEntryInOpenOrders(tick, tickSymbol)
                        .ifPresent(order -> order.placeOrder(tick));
                fileUtils.appendTickToFile(tickSymbol, tick);
            } catch (Exception e) {
                log.warn("Failed to apply tick information for tick: {}", tick.getInstrumentToken(), e);
            }
        }
    }

    private void exitOrder(Ticker tick, String tickSymbol) {
        final Map<String, Ticker> latestTicks = orderCache.getLatestTicks();
        final List<ActiveOrder> activeOrders = orderCache.getActiveOrders();
        int timestampIndex = timeProvider.currentTimeStampIndex();
        latestTicks.put(tickSymbol, tick);
        getActiveOrderToSell(tickSymbol, tick, timestampIndex, activeOrders);
    }

    public void appendOpenOrder(OrderRequest order) {
        final List<OrderRequest> orderRequests = orderCache.getOrderRequests();
        final List<ActiveOrder> activeOrders = orderCache.getActiveOrders();
        final Map<String, Ticker> latestTicks = orderCache.getLatestTicks();

        if(order instanceof IndexOrderRequest) {
            IndexEntryVerifier indexEntryVerifier = new IndexEntryVerifier(kiteService, timeProvider);
            ((IndexOrderRequest) order).setOrderFlowHandler(new IndexOrderFlowHandler(kiteService, indexEntryVerifier, timeProvider, this, fileUtils));
        }
        // TODO: update to append open order if price movement has not passed
        final String otmOptionSymbol = kiteService.getITMStock(order.getIndex(), order.getBuyThreshold(), order.isCallOrder());

        if(StringUtils.isNotEmpty(otmOptionSymbol)) {
            order.setOptionSymbol(otmOptionSymbol);
        } else {
            log.warn("Unable to get OTM option symbol, finding ITM option symbol. open order: {}", order);
            final String itmOptionSymbol = kiteService.getITMStock(order.getIndex(), order.getBuyThreshold(), order.isCallOrder());
            order.setOptionSymbol(itmOptionSymbol);
        }

        final Ticker tick = latestTicks.get(order.getIndex());
        if(tick == null) {
            log.info("Not appending order request : {}, as tick is null", order);
            kiteService.appendWebSocketSymbolsList(List.of(order.getIndex()), false);
            return;
        }

        if(orderCache.isNotInActiveOrders(activeOrders, order) && !hasMoveAlreadyHappened(tick.getLastTradedPrice(), order)) {
            orderRequests.removeIf(o -> o.equals(order));
            orderRequests.add(order);
            addTokenToWebsocket(order);
        }
    }

    public boolean hasMoveAlreadyHappened(double ltp, OrderRequest order) {
        if(order instanceof IndexOrderRequest) {
            return ((IndexOrderRequest) order).getOrderFlowHandler().hasMoveAlreadyHappened(ltp, order);
        }

        log.warn("No implementation of entry verifier available for order : {} or type : {}", order, order.getClass());
        return true;
    }

    public void getActiveOrderToSell(String tickSymbol, Ticker tick, int timestampIndex, List<ActiveOrder> activeOrders) {
        List<ActiveOrder> activeOrdersForTick = activeOrders.stream().filter(o -> o.getIndex().contentEquals(tickSymbol)).toList();

        if(activeOrdersForTick.isEmpty()) {
            return;
        }

        for(ActiveOrder order : activeOrdersForTick) {
            OrderSellDetailModel exitCondition = OrderManagerUtils.isExitCondition(targetAndStopLossStrategy, tick.getLastTradedPrice(), timestampIndex, order);
            if(exitCondition.isSellOrder()) {
                order.sellOrder(exitCondition, tick);
            }
        }
    }

    void removeExpiredOpenOrders() {
        final List<OrderRequest> orderRequests = orderCache.getOrderRequests();
        int timestamp = timeProvider.currentTimeStampIndex();
        orderRequests.removeIf(o -> {
            boolean isOrderExpired = timestamp > o.getExpirationTimestamp();
            if(isOrderExpired) {
                log.info("timestamp: {} crossed, removing open order: {}", timestamp, o);
            }
            return isOrderExpired;
        });
    }

    private void addTokenToWebsocket(OrderRequest order) {
        kiteService.appendWebSocketSymbolsList(List.of(order.getIndex(), order.getOptionSymbol()), false);
    }

    // TODO: add an additional check to ensure order is not placed against wrong instrument.
}
