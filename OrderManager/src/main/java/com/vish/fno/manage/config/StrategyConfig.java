package com.vish.fno.manage.config;

import com.vish.fno.manage.config.task.TaskConfig;
import com.vish.fno.model.cache.OrderCache;
import com.vish.fno.model.strategy.MinuteStrategy;
import com.vish.fno.sutils.orderflow.PartialRevisingStopLoss;
import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.manage.orderflow.OrderHandler;
import com.vish.fno.model.strategy.Strategy;
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
        List<MinuteStrategy> indexStrategies = getStrategies(taskConfig.getTaskProperties().getIndexStrategyList());
        List<MinuteStrategy> optionStrategies = getStrategies(taskConfig.getTaskProperties().getOptionStrategyList());
        return new StrategyExecutor(kiteService, orderHandler, dataCache, orderCache, indexStrategies, optionStrategies, timeProvider);
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

    @Bean
    public TargetAndStopLossStrategy targetHandler(DataCache dataCache) {
        return new PartialRevisingStopLoss(dataCache);
    }
}
