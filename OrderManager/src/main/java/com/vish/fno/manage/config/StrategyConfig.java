package com.vish.fno.manage.config;

import com.vish.fno.manage.config.task.TaskConfig;
import com.vish.fno.manage.helper.DataCacheImpl;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.manage.OrderHandler;
import com.vish.fno.model.Strategy;
import com.vish.fno.manage.StrategyExecutor;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.orderflow.FixedTargetAndStopLossStrategy;
import com.vish.fno.util.orderflow.TargetAndStopLossStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class StrategyConfig {

    private final ApplicationContext context;
    private final TaskConfig taskConfig;

    @Bean
    public StrategyExecutor strategyExecutor(KiteService kiteService,
                                             OrderHandler orderHandler,
                                             DataCacheImpl dataCacheImpl,
                                             TimeProvider timeProvider) {
        List<Strategy> activeStrategies = getStrategies();
        List<String> symbolList =  taskConfig.getTaskProperties().getList().stream()
                .map(StrategyTasks::getIndex)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        log.info("Initialising strategy executor with symbols: {}", symbolList);
        return new StrategyExecutor(kiteService, orderHandler, dataCacheImpl, activeStrategies, symbolList, timeProvider);
    }

    private List<Strategy> getStrategies() {
        List<Strategy> strategyList = new ArrayList<>();
        for(StrategyTasks task: taskConfig.getTaskProperties().getList()) {
            log.info("Adding strategy: {} for symbol: {} (enabled={}) to the active strategies", task.getStrategyName(), task.getIndex(), task.isEnabled());
            Strategy strategy = context.getBean(task.getStrategyName(), Strategy.class);
            strategy.initialise(task);
            strategyList.add(strategy);
        }
        return strategyList;
    }

    @Bean
    public TargetAndStopLossStrategy targetHandler() {
        return new FixedTargetAndStopLossStrategy();
    }
}
