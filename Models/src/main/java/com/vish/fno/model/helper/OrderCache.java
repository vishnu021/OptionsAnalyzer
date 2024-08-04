package com.vish.fno.model.helper;

import com.vish.fno.model.Ticker;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@Getter
public class OrderCache {
    public static final String REJECTED = "REJECTED";
    public static final String BUY = "BUY";
    private final List<OrderRequest> orderRequests;
    private final List<ActiveOrder> activeOrders;
    private final List<ActiveOrder> buyCompletedOrders;
    private final List<ActiveOrder> completedOrders;
    private final Map<String, Ticker> latestTicks;
    @Setter
    private double availableCash;

    public OrderCache(double availableCash) {
        this.availableCash = availableCash;
        orderRequests = new ArrayList<>();
        activeOrders = new ArrayList<>();
        buyCompletedOrders = new ArrayList<>();
        completedOrders = new ArrayList<>();
        latestTicks = new HashMap<>();
        log.info("Initialising order cache with available cash: {}", this.availableCash);
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

    //visible for testing
    public void appendToLatestTick(String symbol, Ticker tick) {
        latestTicks.put(symbol, tick);
    }
}
