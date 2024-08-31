package com.vish.fno.manage.config.task;

import com.vish.fno.manage.model.StrategyTasks;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("strategies")
public class TaskProperties {
    private List<StrategyTasks> indexStrategyList;
    private List<StrategyTasks> optionStrategyList;
}
