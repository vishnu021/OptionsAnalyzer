package com.vish.fno.manage.orderflow;

import com.vish.fno.manage.helper.IndexEntryVerifier;
import com.vish.fno.model.Ticker;
import com.vish.fno.model.order.ActiveOrderFactory;
import com.vish.fno.model.order.OrderSellDetailModel;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.IndexOrderRequest;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.OrderFlowHandler;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.vish.fno.manage.helper.AbstractEntryVerifier.ORDER_EXECUTED;
import static com.vish.fno.manage.orderflow.OrderDetailsLogger.logMarketDepth;
import static com.vish.fno.util.JsonUtils.getFormattedObject;

@Slf4j
@RequiredArgsConstructor
public class IndexOrderFlowHandler implements OrderFlowHandler {
    private static final String ORDER_EXIT_DETAILS = "orderExitDetails";
    private final KiteService kiteService;
    private final IndexEntryVerifier entryVerifier;
    private final TimeProvider timeProvider;
    private final OrderHandler orderHandler;
    private final FileUtils fileUtils;

    public void placeOrder(Ticker tick, OrderRequest order) {
        final List<ActiveOrder> activeOrders = orderHandler.getOrderCache().getActiveOrders();
        final Map<String, Ticker> latestTicks = orderHandler.getOrderCache().getLatestTicks();
        final List<OrderRequest> orderRequests = orderHandler.getOrderCache().getOrderRequests();

        final String orderEntryTimestamp = TimeUtils.getStringDateTime(tick.getTickTimestamp());
        ActiveOrder activeOrder = createActiveOrder(order, tick, orderEntryTimestamp);
        if(!activeOrders.contains(activeOrder)) {
            // getting null pointer if the tick didn't come
            if(latestTicks.containsKey(activeOrder.getOptionSymbol())) {
                Ticker latestOptionTick = latestTicks.get(order.getOptionSymbol());

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
                    entryVerifier.isPlaceOrder(activeOrder, true, null, orderHandler.getOrderCache()));
            if(orderPlacedOptional.isPresent() && orderPlacedOptional.get().isOrderPlaced()) {
                KiteOpenOrder orderPlaced = orderPlacedOptional.get();
                activeOrder.setActive(true);
                if(orderPlaced.getOrder() != null) {
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

    public void sellOrder(OrderSellDetailModel exitCondition, Ticker tick, ActiveOrder order) {
        final List<ActiveOrder> activeOrders = orderHandler.getOrderCache().getActiveOrders();
        final Map<String, Ticker> latestTicks = orderHandler.getOrderCache().getLatestTicks();
        final Optional<KiteOpenOrder> orderSoldOptional = kiteService.sellOrder(order.getOptionSymbol(),
                tick.getLastTradedPrice(),
                exitCondition.getQuantity(),
                order.getTag(),
                entryVerifier.isPlaceOrder(order, false, exitCondition, orderHandler.getOrderCache()));

        if(orderSoldOptional.isPresent() && orderSoldOptional.get().isOrderPlaced()) {
            final Ticker latestOptionTick = latestTicks.get(order.getOptionSymbol());
            final double latestOptionLTP = latestOptionTick == null ? 0d : latestOptionTick.getLastTradedPrice();

            final KiteOpenOrder soldOrder = orderSoldOptional.get();
            log.info("Sold status: {}", getFormattedObject(soldOrder));
            order.incrementSoldQuantity(exitCondition.getQuantity(), latestOptionLTP);
            appendExtraData(exitCondition, tick, order);

            if(latestOptionTick != null) {
                final String orderSellingTimestamp = TimeUtils.getStringDateTime(latestOptionTick.getTickTimestamp());
                log.info("Setting sell option price: {} at: {} for symbol: {} in order: {}",
                        latestOptionLTP, orderSellingTimestamp, order.getOptionSymbol(), order);
                logMarketDepth(latestOptionTick);
                order.setSellOptionPrice(latestOptionLTP);
            }

            if(order.getSoldQuantity() == order.getBuyQuantity()) {
                order.closeOrder(tick.getLastTradedPrice(), timeProvider.currentTimeStampIndex(), TimeUtils.getStringDateTime(TimeUtils.currentTime()));
                fileUtils.logCompletedOrder(order);
            }
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

    @Override
    public Optional<OrderRequest> verifyBuyThreshold(Ticker tick, IndexOrderRequest order) {
        if(order.isCallOrder()) {
            if (tick.getLastTradedPrice() > order.getBuyThreshold()) {
                log.info("tick ltp: {} is greater than buy threshold {}, placing order request({}) : {}",
                        tick.getLastTradedPrice(), order.getBuyThreshold(), order.getOptionSymbol(), order);
                return Optional.of(order);
            }
        } else {
            if (tick.getLastTradedPrice() < order.getBuyThreshold()) {
                log.info("tick ltp: {} is lesser than buy threshold {}, placing order request({}) : {}",
                        tick.getLastTradedPrice(), order.getBuyThreshold(), order.getOptionSymbol(), order);
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean hasMoveAlreadyHappened(double ltp, OrderRequest order) {
        return entryVerifier.hasMoveAlreadyHappened(ltp, order);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void appendExtraData(OrderSellDetailModel exitCondition, Ticker tick, ActiveOrder order) {
        final Map<String, Ticker> latestTicks = orderHandler.getOrderCache().getLatestTicks();
        try {
            final Ticker latestOptionTick = latestTicks.get(order.getOptionSymbol());
            final double sellOptionPrice = latestOptionTick == null ? 0 : latestOptionTick.getLastTradedPrice();
            final String existingExitOrderDetails = order.getExtraData().get(ORDER_EXIT_DETAILS);
            final String exitOrderDetails = (existingExitOrderDetails == null ? "" : existingExitOrderDetails + ",") +
                    Map.of("quantity", exitCondition.getQuantity(),
                            "sellPrice", String.valueOf(tick.getLastTradedPrice()),
                            "sellOptionPrice", String.valueOf(sellOptionPrice));
            order.getExtraData().put(ORDER_EXIT_DETAILS, exitOrderDetails);
        } catch (Exception e) {
            log.error("failed to put data", e);
        }
    }

    private ActiveOrder createActiveOrder(OrderRequest orderRequest, Ticker tick, String orderEntryTimestamp) {
        final int timestamp = timeProvider.currentTimeStampIndex();
        return ActiveOrderFactory.createOrder(orderRequest, tick.getLastTradedPrice(), timestamp, orderEntryTimestamp);
    }
}
