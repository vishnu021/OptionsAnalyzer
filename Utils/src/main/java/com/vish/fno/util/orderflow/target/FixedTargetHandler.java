package com.vish.fno.util.orderflow.target;

import com.vish.fno.model.order.ActiveOrder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedTargetHandler implements TargetHandler {
    public boolean isCallTargetAchieved(ActiveOrder order, double ltp) {
        if(order.getTarget() < ltp) {
            log.info("Call target achieved for order : {} ltp: {}", order, ltp);
            return true;
        }
        return false;
    }

    public boolean isPutTargetAchieved(ActiveOrder order, double ltp) {
        if(order.getTarget() > ltp) {
            log.info("Put target achieved for order : {} ltp: {}", order, ltp);
            return true;
        }
        return false;
    }
}
