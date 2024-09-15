package com.vish.fno.model.cache;

import com.vish.fno.model.Ticker;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.vish.fno.model.util.ModelUtils.roundTo5Paise;

@Slf4j
public class OrderCache {
    private final List<OrderRequest> orderRequests;
    private final List<ActiveOrder> activeOrders;
    private final Map<String, Ticker> latestTicks;
    private final LimitedCache<String, Ticker> ticksCache;
    @Getter
    @Setter
    private double availableCash;

    private int timestamp = 1;
    private static final List<String> symbolsToLog = List.of("NIFTY BANK", "NIFTY 50", "RELIANCE", "HDFCBANK");

    public OrderCache(double availableCash) {
        this.availableCash = availableCash;
        orderRequests = new ArrayList<>();
        activeOrders = new ArrayList<>();
        latestTicks = new HashMap<>();
        ticksCache = new LimitedCache<>(100);
        log.info("Initialising order cache with available cash: {}", this.availableCash);
    }

    public void logOpenOrders() {
        if(!orderRequests.isEmpty()) {
            log.info("list of open orders({}) : {}", orderRequests.size(), orderRequests);
        }
        if(!activeOrders.isEmpty()) {
            log.info("list of active orders({}) : {}", activeOrders.size(), activeOrders);
        }

        if(timestamp++ % 5 == 0) {
            timestamp = 1;
            ticksCache.keySet().stream().filter(symbolsToLog::contains).forEach(key -> {
                final List<Ticker> ticks = ticksCache.get(key).stream().sorted().toList();
                final double max = ticks.stream().mapToDouble(Ticker::getLastTradedPrice).max().orElse(0d);
                final double min = ticks.stream().mapToDouble(Ticker::getLastTradedPrice).min().orElse(0d);
                log.info("symbol: {}, [0]: {}, [{}]: {}, max: {}, min: {}", key, ticks.get(0), ticks.size() - 1, ticks.get(ticks.size() - 1), roundTo5Paise(max), roundTo5Paise(min));
            });
        }
    }

    public Optional<OrderRequest> checkEntryInOpenOrders(Ticker tick, final String tickSymbol) {
        if(orderRequests.isEmpty()) {
            return Optional.empty();
        }

        List<OrderRequest> tickSymbolOrders = orderRequests
                .stream()
                .filter(e -> e.getIndex().contentEquals(tickSymbol) && isNotInActiveOrders(e))
                .toList();

        for (OrderRequest order : tickSymbolOrders) {
            Optional<OrderRequest> openOrderOptional = order.verifyBuyThreshold(tick);
            if(!orderRequests.isEmpty()) {
                return openOrderOptional;
            }
        }
        return Optional.empty();
    }

    public boolean isNotInActiveOrders(OrderRequest tickOrderRequest) {
        boolean isNotInActiveOrder = activeOrders
                .stream()
                .noneMatch(a -> a.getTag().equalsIgnoreCase(tickOrderRequest.getTag())
                        && a.getIndex().equalsIgnoreCase(tickOrderRequest.getIndex()));
        if(!isNotInActiveOrder) {
            log.info("Already an active order present for symbol: {}, open order: {}",
                    tickOrderRequest.getIndex(), tickOrderRequest);
        }
        return isNotInActiveOrder;
    }

    public void removeActiveOrder(ActiveOrder order) {
        activeOrders.remove(order);
    }

    public List<ActiveOrder> getActiveOrderForSymbol(String symbol) {
        return activeOrders.stream().filter(o -> o.getIndex().contentEquals(symbol)).toList();
    }

    public void removeOrderRequest(OrderRequest order) {
        orderRequests.remove(order);
    }

    public void addOrderRequest(OrderRequest order) {
        orderRequests.removeIf(o -> o.equals(order));
        orderRequests.add(order);
    }

    public void removeExpiredOpenOrders(int timestamp) {
        orderRequests.removeIf(o -> {
            boolean isOrderExpired = timestamp > o.getExpirationTimestamp();
            if(isOrderExpired) {
                log.info("timestamp: {} crossed, removing open order: {}", timestamp, o);
            }
            return isOrderExpired;
        });
    }

    public void appendTick(String symbol, Ticker tick) {
        latestTicks.put(symbol, tick);
        ticksCache.put(symbol, tick);
    }

    public Ticker getLatestTick(String symbol) {
        return latestTicks.get(symbol);
    }

    public void appendActiveOrder(ActiveOrder activeOrder) {
        this.activeOrders.add(activeOrder);
    }

    public List<ActiveOrder> getActiveOrders() {
        return activeOrders;
    }
}
