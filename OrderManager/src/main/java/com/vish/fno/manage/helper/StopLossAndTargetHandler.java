package com.vish.fno.manage.helper;

import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.util.orderflow.TargetAndStopLossStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public final class StopLossAndTargetHandler {

    private static final int INTRADAY_EXIT_POSITION_TIME_INDEX = 368;

    private final TargetAndStopLossStrategy targetAndStopLossHandler;

    public Optional<ActiveOrder> getActiveOrderToSell(String tickSymbol, double ltp, int timestampIndex, List<ActiveOrder> activeOrders) {

        List<ActiveOrder> activeOrdersForTick = activeOrders.stream().filter(o -> o.getIndex().contentEquals(tickSymbol)).toList();

        if(activeOrdersForTick.isEmpty()) {
            return Optional.empty();
        }

        for(ActiveOrder order : activeOrdersForTick) {
            if(isExitCondition(ltp, timestampIndex, order)) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    private boolean isExitCondition(double ltp, int timestampIndex, ActiveOrder order) {
        return targetAndStopLossHandler.isTargetAchieved(order, ltp)
                || targetAndStopLossHandler.isStopLossHit(order, ltp)
                || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX;
    }
}
