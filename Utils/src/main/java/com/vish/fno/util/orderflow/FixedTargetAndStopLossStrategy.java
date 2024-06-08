package com.vish.fno.util.orderflow;

import com.vish.fno.model.order.ActiveOrder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedTargetAndStopLossStrategy implements TargetAndStopLossStrategy {
    @Override
    public boolean isTargetAchieved(ActiveOrder order, double ltp) {
        if(order.isCallOrder() && order.getTarget() < ltp) {
            log.info("Call target achieved for order : {} ltp: {}", order, ltp);
            return true;
        }
        if(!order.isCallOrder() && order.getTarget() > ltp) {
            log.info("Put target achieved for order : {} ltp: {}", order, ltp);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStopLossHit(ActiveOrder order, double ltp) {
        if(order.isCallOrder() && order.getStopLoss() > ltp) {
            log.info("Call stopLoss hit for order : {} ltp: {}", order, ltp);
            return true;
        }
        if(!order.isCallOrder() && order.getStopLoss() < ltp) {
            log.info("Put stopLoss hit for order : {} ltp: {}", order, ltp);
            return true;
        }
        return false;
    }
}
