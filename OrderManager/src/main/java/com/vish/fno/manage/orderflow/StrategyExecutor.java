package com.vish.fno.manage.orderflow;

import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.model.Strategy;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.reader.service.KiteService;
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
    private final OrderHandler orderHandler;
    private final DataCache dataCacheImpl;
    private final List<Strategy> activeStrategies;
    @Getter
    private final List<String> symbolList;
    private final TimeProvider timeProvider;

    private static final LocalTime START_TRADING_HOUR = LocalTime.of(9, 16);
    private static final LocalTime END_TRADING_HOUR = LocalTime.of(15, 30);

    @Scheduled(cron = "0 0/1 * * * ?")
    public void update() {
        // will expire the session and fail the app if the cron job runs before initialization
        if(!(isWithinTradingHours(timeProvider.now()) && kiteService.isInitialised())) {
            return;
        }

        kiteService.appendIndexITMOptions();
        orderHandler.removeExpiredOpenOrders();
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
        final String symbol = strategy.getTask().getIndex();
        if(symbol == null) {
            log.error("symbol is null for strategy: {}", strategy);
            return;
        }
        Optional.ofNullable(dataCacheImpl.updateAndGetMinuteData(symbol))
                .ifPresentOrElse(
                        data -> strategy.test(data, timeProvider.currentTimeStampIndex()).ifPresent(orderHandler::appendOpenOrder),
                        () -> log.error("data is null for symbol: {}, strategy: {}", symbol, strategy));
    }

    boolean isWithinTradingHours(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !now.toLocalTime().isBefore(START_TRADING_HOUR) && !now.toLocalTime().isAfter(END_TRADING_HOUR);
    }
}
