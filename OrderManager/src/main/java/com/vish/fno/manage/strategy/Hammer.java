package com.vish.fno.manage.strategy;

import com.vish.fno.model.Candle;
import com.vish.fno.model.IndexBasedStrategy;
import com.vish.fno.model.Task;
import com.vish.fno.model.order.orderrequest.IndexOrderRequest;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.technical.indicators.ma.ExponentialMovingAverage;
import com.vish.fno.util.CandleUtils;
import com.vish.fno.util.OptionsMetaDataUtils;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.vish.fno.util.TimeUtils.getTimeStringForZonedDateString;

// CPD-OFF
@Slf4j
@Component("Hammer")
@Scope(value = "prototype")
@SuppressWarnings("PMD")
public class Hammer implements IndexBasedStrategy {
    @Getter
    private final String tag = "Hammer";
    public static final double UPPER_WICK = 0.2d;
    public static final double LOWER_WICK = 0.66d;
    public static final double RISK_REWARD_RATIO = 1.5;
    public static final int MA_DURATION = 14;
    @Getter
    private Task task;
    private final ExponentialMovingAverage ema14Calculator;

    public Hammer() {
        this.ema14Calculator = new ExponentialMovingAverage(MA_DURATION);
    }

    @Override
    public void initialise(Task task) {
        this.task = task;
    }

    @Override
    public Optional<OrderRequest> test(final List<Candle> candles, final int timestamp) {
        if(candles.size() < MA_DURATION || timestamp > 360) {
            return Optional.empty();
        }

        final Candle latestCandle = candles.get(candles.size() - 1);
        final String currentDateTime = latestCandle.getTime();
        final String timeString = getTimeStringForZonedDateString(currentDateTime);
        final List<Double> ema14Values = ema14Calculator.calculate(candles);
        final Double ema14 = ema14Values.get(ema14Values.size() - 1);

        if(isHammerPattern(latestCandle) && ema14 < 0) {
            double buyThreshold = latestCandle.getHigh();
            double stopLoss = latestCandle.getLow();
            double risk = buyThreshold - stopLoss;
            double reward = risk * RISK_REWARD_RATIO;
            double target = buyThreshold + reward;
            IndexOrderRequest order = createOrderRequest(timestamp, currentDateTime, buyThreshold, target, stopLoss, true);
            log.info("HAMMER at : {}, candle : {}", latestCandle, timeString);
            return Optional.of(order);
        }

        if(isInvertedHammerPattern(latestCandle) && ema14 > 0) {
            double buyThreshold = latestCandle.getLow();
            double stopLoss = latestCandle.getHigh();
            double risk = stopLoss - buyThreshold;
            double reward = risk * RISK_REWARD_RATIO;
            double target = buyThreshold - reward;
            IndexOrderRequest order = createOrderRequest(timestamp, currentDateTime, buyThreshold, target, stopLoss, false);
            log.info("Inv-HAMMER at : {}, candle : {}", latestCandle, timeString);
            return Optional.of(order);
        }
        return Optional.empty();
    }

    private IndexOrderRequest createOrderRequest(int timestamp, String dateTime, double buyThreshold,
                                                 double target, double stopLoss, boolean callOrder) {
        final int lotSize = OptionsMetaDataUtils.getLotSize(task.getIndex());
        return IndexOrderRequest.builder(tag, task.getIndex(), task)
                .date(TimeUtils.getDateTimeObjectMinute(TimeUtils.getDateTimeStringForZonedDateString(dateTime)))
                .timestamp(timestamp)
                .expirationTimestamp(timestamp + 1)
                .buyThreshold(Utils.round(buyThreshold))
                .target(Utils.round(target))
                .stopLoss(Utils.round(stopLoss))
                .quantity(lotSize * 3)
                .lotSize(lotSize)
                .callOrder(callOrder)
                .build();
    }

    private boolean isHammerPattern(final Candle candle) {
        double lowerWickLength = CandleUtils.getLowerWick(candle);
        double totalLength = CandleUtils.getTotalLength(candle);
        double lowerWickRatio = lowerWickLength / totalLength;
        return lowerWickRatio > LOWER_WICK;
    }

    private boolean isInvertedHammerPattern(Candle candle) {
        double upperWickLength = CandleUtils.getUpperWick(candle);
        double totalLength = CandleUtils.getTotalLength(candle);
        double upperWickRatio = upperWickLength / totalLength;
        return upperWickRatio > LOWER_WICK;
    }
}
// CPD-ON