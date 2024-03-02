package com.vish.fno.manage.helper;

import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.util.orderflow.sl.StopLossHandler;
import com.vish.fno.util.orderflow.target.TargetHandler;
import com.zerodhatech.models.Tick;
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

    private final TargetHandler targetHandler;
    private final StopLossHandler stopLossHandler;

    public Optional<ActiveOrder> getActiveOrderToSell(String tickSymbol, double ltp, int timestampIndex, List<ActiveOrder> activeOrders) {

        List<ActiveOrder> activeOrdersForTick = activeOrders.stream().filter(o -> o.getIndex().contentEquals(tickSymbol)).toList();

        if(activeOrdersForTick.isEmpty()) {
            return Optional.empty();
        }

        for(ActiveOrder order : activeOrdersForTick) {
            if(order.isCallOrder()) {
                if(isCallExitCondition(ltp, timestampIndex, order)) {
                    log.info("Exiting call order for : {}", order.getOptionSymbol());
                    return Optional.of(order);
                }
            } else {
                if(isPutExitCondition(ltp, timestampIndex, order)){
                    log.info("Exiting put order for : {}", order.getOptionSymbol());
                    return Optional.of(order);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isPutExitCondition(double ltp, int timestampIndex, ActiveOrder order) {
        return targetHandler.isPutTargetAchieved(order, ltp)
                || stopLossHandler.isPutStopLossHit(order, ltp)
                || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX;
    }

    private boolean isCallExitCondition(double ltp, int timestampIndex, ActiveOrder order) {
        return targetHandler.isCallTargetAchieved(order, ltp)
                || stopLossHandler.isCallStopLossHit(order, ltp)
                || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX;
    }
}
