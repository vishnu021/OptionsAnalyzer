package com.vish.fno.manage.model;

import com.vish.fno.model.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StrategyTasks implements Task {
    private String strategyName;
    private String index;
    private boolean enabled;
    private boolean expiryDayOrders;
}
