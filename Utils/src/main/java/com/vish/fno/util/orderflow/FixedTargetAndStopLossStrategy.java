package com.vish.fno.util.orderflow;

import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.OrderSellDetailModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedTargetAndStopLossStrategy implements TargetAndStopLossStrategy {

    @Override
    public OrderSellDetailModel isTargetAchieved(ActiveOrder order, double ltp) {
        if(order.isCallOrder() && order.getTarget() < ltp) {

            log.info("Call target achieved for order : {} ltp: {}", order, ltp);
            return new OrderSellDetailModel(true, order.getBuyQuantity(), order);
        }
        if(!order.isCallOrder() && order.getTarget() > ltp) {
            log.info("Put target achieved for order : {} ltp: {}", order, ltp);
            return new OrderSellDetailModel(true, order.getBuyQuantity(), order);
        }
        return new OrderSellDetailModel(false);
    }

    @Override
    public OrderSellDetailModel isStopLossHit(ActiveOrder order, double ltp) {
        if(order.isCallOrder() && order.getStopLoss() > ltp) {
            log.info("Call stopLoss hit for order : {} ltp: {}", order, ltp);
            return new OrderSellDetailModel(true, order.getBuyQuantity(), order);
        }
        if(!order.isCallOrder() && order.getStopLoss() < ltp) {
            log.info("Put stopLoss hit for order : {} ltp: {}", order, ltp);
            return new OrderSellDetailModel(true, order.getBuyQuantity(), order);
        }
        return new OrderSellDetailModel(false);
    }
}
