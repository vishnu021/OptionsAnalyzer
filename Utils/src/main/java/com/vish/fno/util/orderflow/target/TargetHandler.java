package com.vish.fno.util.orderflow.target;

import com.vish.fno.model.order.ActiveOrder;

public interface TargetHandler {
    boolean isCallTargetAchieved(ActiveOrder order, double ltp);
    boolean isPutTargetAchieved(ActiveOrder order, double ltp);
}
