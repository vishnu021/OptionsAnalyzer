package com.vish.fno.util.orderflow;

import com.vish.fno.model.order.ActiveOrder;

public interface TargetAndStopLossStrategy {
    boolean isTargetAchieved(ActiveOrder order, double ltp);
    boolean isStopLossHit(ActiveOrder order, double ltp);
}
