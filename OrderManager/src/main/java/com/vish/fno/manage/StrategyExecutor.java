package com.vish.fno.manage;

import com.vish.fno.manage.helper.DataCache;
import com.vish.fno.manage.helper.TimeProvider;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.Candle;
import com.vish.fno.model.Strategy;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.SymbolData;
import com.vish.fno.model.order.OpenOrder;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

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
    private final DataCache dataCache;
    private final List<Strategy> activeStrategies;
    @Getter
    private final List<String> symbolList;
    private final TimeProvider timeProvider;

    private final Map<String, List<Candle>> candlesCache = new HashMap<>();
    private final String todaysDate = TimeUtils.getTodayDate();

    private static final LocalTime START_TRADING_HOUR = LocalTime.of(9, 15);
    private static final LocalTime END_TRADING_HOUR = LocalTime.of(15, 30);

    // TODO: add external schedular as well, and delay by 1 or 2 seconds
    @Scheduled(cron = "0 0/1 * * * ?")
    public void update() {
        // will expire the session and fail the app if the cron job runs before initialization
        if(!(isWithinTradingHours(timeProvider.now()) && kiteService.isInitialised())) {
            return;
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
        final List<OpenOrder> existingOpenOrders = orderHandler.getOpenOrders();
        if(!existingOpenOrders.isEmpty()) {
            log.info("list of open orders({}) : {}", existingOpenOrders.size(), existingOpenOrders);
        }
        final List<ActiveOrder> activeOrders = orderHandler.getActiveOrders();
        if(!activeOrders.isEmpty()) {
            log.info("list of active orders({}) : {}", activeOrders.size(), activeOrders);
        }
    }

    private void testStrategies(Strategy strategy) {
        int timestampIndex = timeProvider.currentTimeStampIndex();

        String symbol = strategy.getTask().getIndex();

        if(symbol == null || candlesCache.get(symbol) == null) {
            log.error("symbol or task is null for strategy: {}", strategy);
        } else {
            Optional<OpenOrder> indexOrderOptional = strategy.test(candlesCache.get(symbol), timestampIndex);
            indexOrderOptional.ifPresent(o -> {
                String itmOptionSymbol = dataCache.getITMStock(o.getIndex(), o.getBuyThreshold(), o.isCallOrder());
                o.setOptionSymbol(itmOptionSymbol);
                orderHandler.appendOpenOrder(o);
            });
        }
    }

    boolean isWithinTradingHours(LocalDateTime now) {
        return !now.toLocalTime().isBefore(START_TRADING_HOUR) && !now.toLocalTime().isAfter(END_TRADING_HOUR);
    }

    private void updateIntradayCache() {
        log.info("updating intraday cache for: {} ", symbolList);
        for(String symbol: symbolList) {
            try {
                Optional<SymbolData> candleStickData = candlestickService.getEntireDayHistoryData(todaysDate, symbol);
                candlesCache.remove(symbol);
                candleStickData.ifPresent(d -> candlesCache.put(symbol, d.getData()));
            } catch (Exception e) {
                log.warn("Failed to update cache for symbol: {}", symbol, e);
            }
        }
    }
}
