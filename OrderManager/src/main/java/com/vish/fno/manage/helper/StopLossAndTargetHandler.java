package com.vish.fno.manage.helper;

import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.reader.helper.InstrumentCache;
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
    private final InstrumentCache instrumentCache;

    private static final int INTRADAY_EXIT_POSITION_TIME_INDEX = 368;

    public Optional<ActiveOrder> getActiveOrderToSell(Tick tick, List<ActiveOrder> activeOrders) {
        String tickSymbol = instrumentCache.getSymbol(tick.getInstrumentToken());

        List<ActiveOrder> activeOrdersForTick = activeOrders.stream().filter(o -> o.getIndex().contentEquals(tickSymbol)).toList();

        if(activeOrdersForTick.isEmpty()) {
            return Optional.empty();
        }

        int timestampIndex = timeProvider.currentTimeStampIndex();

        for(ActiveOrder order : activeOrdersForTick) {
            if(order.isCallOrder()) {
                if(isCallTargetAchieved(order, tick) || isCallStopLossHit(order, tick) || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX) {
                    log.info("Exiting call order for : {}", order.getOptionSymbol());
                    return Optional.of(order);
                }
            } else {
                if(isPutTargetAchieved(order, tick) || isPutStopLossHit(order, tick) || timestampIndex > INTRADAY_EXIT_POSITION_TIME_INDEX){
                    log.info("Exiting put order for : {}", order.getOptionSymbol());
                    return Optional.of(order);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isPutStopLossHit(ActiveOrder order, Tick tick) {
        if(order.getStopLoss() < tick.getLastTradedPrice()) {
            log.info("Put stoploss hit for order : {} ltp: {}", order, tick.getLastTradedPrice());
            return true;
        }
        return false;
    }
    private boolean isCallTargetAchieved(ActiveOrder order, Tick tick) {
        if(order.getTarget() < tick.getLastTradedPrice()) {
            log.info("Call target achieved for order : {} ltp: {}", order, tick.getLastTradedPrice());
            return true;
        }
        return false;
    }

    private boolean isCallStopLossHit(ActiveOrder order, Tick tick) {
        if(order.getStopLoss() > tick.getLastTradedPrice()) {
            log.info("Call stoploss hit for order : {} ltp: {}", order, tick.getLastTradedPrice());
            return true;
        }
        return false;
    }


    private boolean isPutTargetAchieved(ActiveOrder order, Tick tick) {
        if(order.getTarget() > tick.getLastTradedPrice()) {
            log.info("Put target achieved for order : {} ltp: {}", order, tick.getLastTradedPrice());
            return true;
        }
        return false;
    }
}
