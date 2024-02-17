package com.vish.fno.util.orderflow.sl;

import com.vish.fno.model.order.ActiveOrder;

public interface StopLossHandler {
    boolean isCallStopLossHit(ActiveOrder order, double ltp);
    boolean isPutStopLossHit(ActiveOrder order, double ltp);
}
