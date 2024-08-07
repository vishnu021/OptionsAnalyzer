package com.vish.fno.config;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.config.order.OrderProperties;
import com.vish.fno.manage.config.task.TaskConfig;
import com.vish.fno.manage.helper.DataCacheImpl;
import com.vish.fno.model.helper.OrderCache;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.manage.orderflow.OrderHandler;
import com.vish.fno.manage.orderflow.StrategyExecutor;
import com.vish.fno.manage.service.CalendarService;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.Strategy;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.FileUtils;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.util.orderflow.FixedTargetAndStopLossStrategy;
import com.vish.fno.util.orderflow.TargetAndStopLossStrategy;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.vish.fno.manage.IntegrationTest.TEST_DATE;
import static org.mockito.Mockito.when;

@Slf4j
@TestConfiguration
@EnableConfigurationProperties(OrderProperties.class)
public class TestConfig {
    @Autowired
    private ApplicationContext context;
    private static final LocalDateTime startTime = LocalDateTime.of(2024, 8, 2, 9, 15);

    @Bean
    public TimeProvider timeProvider() {
        TimeProvider timeProvider =  Mockito.mock(TimeProvider.class);
        when(timeProvider.getTodaysDateString()).thenReturn(TEST_DATE);
        when(timeProvider.now()).thenReturn(startTime);
        Date date = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
        when(timeProvider.currentTimeStampIndex()).thenReturn(TimeUtils.getIndexOfTimeStamp(date));
        return timeProvider;
    }

    public static void mockTimeProvider(TimeProvider timeProvider, int i) {
        LocalDateTime mockTime = startTime.plusMinutes(i);
        when(timeProvider.now()).thenReturn(mockTime);
        Date date = Date.from(mockTime.atZone(ZoneId.systemDefault()).toInstant());
        when(timeProvider.currentTimeStampIndex()).thenReturn(TimeUtils.getIndexOfTimeStamp(date));
    }

    @Bean
    public OrderConfiguration testOrderConfiguration(OrderProperties orderProperties) {
        return new OrderConfiguration(orderProperties);
    }

    @Bean
    public DataCache dataCache(CandlestickService candlestickService, CalendarService calendarService, TimeProvider timeProvider) {
        return new DataCacheImpl(candlestickService, calendarService, timeProvider);
    }

    @Bean
    public TargetAndStopLossStrategy targetHandler() {
        return new FixedTargetAndStopLossStrategy();
    }

    @Bean
    public OrderHandler testOrderHandler(KiteService kiteService, OrderConfiguration testOrderConfiguration, OrderCache orderCache, TimeProvider timeProvider) {
        return new OrderHandler(kiteService, testOrderConfiguration, new FileUtils(), timeProvider, new FixedTargetAndStopLossStrategy(), orderCache);
    }

    @Bean
    public StrategyExecutor testStrategyExecutor(KiteService kiteService,
                                                 OrderHandler testOrderHandler,
                                                 DataCache dataCache,
                                                 OrderCache orderCache,
                                                 TimeProvider timeProvider,
                                                 TaskConfig taskConfig) {
        List<Strategy> activeStrategies = getStrategies(taskConfig);
        List<String> symbolList = activeStrategies.stream().map(s -> s.getTask().getIndex()).toList();
        return new StrategyExecutor(kiteService, testOrderHandler, dataCache, orderCache, activeStrategies, symbolList, timeProvider);
    }
    private List<Strategy> getStrategies(TaskConfig taskConfig) {
        List<Strategy> strategyList = new ArrayList<>();
        for(StrategyTasks task: taskConfig.getTaskProperties().getList()) {
            log.info("Adding strategy: {} for symbol: {} (enabled={}) to the active strategies", task.getStrategyName(), task.getIndex(), task.isEnabled());
            Strategy strategy = context.getBean(task.getStrategyName(), Strategy.class);
            strategy.initialise(task);
            strategyList.add(strategy);
        }
        return strategyList;
    }
}
