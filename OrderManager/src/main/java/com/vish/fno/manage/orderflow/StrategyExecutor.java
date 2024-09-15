package com.vish.fno.manage.orderflow;

import com.vish.fno.model.Candle;
import com.vish.fno.model.strategy.MinuteStrategy;
import com.vish.fno.model.strategy.OptionBasedStrategy;
import com.vish.fno.model.cache.OrderCache;
import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.reader.service.KiteService;
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
    private final OrderCache orderCache;
    private final List<MinuteStrategy> strategies;
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
        orderCache.removeExpiredOpenOrders(timeProvider.currentTimeStampIndex());

        strategies.forEach(this::executeStrategy);
        orderCache.logOpenOrders();
    }

    private void executeStrategy(MinuteStrategy strategy) {
        try {
            if (strategy instanceof OptionBasedStrategy) {
                executeOptionStrategy((OptionBasedStrategy) strategy);
            } else {
                processSymbolData(strategy.getTask().getIndex(), strategy);
            }
        } catch (Exception e) {
            log.error("Failed to execute strategy: {}", strategy, e);
        }
    }

    @SuppressWarnings("PMD.PrematureDeclaration")
    private void executeOptionStrategy(OptionBasedStrategy strategy) {
        try {
            String indexOptionValue = strategy.getTask().getIndex();

            String[] symbolOptionPair = indexOptionValue.split("\\|");
            if (symbolOptionPair.length < 2) {
                log.error("Invalid index option value: {} for strategy: {}", indexOptionValue, strategy);
                return;
            }

            String index = symbolOptionPair[0];
            String optionDetail = symbolOptionPair[1];

            List<Candle> indexData = dataCacheImpl.updateAndGetMinuteData(index);
            if (indexData == null || indexData.isEmpty()) {
                log.error("Index data is null or empty for index: {} in strategy: {}", index, strategy);
                return;
            }

            double lastPrice = indexData.get(indexData.size() - 1).getClose();
            processOptionData(strategy, index, lastPrice, optionDetail);
        } catch (Exception e) {
            log.error("Failed to execute option strategy: {}", strategy, e);
        }
    }

// better create 2 strategies for 1 option strategy
    private void processOptionData(OptionBasedStrategy strategy, String index, double lastPrice, String optionDetail) {
        final String callSymbol = getOptionSymbol(index, lastPrice, optionDetail, true);
        final String putSymbol = getOptionSymbol(index, lastPrice, optionDetail, false);
        strategy.setSymbol(callSymbol);
        processSymbolData(callSymbol, strategy);
        strategy.setSymbol(putSymbol);
        processSymbolData(putSymbol, strategy);
    }

    private String getOptionSymbol(String index, double lastPrice, String optionDetail, boolean isCall) {
        if ("ITM".equals(optionDetail)) {
            return kiteService.getITMStock(index, lastPrice, isCall);
        } else {
            return kiteService.getOTMStock(index, lastPrice, isCall);
        }
    }

    private void processSymbolData(String symbol, MinuteStrategy strategy) {
        Optional.ofNullable(dataCacheImpl.updateAndGetMinuteData(symbol))
                .ifPresentOrElse(
                        data -> strategy.test(data, timeProvider.currentTimeStampIndex())
                                .ifPresent(orderHandler::appendOpenOrder),
                        () -> log.error("Data is null for symbol: {}, strategy: {}", symbol, strategy)
                );
    }

    boolean isWithinTradingHours(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        return !now.toLocalTime().isBefore(START_TRADING_HOUR) && !now.toLocalTime().isAfter(END_TRADING_HOUR);
    }
}
