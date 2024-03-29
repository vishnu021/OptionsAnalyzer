package com.vish.fno.manage;

import com.google.common.annotations.VisibleForTesting;
import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.helper.EntryVerifier;
import com.vish.fno.manage.helper.StopLossAndTargetHandler;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.Depth;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.OnOrderUpdate;
import com.zerodhatech.ticker.OnTicks;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.vish.fno.manage.helper.EntryVerifier.ORDER_EXECUTED;
import static com.vish.fno.util.JsonUtils.getFormattedObject;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidCatchingGenericException"})
public class OrderHandler {
    private static final String BUY = "BUY";
    private static final String COMPLETE = "COMPLETE";
    private static final String SELL = "SELL";
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
    private final List<ActiveOrder> buyCompletedOrders = new ArrayList<>();
    private final List<ActiveOrder> completedOrders = new ArrayList<>();
    private final Map<String, Tick> latestTicks = new HashMap<>();

    @PreDestroy
    public void logOrders() {
        log.info("Buy completed orders : {}", buyCompletedOrders);
        log.info("All completed orders : {}", completedOrders);
    }

    @PostConstruct
    public void initialiseWebSocket() {
        final OnTicks onTickerArrivalListener = ticks -> {
            if (!ticks.isEmpty()) {
                handleTicks(ticks);
            }
        };
        final OnOrderUpdate onOrderUpdateListener = this::onOrderUpdate;
        kiteService.setOnTickerArrivalListener(onTickerArrivalListener);
        kiteService.setOnOrderUpdateListener(onOrderUpdateListener);
        final List<String> initialSymbols = Arrays.stream(orderConfiguration.getWebSocketDefaultSymbols()).toList();
        kiteService.appendWebSocketSymbolsList(initialSymbols, true);
    }

    public void appendOpenOrder(OrderRequest order) { // TODO: update to append open order if price movement has not passed

        String itmOptionSymbol = kiteService.getITMStock(order.getIndex(), order.getBuyThreshold(), order.isCallOrder());
        order.setOptionSymbol(itmOptionSymbol);

        Tick tick = latestTicks.get(order.getIndex());
        if(tick == null) {
            log.info("Not appending order request : {}, as tick is null", order);
            kiteService.appendWebSocketSymbolsList(List.of(order.getIndex()), false);
            return;
        }

        if(entryVerifier.isNotInActiveOrders(activeOrders, order) && !entryVerifier.hasMoveAlreadyHappened(tick.getLastTradedPrice(), order)) {
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
                int timestampIndex = timeProvider.currentTimeStampIndex();
                double ltp = tick.getLastTradedPrice();
                latestTicks.put(tickSymbol, tick);
                Optional<ActiveOrder> orderToSell = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, timestampIndex, activeOrders);
                orderToSell.ifPresent(order -> sellOrder(tick, order));
                Optional<OrderRequest> orderToBuy = entryVerifier.checkEntryInOpenOrders(tick, orderRequests, activeOrders);
                orderToBuy.ifPresent(order -> {
                    placeOrder(tick, order, timestampIndex);
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

    @VisibleForTesting
    void appendToLatestTick(String symbol, Tick tick) {
        latestTicks.put(symbol, tick);
    }

    private void onOrderUpdate(Order order) {
        log.info("Order update complete : {}", getFormattedObject(order));
        try {
            String orderId = order.orderId;
            List<ActiveOrder> activeOrdersForOrderId = getActiveOrdersByOrderId(orderId);
            log.info("Existing active order for order id: {}: {}", orderId, activeOrdersForOrderId);
            logOrderLifeCycle(order, orderId);

        } catch (Exception e) {
            log.error("Exception while finding active order for order update", e);
        }
    }

    private void logOrderLifeCycle(Order order, String orderId) {
        if(BUY.contentEquals(order.transactionType)) {
            if (COMPLETE.contentEquals(order.status)) {
                List<ActiveOrder> ordersForOrderId = getActiveOrdersByOrderId(orderId);
                log.info("Buy order completed for : {}", ordersForOrderId);
                buyCompletedOrders.addAll(ordersForOrderId);
            } else {
                List<ActiveOrder> ordersForOrderId = getActiveOrdersByOrderId(orderId);
                log.info("Buy order placed for : {}", ordersForOrderId);
            }
        }
        if(SELL.contentEquals(order.transactionType)) {
            if (COMPLETE.contentEquals(order.status)) {
                List<ActiveOrder> ordersForOrderId = getActiveOrdersByOrderId(orderId);
                log.info("Sell order completed for : {}", ordersForOrderId);
                completedOrders.addAll(ordersForOrderId);
            } else {
                List<ActiveOrder> ordersForOrderId = getActiveOrdersByOrderId(orderId);
                log.info("Sell order placed for : {}", ordersForOrderId);
            }
        }
    }

    @NotNull
    private List<ActiveOrder> getActiveOrdersByOrderId(String orderId) {
        return activeOrders
                .stream()
                .filter(o -> o.getExtraData() != null && o.getExtraData().get("kiteOrderId").contentEquals(orderId))
                .toList();
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
            log.info("Sold status : {}", getFormattedObject(soldOrder));
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
        log.info("Market depth: {}", getFormattedObject(marketDepth));
        if(marketDepth.containsKey("buy") && !marketDepth.get("buy").isEmpty()) {
            ArrayList<Depth> buyMarketDepth = marketDepth.get("buy");
            log.info("next buy price: {}", getFormattedObject(buyMarketDepth.get(0)));
        }
        if(marketDepth.containsKey("sell") && !marketDepth.get("sell").isEmpty()) {
            ArrayList<Depth> sellMarketDepth = marketDepth.get("sell");
            log.info("next sell price: {}", getFormattedObject(sellMarketDepth.get(0)));
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
