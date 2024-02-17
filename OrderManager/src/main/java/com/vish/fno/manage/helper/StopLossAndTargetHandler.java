package com.vish.fno.manage.helper;

import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.reader.service.KiteService;
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
    private final TimeProvider timeProvider;
    private final KiteService kiteService;
    private final TargetHandler targetHandler;
    private final StopLossHandler stopLossHandler;

    private static final int INTRADAY_EXIT_POSITION_TIME_INDEX = 368;

    public Optional<ActiveOrder> getActiveOrderToSell(Tick tick, List<ActiveOrder> activeOrders) {
        String tickSymbol = kiteService.getSymbol(tick.getInstrumentToken());

        List<ActiveOrder> activeOrdersForTick = activeOrders.stream().filter(o -> o.getIndex().contentEquals(tickSymbol)).toList();

        if(activeOrdersForTick.isEmpty()) {
            return Optional.empty();
        }

        int timestampIndex = timeProvider.currentTimeStampIndex();

        for(ActiveOrder order : activeOrdersForTick) {
            if(order.isCallOrder()) {
                if(isCallExitCondition(tick, timestampIndex, order)) {
                    log.info("Exiting call order for : {}", order.getOptionSymbol());
                    return Optional.of(order);
                }
            } else {
                if(isPutExitCondition(tick, timestampIndex, order)){
                    log.info("Exiting put order for : {}", order.getOptionSymbol());
                    return Optional.of(order);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isPutExitCondition(Tick tick, int timestampIndex, ActiveOrder order) {
        return targetHandler.isPutTargetAchieved(order, tick.getLastTradedPrice())
                || stopLossHandler.isPutStopLossHit(order, tick.getLastTradedPrice())
                || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX;
    }

    private boolean isCallExitCondition(Tick tick, int timestampIndex, ActiveOrder order) {
        return targetHandler.isCallTargetAchieved(order, tick.getLastTradedPrice())
                || stopLossHandler.isCallStopLossHit(order, tick.getLastTradedPrice())
                || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX;
    }
}
