package com.vish.fno.manage.config;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.config.task.TaskConfig;
import com.vish.fno.manage.orderflow.TickStrategyExecutor;
import com.vish.fno.model.cache.OrderCache;
import com.vish.fno.model.strategy.MinuteStrategy;
import com.vish.fno.model.strategy.TickBasedStrategy;
import com.vish.fno.sutils.orderflow.PartialRevisingStopLoss;
import com.vish.fno.util.FileUtils;
import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.manage.orderflow.OrderHandler;
import com.vish.fno.manage.orderflow.StrategyExecutor;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.orderflow.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class StrategyConfig {

    private final ApplicationContext context;
    private final TaskConfig taskConfig;

    @Bean
    public StrategyExecutor strategyExecutor(final KiteService kiteService,
                                             final OrderHandler orderHandler,
                                             final DataCache dataCache,
                                             final OrderCache orderCache,
                                             final TimeProvider timeProvider) {
        List<MinuteStrategy> strategies = Stream.concat(
                getStrategies(taskConfig.getTaskProperties().getIndexStrategyList()).stream(),
                getStrategies(taskConfig.getTaskProperties().getOptionStrategyList()).stream()
        ).collect(Collectors.toList());

        return new StrategyExecutor(kiteService, orderHandler, dataCache, orderCache, strategies, timeProvider);
    }

    @Bean
    public TickStrategyExecutor tickStrategyExecutor(final TimeProvider timeProvider) {
        List<TickBasedStrategy> strategies = getTickStrategies(taskConfig.getTaskProperties().getTickStrategyList());
        return new TickStrategyExecutor(strategies, timeProvider);
    }

    @Bean
    public OrderHandler orderHandler(final KiteService kiteService,
                                     final OrderConfiguration orderConfiguration,
                                     final FileUtils fileUtils,
                                     final TimeProvider timeProvider,
                                     final TargetAndStopLossStrategy targetAndStopLossStrategy,
                                     final OrderCache orderCache,
                                     final TickStrategyExecutor tickStrategyExecutor) {
        return new OrderHandler(kiteService, orderConfiguration, fileUtils, timeProvider, targetAndStopLossStrategy, orderCache, tickStrategyExecutor);
    }

    private List<MinuteStrategy> getStrategies(List<StrategyTasks> strategyTasks) {
        return Optional.ofNullable(strategyTasks)
                .orElse(List.of())
                .stream()
                .map(task -> {
                    log.info("Adding task: {} to strategies", task);
                    MinuteStrategy strategy = context.getBean(task.getStrategyName(), MinuteStrategy.class);
                    strategy.initialise(task);
                    return strategy;
                })
                .collect(Collectors.toList());
    }

    private List<TickBasedStrategy> getTickStrategies(List<StrategyTasks> strategyTasks) {
        return Optional.ofNullable(strategyTasks)
                .orElse(List.of())
                .stream()
                .map(task -> {
                    log.info("Adding task: {} to tick strategies", task);
                    TickBasedStrategy strategy = context.getBean(task.getStrategyName(), TickBasedStrategy.class);
                    strategy.initialise(task);
                    return strategy;
                })
                .collect(Collectors.toList());
    }

    @Bean
    public TargetAndStopLossStrategy targetHandler(DataCache dataCache) {
        return new PartialRevisingStopLoss(dataCache);
    }
}
