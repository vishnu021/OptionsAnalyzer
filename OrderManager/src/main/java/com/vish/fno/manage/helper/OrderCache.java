package com.vish.fno.manage.helper;

import com.google.common.annotations.VisibleForTesting;
import com.vish.fno.manage.orderflow.OrderDetailsLogger;
import com.vish.fno.model.Ticker;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.zerodhatech.models.Order;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.vish.fno.manage.helper.AbstractEntryVerifier.ORDER_EXECUTED;
import static com.vish.fno.util.JsonUtils.getFormattedObject;

@Slf4j
@Getter
@Component
public class OrderCache {
    public static final String REJECTED = "REJECTED";
    public static final String BUY = "BUY";
    private final List<OrderRequest> orderRequests;
    private final List<ActiveOrder> activeOrders;
    private final List<ActiveOrder> buyCompletedOrders;
    private final List<ActiveOrder> completedOrders;
    private final Map<String, Ticker> latestTicks;
    @Setter
    @Value("${order.availableCash}")
    private double availableCash;

    public OrderCache() {
        orderRequests = new ArrayList<>();
        activeOrders = new ArrayList<>();
        buyCompletedOrders = new ArrayList<>();
        completedOrders = new ArrayList<>();
        latestTicks = new HashMap<>();
    }

    @PreDestroy
    public void logOrders() {
        log.info("Buy completed orders : {}", buyCompletedOrders);
        log.info("All completed orders : {}", completedOrders);
    }

    public void logOpenOrders() {
        if(!orderRequests.isEmpty()) {
            log.info("list of open orders({}) : {}", orderRequests.size(), orderRequests);
        }
        if(!activeOrders.isEmpty()) {
            log.info("list of active orders({}) : {}", activeOrders.size(), activeOrders);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void onOrderUpdate(Order order) {
        final List<ActiveOrder> activeOrders = getActiveOrders();

        log.info("Order update complete : {}", getFormattedObject(order));
        try {
            String orderId = order.orderId;
            List<ActiveOrder> activeOrdersForOrderId = OrderDetailsLogger.getActiveOrdersByOrderId(activeOrders, orderId);
            log.info("Existing active order for order id: {}: {}", orderId, activeOrdersForOrderId);
            OrderDetailsLogger.logOrderLifeCycle(activeOrders, buyCompletedOrders, completedOrders, order, orderId);

            if(order.status.equals(REJECTED) && order.transactionType.equals(BUY)) {
                log.warn("Order was rejected with statusMessage: {}, marking it as not complete", order.statusMessage);
                activeOrders.stream().filter(a ->
                        a.getOptionSymbol().equals(order.tradingSymbol) && a.getTag().substring(0,20).equals(order.tag)
                ).forEach(a -> {
                    log.info("marking order executed as false for order: {}", a);
                    a.getExtraData().put(ORDER_EXECUTED, "false");
                });
            }
        } catch (Exception e) {
            log.error("Exception while finding active order for order update", e);
        }
    }

    public Optional<OrderRequest> checkEntryInOpenOrders(Ticker tick, final String tickSymbol) {
        if(orderRequests.isEmpty()) {
            return Optional.empty();
        }

        List<OrderRequest> tickSymbolOrders = orderRequests
                .stream()
                .filter(e -> e.getIndex().contentEquals(tickSymbol) && isNotInActiveOrders(activeOrders, e))
                .toList();

        for (OrderRequest order : tickSymbolOrders) {
            Optional<OrderRequest> openOrderOptional = order.verifyBuyThreshold(tick);
            if(!orderRequests.isEmpty()) {
                return openOrderOptional;
            }
        }
        return Optional.empty();
    }

    public boolean isNotInActiveOrders(List<ActiveOrder> activeOrders, OrderRequest tickOrderRequest) {
        boolean isNotInActiveOrder = activeOrders
                .stream()
                .noneMatch(a -> a.getTag().equalsIgnoreCase(tickOrderRequest.getTag())
                        && a.getIndex().equalsIgnoreCase(tickOrderRequest.getIndex())
                        && a.isCallOrder() == tickOrderRequest.isCallOrder());
        if(!isNotInActiveOrder) {
            log.info("Already an active order present for symbol: {}, open order: {}, call: {}",
                    tickOrderRequest.getIndex(), tickOrderRequest, tickOrderRequest.isCallOrder());
        }
        return isNotInActiveOrder;
    }

    @VisibleForTesting
    public void appendToLatestTick(String symbol, Ticker tick) {
        latestTicks.put(symbol, tick);
    }
}
