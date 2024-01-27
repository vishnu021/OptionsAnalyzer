package com.vish.fno.manage;

import com.google.common.annotations.VisibleForTesting;
import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.helper.DataCache;
import com.vish.fno.manage.helper.TimeProvider;
import com.vish.fno.manage.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.OnTicks;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.vish.fno.manage.util.Constants.NIFTY_BANK;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.LooseCoupling"})
public class OrderHandler {

    public static final int INTRADAY_EXIT_POSITION_TIME_INDEX = 368;
    private final KiteService kiteService;
    private final DataCache dataCache;
    private final OrderConfiguration orderConfiguration;
    private final FileUtils fileUtils;
    private final TimeProvider timeProvider;
    @Getter
    private final List<OpenOrder> openOrders = new ArrayList<>();
    @Getter
    private final List<ActiveOrder> activeOrders = new ArrayList<>();

    private final Map<String, Double> latestTickPrices = new HashMap<>();

    private int iter;

    @PostConstruct
    public void initialiseWebSocket() {
        final OnTicks onTickerArrivalListener = ticks -> {
            if (!ticks.isEmpty()) {
                handleTicks(ticks);
            }
        };

        kiteService.setOnTickerArrivalListener(onTickerArrivalListener);
        final ArrayList<Long> initialTokens = Arrays.stream(orderConfiguration.getWebSocketDefaultSymbols())
                .map(dataCache::getInstrument)
                .collect(Collectors.toCollection(ArrayList::new));
        kiteService.appendWebSocketTokensList(initialTokens);
    }

    public void appendOpenOrder(OpenOrder order) {
        if(isNotInActiveOrders(order)) {
            openOrders.removeIf(o -> o.equals(order));
            openOrders.add(order);
            addTokenToWebsocket(order);
        }
    }

    @VisibleForTesting
    void handleTicks(ArrayList<Tick> ticks) {
        for(Tick tick: ticks) {
            try {
                String tickSymbol = dataCache.getSymbol(tick.getInstrumentToken());
                latestTickPrices.put(tickSymbol, tick.getLastTradedPrice());
                checkActiveOrdersStatus(tick, tickSymbol);
                checkEntryInOpenOrders(tick, tickSymbol);
                fileUtils.appendTickToFile(tickSymbol, tick);
            } catch (Exception e) {
                log.warn("Failed to apply tick information for tick: {}", tick.getInstrumentToken(), e);
            }
        }
    }

    private void checkEntryInOpenOrders(Tick tick, String tickSymbol) {
        if(openOrders.isEmpty()) {
            return;
        }

        Optional<OpenOrder> tickOrderOptional = openOrders.stream().filter(e -> e.getIndex().contentEquals(tickSymbol)).findAny();
        if (tickOrderOptional.isPresent() && iter++ % 25 == 0) {
            OpenOrder tickOrder = tickOrderOptional.get();
            if(isNotInActiveOrders(tickOrder)) {
                log.debug("token {} has an open order, buyAt: {}, ltp: {}, ce : {}", tickSymbol, tickOrder.getBuyThreshold(), tick.getLastTradedPrice(), tickOrder.isCallOrder());
            } else {
                log.debug("already active order present for token: {}", tickSymbol);
            }
        }

        List<OpenOrder> tickSymbolOrders = openOrders
                .stream()
                .filter(e -> e.getIndex().contentEquals(tickSymbol) && isNotInActiveOrders(e))
                .toList();

        for (OpenOrder order : tickSymbolOrders) {
            verifyAndPlaceOrder(tick, order);
        }
    }

    private boolean isNotInActiveOrders(OpenOrder tickOpenOrder) {
        return activeOrders
                .stream()
                .noneMatch(a -> a.getTag().equalsIgnoreCase(tickOpenOrder.getTag()) && a.getIndex().equalsIgnoreCase(tickOpenOrder.getIndex()));
    }

    private void checkActiveOrdersStatus(Tick tick, String tickSymbol) {
        List<ActiveOrder> activeOrdersForTick = activeOrders.stream().filter(o -> o.getIndex().contentEquals(tickSymbol)).toList();

        if(activeOrdersForTick.isEmpty()) {
            return;
        }
        int timestampIndex = timeProvider.currentTimeStampIndex();

        for(ActiveOrder order : activeOrdersForTick) {
            if(order.isCallOrder()) {
                if(order.getTarget() < tick.getLastTradedPrice() || order.getStopLoss() > tick.getLastTradedPrice() || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX) {
                    log.info("Exiting call order for : {}", order.getOptionSymbol());
                    sellOrder(tick, order);
                }
            } else {
                if(order.getTarget() > tick.getLastTradedPrice() || order.getStopLoss() < tick.getLastTradedPrice() || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX){
                    log.info("Exiting put order for : {}", order.getOptionSymbol());
                    sellOrder(tick, order);
                }
            }
        }

        List<ActiveOrder> soldOrders = activeOrdersForTick.stream().filter(a -> !a.isActive()).toList();
        for(ActiveOrder order: soldOrders) {
            log.debug("Removing sold order: {}", order);
            activeOrders.remove(order);
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
        newToken.add(dataCache.getInstrument(order.getIndex()));
        newToken.add(dataCache.getInstrument(order.getOptionSymbol()));
        kiteService.appendWebSocketTokensList(newToken);
    }

    private void sellOrder(Tick tick, ActiveOrder order) {
        log.info("kite service : {}", kiteService);
        boolean orderSold = kiteService.sellOrder(order.getOptionSymbol(), tick.getLastTradedPrice(), order.getQuantity(), order.getTag(),  isPlaceOrder(order));
        log.info("Sold status : {}", orderSold);
        if(orderSold) {
            order.setActive(false);
            order.setSellOptionPrice(latestTickPrices.getOrDefault(order.getOptionSymbol(), -1d));
            order.setExitDatetime(new Date());
            order.closeOrder(tick.getLastTradedPrice(), timeProvider.currentTimeStampIndex());
            fileUtils.logCompletedOrder(order);
        } else {
            log.error("########################################################");
            log.error("FAILED TO EXIT ORDER, PLEASE EXIT ORDER MANUALLY...");
            log.error("order : {}", order);
            log.error("########################################################");
        }
    }

    // TODO: add an additional check to ensure order is not placed against wrong instrument.
    private void verifyAndPlaceOrder(Tick tick, OpenOrder order) {
        int timestamp = timeProvider.currentTimeStampIndex();

        if(order.isCallOrder()) {
            if (tick.getLastTradedPrice() > order.getBuyThreshold()) {
                log.info("tick ltp: {} is greater than buy threshold {}, placing order({}) : {}",
                        tick.getLastTradedPrice(), order.getBuyThreshold(), order.getOptionSymbol(), order);
                placeOrder(tick, order, timestamp);
            }
        } else {
            if (tick.getLastTradedPrice() < order.getBuyThreshold()) {
                log.info("tick ltp: {} is lesser than buy threshold {}, placing order({}) : {}",
                        tick.getLastTradedPrice(), order.getBuyThreshold(), order.getOptionSymbol(), order);
                placeOrder(tick, order, timestamp);
            }
        }
    }

    private void placeOrder(Tick tick, OpenOrder order, int timestamp) {
        ActiveOrder activeOrder = createActiveOrder(order, tick, timestamp);
        if(!activeOrders.contains(activeOrder)) {
            activeOrder.setEntryDatetime(new Date());
            // getting null pointer if the tick didn't come
            if(latestTickPrices.containsKey(activeOrder.getOptionSymbol())) {
                // TODO: got Null pointer exception, for 341249
                activeOrder.setBuyOptionPrice(latestTickPrices.get(activeOrder.getOptionSymbol()));
            } else {
                log.warn("Latest tick price for symbol: {} not present in latest tick prices: {}",
                        activeOrder.getOptionSymbol(), latestTickPrices.keySet());
            }
            log.debug("Placing an order for index: {}, symbol: {}, at buyThreshold: {}", order.getIndex(), order.getOptionSymbol(), order.getBuyThreshold());
            boolean orderPlaced = kiteService.buyOrder(order.getOptionSymbol(), tick.getLastTradedPrice(), order.getQuantity(), order.getTag(), isPlaceOrder(activeOrder));
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

    // TODO: dont stop sell order by price (but the condition is only for buyPrice :thinking)
    @VisibleForTesting
    boolean isPlaceOrder(ActiveOrder order) {
        int ordersCount = activeOrders.stream().filter(o -> o.getIndex().equals(NIFTY_BANK)).toList().size();
        boolean isPlaceOrder = order.getIndex().equals(NIFTY_BANK)
                && (order.getQuantity() * order.getBuyOptionPrice() < orderConfiguration.getAvailableCash());
        if(!isPlaceOrder) {
            log.info("Not placing order as the symbol is : {} or amount is: {}, ordersCount(NIFTY_BANK) : {}",
                    order.getOptionSymbol(), (order.getQuantity() * order.getBuyOptionPrice()), ordersCount);
        }
        return isPlaceOrder;
    }

    private ActiveOrder createActiveOrder(OpenOrder openOrder, Tick tick, int timestamp) {
        return ActiveOrderFactory.createOrder(openOrder, tick.getLastTradedPrice(), timestamp);
    }
}
