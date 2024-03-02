package com.vish.fno.manage;

import com.vish.fno.util.helper.CandleStickCache;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.Strategy;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.SymbolData;
import com.vish.fno.model.order.OrderRequest;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class StrategyExecutor {

    private final KiteService kiteService;
    private final CandlestickService candlestickService;
    private final OrderHandler orderHandler;
    private final CandleStickCache candleStickCache;
    private final List<Strategy> activeStrategies;
    @Getter
    private final List<String> symbolList;
    private final TimeProvider timeProvider;
    private final String todaysDate = TimeUtils.getTodayDate();

    private static final LocalTime START_TRADING_HOUR = LocalTime.of(9, 16);
    private static final LocalTime END_TRADING_HOUR = LocalTime.of(15, 30);
    private boolean itmOptionsAppended;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void update() {
        // will expire the session and fail the app if the cron job runs before initialization
        if(!(isWithinTradingHours(timeProvider.now()) && kiteService.isInitialised())) {
            return;
        }

        if(!itmOptionsAppended) {
            itmOptionsAppended = kiteService.appendIndexITMOptions();
        }

        updateIntradayCache();
        orderHandler.removeExpiredOpenOrders(timeProvider.currentTimeStampIndex());
        for(Strategy strategy: activeStrategies) {
            try {
                testStrategies(strategy);
            } catch (Exception e) {
                log.error("exception while running strategy : {}", strategy.getTask(), e);
            }
        }
        logOpenOrders();
    }

    private void logOpenOrders() {
        final List<OrderRequest> existingOrderRequests = orderHandler.getOrderRequests();
        if(!existingOrderRequests.isEmpty()) {
            log.info("list of open orders({}) : {}", existingOrderRequests.size(), existingOrderRequests);
        }
        final List<ActiveOrder> activeOrders = orderHandler.getActiveOrders();
        if(!activeOrders.isEmpty()) {
            log.info("list of active orders({}) : {}", activeOrders.size(), activeOrders);
        }
    }

    private void testStrategies(Strategy strategy) {
        String symbol = strategy.getTask().getIndex();

        if(symbol == null || candleStickCache.get(symbol) == null) {
            log.error("symbol or task is null for strategy: {}", strategy);
            return;
        }

        Optional<? extends OrderRequest> openOrderOptional = strategy.test(candleStickCache.get(symbol), timeProvider.currentTimeStampIndex());
        openOrderOptional.ifPresent(o -> {
            String itmOptionSymbol = kiteService.getITMStock(o.getIndex(), o.getBuyThreshold(), o.isCallOrder());
            o.setOptionSymbol(itmOptionSymbol);
            orderHandler.appendOpenOrder(o);
        });
    }

    boolean isWithinTradingHours(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !now.toLocalTime().isBefore(START_TRADING_HOUR) && !now.toLocalTime().isAfter(END_TRADING_HOUR);
    }

    private void updateIntradayCache() {
        log.info("updating intraday cache for: {} ", symbolList);
        for(String symbol: symbolList) {
            try {
                Optional<SymbolData> candleStickData = candlestickService.getEntireDayHistoryData(todaysDate, symbol);
                candleStickCache.update(symbol, candleStickData);
            } catch (Exception e) {
                log.warn("Failed to update cache for symbol: {}", symbol, e);
            }
        }
    }
}
