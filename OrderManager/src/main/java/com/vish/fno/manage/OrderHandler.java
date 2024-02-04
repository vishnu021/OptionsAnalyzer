package com.vish.fno.manage;

import com.google.common.annotations.VisibleForTesting;
import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.reader.helper.InstrumentCache;
import com.vish.fno.manage.helper.OpenOrderVerifier;
import com.vish.fno.manage.helper.StopLossAndTargetHandler;
import com.vish.fno.manage.helper.TimeProvider;
import com.vish.fno.manage.model.Pair;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.OnTicks;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.LooseCoupling"})
public class OrderHandler {
    private final KiteService kiteService;
    private final InstrumentCache instrumentCache;
    private final OrderConfiguration orderConfiguration;
    private final FileUtils fileUtils;
    private final TimeProvider timeProvider;
    private final OpenOrderVerifier openOrderVerifier;
    private final StopLossAndTargetHandler stopLossAndTargetHandler;
    @Getter
    private final List<OpenOrder> openOrders = new ArrayList<>();
    @Getter
    private final List<ActiveOrder> activeOrders = new ArrayList<>();

    private final Map<String, Pair<Date, Double>> latestTickPrices = new HashMap<>();

    @PostConstruct
    public void initialiseWebSocket() {
        final OnTicks onTickerArrivalListener = ticks -> {
            if (!ticks.isEmpty()) {
                handleTicks(ticks);
            }
        };

        kiteService.setOnTickerArrivalListener(onTickerArrivalListener);
        final ArrayList<Long> initialTokens = Arrays.stream(orderConfiguration.getWebSocketDefaultSymbols())
                .map(instrumentCache::getInstrument)
                .collect(Collectors.toCollection(ArrayList::new));
        kiteService.appendWebSocketTokensList(initialTokens);
    }

    public void appendOpenOrder(OpenOrder order) {
        if(openOrderVerifier.isNotInActiveOrders(activeOrders, order)) {
            openOrders.removeIf(o -> o.equals(order));
            openOrders.add(order);
            addTokenToWebsocket(order);
        }
    }

    @VisibleForTesting
    void handleTicks(ArrayList<Tick> ticks) {
        for(Tick tick: ticks) {
            try {
                String tickSymbol = instrumentCache.getSymbol(tick.getInstrumentToken());
                latestTickPrices.put(tickSymbol, new Pair<>(tick.getTickTimestamp(), tick.getLastTradedPrice()));
                Optional<ActiveOrder> orderToSell = stopLossAndTargetHandler.getActiveOrderToSell(tick, activeOrders);
                orderToSell.ifPresent(order -> sellOrder(tick, order));
                Optional<OpenOrder> orderToBuy = openOrderVerifier.checkEntryInOpenOrders(tick, openOrders, activeOrders);
                orderToBuy.ifPresent(order -> {
                    int timestamp = timeProvider.currentTimeStampIndex();
                    placeOrder(tick, order, timestamp);
                });
                fileUtils.appendTickToFile(tickSymbol, tick);
            } catch (Exception e) {
                log.warn("Failed to apply tick information for tick: {}", tick.getInstrumentToken(), e);
            }
        }
    }

    void removeExpiredOpenOrders(int timestamp) {
        openOrders.removeIf(o -> {
            boolean isOrderExpired = timestamp > o.getExpirationTimestamp();
            if(isOrderExpired) {
                log.debug("timestamp: {} crossed, removing open order: {}", timestamp, o);
            }
            return isOrderExpired;
        });
    }

    private void addTokenToWebsocket(OpenOrder order) {
        ArrayList<Long> newToken = new ArrayList<>();
        newToken.add(instrumentCache.getInstrument(order.getIndex()));
        newToken.add(instrumentCache.getInstrument(order.getOptionSymbol()));
        kiteService.appendWebSocketTokensList(newToken);
    }

    private void sellOrder(Tick tick, ActiveOrder order) {
        log.info("kite service : {}", kiteService);
        boolean orderSold = kiteService.sellOrder(order.getOptionSymbol(),
                tick.getLastTradedPrice(),
                order.getQuantity(),
                order.getTag(),
                openOrderVerifier.isPlaceOrder(order, false));
        log.info("Sold status : {}", orderSold);
        if(orderSold) {
            order.setActive(false);
            Pair<Date, Double> lastPricePair = latestTickPrices.get(order.getOptionSymbol());
            if(lastPricePair != null) {
                log.info("Setting sell option price: {} at: {} for symbol: {} in order: {}",
                        lastPricePair.getValue(), TimeUtils.getStringDateTime(lastPricePair.getKey()),
                        order.getOptionSymbol(), order);
                order.setSellOptionPrice(lastPricePair.getValue());
            }
            order.setExitDatetime(new Date());
            order.closeOrder(tick.getLastTradedPrice(), timeProvider.currentTimeStampIndex());
            fileUtils.logCompletedOrder(order);
        } else {
            log.error("########################################################");
            log.error("FAILED TO EXIT ORDER, PLEASE EXIT ORDER MANUALLY...");
            log.error("order : {}", order);
            log.error("########################################################");
        }
        List<ActiveOrder> soldOrders = activeOrders.stream().filter(a -> !a.isActive()).toList();
        for(ActiveOrder activeOrder: soldOrders) {
            log.debug("Removing sold order: {}", activeOrder);
            activeOrders.remove(activeOrder);
        }
    }

    // TODO: add an additional check to ensure order is not placed against wrong instrument.

    private void placeOrder(Tick tick, OpenOrder order, int timestamp) {
        ActiveOrder activeOrder = createActiveOrder(order, tick, timestamp);
        if(!activeOrders.contains(activeOrder)) {
            activeOrder.setEntryDatetime(new Date());
            // getting null pointer if the tick didn't come
            if(latestTickPrices.containsKey(activeOrder.getOptionSymbol())) {
                Pair<Date, Double> lastPricePair = latestTickPrices.get(order.getOptionSymbol());

                log.info("Setting buy option price: {} at: {} for symbol: {} in order: {}",
                        lastPricePair.getValue(), TimeUtils.getStringDateTime(lastPricePair.getKey()),
                        order.getOptionSymbol(), order);
                activeOrder.setBuyOptionPrice(lastPricePair.getValue());
            } else {
                log.warn("Latest tick price for symbol: {} not present in latest tick prices: {}",
                        activeOrder.getOptionSymbol(), latestTickPrices.keySet());
            }
            log.debug("Placing an order for index: {}, symbol: {}, at buyThreshold: {}", order.getIndex(), order.getOptionSymbol(), order.getBuyThreshold());

            // check if the price has not already moved too much
            final boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, order);

            boolean orderPlaced = kiteService.buyOrder(order.getOptionSymbol(),
                    order.getQuantity(),
                    order.getTag(),
                    openOrderVerifier.isPlaceOrder(activeOrder, true) && continueOrder);
            if(orderPlaced) {
                activeOrder.setActive(true);
                activeOrders.add(activeOrder);
                openOrders.remove(order);
                log.debug("Order placed successfully, active orders ({}): {}", activeOrders.size(), activeOrders);
            } else {
                log.warn("Failed to place order : {}", order);
            }
        }
    }

    private ActiveOrder createActiveOrder(OpenOrder openOrder, Tick tick, int timestamp) {
        return ActiveOrderFactory.createOrder(openOrder, tick.getLastTradedPrice(), timestamp);
    }
}
