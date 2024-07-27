package com.vish.fno.util.helper;

import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.OrderSellDetailModel;
import com.vish.fno.model.order.OrderSellReason;
import com.vish.fno.util.orderflow.TargetAndStopLossStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderManagerUtils {
    private static final int INTRADAY_EXIT_POSITION_TIME_INDEX = 368;

    public static OrderSellDetailModel isExitCondition(TargetAndStopLossStrategy targetAndStopLossStrategy,
                                                       double ltp,
                                                       int timestampIndex,
                                                       ActiveOrder order) {
        if(timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX) {
            final int quantityToSell = order.getBuyQuantity() - order.getSoldQuantity();
            log.info("Exit time reached, selling available {} quantity for symbol: {}, for order : {}",
                    quantityToSell, order.getIndex(), order);
            return new OrderSellDetailModel(true, quantityToSell, OrderSellReason.EXPIRY_TIME_REACHED, order);
        }

        final OrderSellDetailModel orderSellDetailModel = targetAndStopLossStrategy.isTargetAchieved(order, ltp);
        if(orderSellDetailModel.isSellOrder()) {
            return orderSellDetailModel;
        }

        return targetAndStopLossStrategy.isStopLossHit(order, ltp);
    }
}
