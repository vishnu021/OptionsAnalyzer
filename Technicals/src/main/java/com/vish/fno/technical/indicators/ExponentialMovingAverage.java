package com.vish.fno.technical.indicators;

import com.vish.fno.technical.model.Candlestick;

import java.util.ArrayList;
import java.util.List;

public class ExponentialMovingAverage implements Indicator {
    private int duration = 14;
    public ExponentialMovingAverage(int duration) {
        this.duration = duration;
    }

    @Override
    public List<Double> calculate(List<Candlestick> candles) {
        List<Double> ema = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            ema.add(exponentialMovingAverage(candles, i, ema, duration));
        }
        return ema;
    }

    private Double exponentialMovingAverage(List<Candlestick> candles, int i, List<Double> emaList, int n) {
        double close = candles.get(i).getClose();
        int emaListSize = emaList.size();
        if (emaListSize == 0)
            return close;

        double previousEma = emaList.get(emaListSize - 1);

        if (emaListSize < n)
            return (previousEma * (emaListSize) + close) / (emaListSize + 1);

        return (close - previousEma) * 2 / (n + 1) + previousEma;
    }
}
