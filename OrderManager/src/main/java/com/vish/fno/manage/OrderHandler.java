package com.vish.fno.manage;

import com.google.common.annotations.VisibleForTesting;
import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.helper.EntryVerifier;
import com.vish.fno.manage.helper.StopLossAndTargetHandler;
import com.vish.fno.manage.helper.TimeProvider;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.JsonUtils;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.Depth;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.OnOrderUpdate;
import com.zerodhatech.ticker.OnTicks;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.vish.fno.manage.helper.EntryVerifier.ORDER_EXECUTED;
import static com.vish.fno.reader.util.KiteUtils.getFormattedOrder;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidCatchingGenericException"})
public class OrderHandler {
    private final KiteService kiteService;
    private final OrderConfiguration orderConfiguration;
    private final FileUtils fileUtils;
    private final TimeProvider timeProvider;
    private final EntryVerifier entryVerifier;
    private final StopLossAndTargetHandler stopLossAndTargetHandler;
    @Getter
    private final List<OrderRequest> orderRequests = new ArrayList<>();
    @Getter
    private final List<ActiveOrder> activeOrders = new ArrayList<>();
    private final Map<String, Tick> latestTicks = new HashMap<>();

    @PostConstruct
    public void initialiseWebSocket() {
        final OnTicks onTickerArrivalListener = ticks -> {
            if (!ticks.isEmpty()) {
                handleTicks(ticks);
            }
        };
        final OnOrderUpdate onOrderUpdateListener = order -> {
            log.info("Order update complete : {}", getFormattedOrder(order));
            try {
                String orderId = order.orderId;
                List<ActiveOrder> activeOrdersForOrderId = activeOrders
                        .stream()
                        .filter(o -> o.getExtraData() != null && o.getExtraData().get("kiteOrderId").contentEquals(orderId))
                        .collect(Collectors.toList());
                log.info("Existing active order: {}", activeOrdersForOrderId);
            } catch (Exception e) {
                log.error("Exception while finding active order for order update", e);
            }
        };
        kiteService.setOnTickerArrivalListener(onTickerArrivalListener);
        kiteService.setOnOrderUpdateListener(onOrderUpdateListener);
        final List<String> initialSymbols = Arrays.stream(orderConfiguration.getWebSocketDefaultSymbols()).toList();
        kiteService.appendWebSocketSymbolsList(initialSymbols, true);
    }

    public void appendOpenOrder(OrderRequest order) {
        if(entryVerifier.isNotInActiveOrders(activeOrders, order)) {
            orderRequests.removeIf(o -> o.equals(order));
            orderRequests.add(order);
            addTokenToWebsocket(order);
        }
    }

    @VisibleForTesting
    @SuppressWarnings({"PMD.LooseCoupling"})
    void handleTicks(ArrayList<Tick> ticks) {
        for(Tick tick: ticks) {
            try {
                String tickSymbol = kiteService.getSymbol(tick.getInstrumentToken());
                latestTicks.put(tickSymbol, tick);
                Optional<ActiveOrder> orderToSell = stopLossAndTargetHandler.getActiveOrderToSell(tick, activeOrders);
                orderToSell.ifPresent(order -> sellOrder(tick, order));
                Optional<OrderRequest> orderToBuy = entryVerifier.checkEntryInOpenOrders(tick, orderRequests, activeOrders);
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
        orderRequests.removeIf(o -> {
            boolean isOrderExpired = timestamp > o.getExpirationTimestamp();
            if(isOrderExpired) {
                log.debug("timestamp: {} crossed, removing open order: {}", timestamp, o);
            }
            return isOrderExpired;
        });
    }

    private void addTokenToWebsocket(OrderRequest order) {
        kiteService.appendWebSocketSymbolsList(List.of(order.getIndex(), order.getOptionSymbol()), false);
    }

    private void sellOrder(Tick tick, ActiveOrder order) {
        Optional<KiteOpenOrder> orderSoldOptional = kiteService.sellOrder(order.getOptionSymbol(),
                tick.getLastTradedPrice(),
                order.getQuantity(),
                order.getTag(),
                entryVerifier.isPlaceOrder(order, false));
        if(orderSoldOptional.isPresent() && orderSoldOptional.get().isOrderPlaced()) {
            KiteOpenOrder soldOrder = orderSoldOptional.get();
            log.info("Sold status : {}", JsonUtils.getFormattedObject(soldOrder));
            order.setActive(false);
            Tick latestOptionTick = latestTicks.get(order.getOptionSymbol());
            if(latestOptionTick != null) {
                log.info("Setting sell option price: {} at: {} for symbol: {} in order: {}",
                        latestOptionTick.getLastTradedPrice(), TimeUtils.getStringDateTime(latestOptionTick.getTickTimestamp()),
                        order.getOptionSymbol(), order);
                logMarketDepth(latestOptionTick);
                order.setSellOptionPrice(latestOptionTick.getLastTradedPrice());
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

    private void logMarketDepth(Tick tick) {
        Map<String, ArrayList<Depth>> marketDepth = tick.getMarketDepth();
        if(marketDepth == null) {
            return;
        }
        log.info("Market depth: {}", JsonUtils.getFormattedObject(marketDepth));
        if(marketDepth.containsKey("buy") && !marketDepth.get("buy").isEmpty()) {
            ArrayList<Depth> buyMarketDepth = marketDepth.get("buy");
            log.info("next buy price: {}", JsonUtils.getFormattedObject(buyMarketDepth.get(0)));
        }
        if(marketDepth.containsKey("sell") && !marketDepth.get("sell").isEmpty()) {
            ArrayList<Depth> sellMarketDepth = marketDepth.get("sell");
            log.info("next sell price: {}", JsonUtils.getFormattedObject(sellMarketDepth.get(0)));
        }
    }

    // TODO: add an additional check to ensure order is not placed against wrong instrument.

    private void placeOrder(Tick tick, OrderRequest order, int timestamp) {
        ActiveOrder activeOrder = createActiveOrder(order, tick, timestamp);
        if(!activeOrders.contains(activeOrder)) {
            activeOrder.setEntryDatetime(new Date());
            // getting null pointer if the tick didn't come
            if(latestTicks.containsKey(activeOrder.getOptionSymbol())) {
                Tick latestOptionTick = latestTicks.get(order.getOptionSymbol());

                log.info("Setting buy option price: {} at: {} for symbol: {} in order: {}",
                        latestOptionTick.getLastTradedPrice(), TimeUtils.getStringDateTime(latestOptionTick.getTickTimestamp()),
                        order.getOptionSymbol(), order);
                logMarketDepth(latestOptionTick);
                activeOrder.setBuyOptionPrice(latestOptionTick.getLastTradedPrice());
            } else {
                log.warn("Latest tick price for symbol: {} not present in latest tick prices: {}",
                        activeOrder.getOptionSymbol(), latestTicks.keySet());
            }

            if(entryVerifier.hasMoveAlreadyHappened(activeOrder.getBuyPrice(), activeOrder)) {
                return;
            }

            log.debug("Placing an order for index: {}, symbol: {}, at buyThreshold: {}", order.getIndex(), order.getOptionSymbol(), order.getBuyThreshold());

            Optional<KiteOpenOrder> orderPlacedOptional = kiteService.buyOrder(order.getOptionSymbol(),
                    order.getQuantity(),
                    order.getTag(),
                    entryVerifier.isPlaceOrder(activeOrder, true));
            if(orderPlacedOptional.isPresent() && orderPlacedOptional.get().isOrderPlaced()) {
                KiteOpenOrder orderPlaced = orderPlacedOptional.get();
                activeOrder.setActive(true);
                if(orderPlaced != null && orderPlaced.getOrder() != null) {
                    activeOrder.appendExtraData("kiteOrderId", orderPlaced.getOrder().orderId);
                }
                activeOrders.add(activeOrder);
                orderRequests.remove(order);
                log.debug("Order placed successfully, active orders ({}): {}", activeOrders.size(), activeOrders);
            } else {
                log.warn("Failed to place order : {}", order);
                // TODO: add back the amount in open order verifier
                // TODO: temporarily continuing the order in mock trade
                activeOrder.appendExtraData(ORDER_EXECUTED, String.valueOf(false));
                activeOrder.setActive(true);
                activeOrders.add(activeOrder);
                orderRequests.remove(order);
            }
        }
    }

    private ActiveOrder createActiveOrder(OrderRequest orderRequest, Tick tick, int timestamp) {
        return ActiveOrderFactory.createOrder(orderRequest, tick.getLastTradedPrice(), timestamp);
    }
}
