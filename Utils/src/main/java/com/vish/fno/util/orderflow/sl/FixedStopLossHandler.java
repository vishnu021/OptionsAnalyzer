package com.vish.fno.util.orderflow.sl;

import com.vish.fno.model.order.ActiveOrder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedStopLossHandler implements StopLossHandler {
    public boolean isPutStopLossHit(ActiveOrder order, double ltp) {
        if(order.getStopLoss() < ltp) {
            log.info("Put stopLoss hit for order : {} ltp: {}", order, ltp);
            return true;
        }
        return false;
    }

    public boolean isCallStopLossHit(ActiveOrder order, double ltp) {
        if(order.getStopLoss() > ltp) {
            log.info("Call stopLoss hit for order : {} ltp: {}", order, ltp);
            return true;
        }
        return false;
    }
}
