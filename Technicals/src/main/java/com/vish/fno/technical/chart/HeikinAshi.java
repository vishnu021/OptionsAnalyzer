package com.vish.fno.technical.chart;

import com.vish.fno.model.Candle;

import java.util.ArrayList;
import java.util.List;

import static com.vish.fno.technical.util.TimeFrameUtils.mergeCandle;

public class HeikinAshi {

    public List<Candle> getCandles(List<Candle> allCandles, int timeFrame) {
        List<Candle> haCandles = new ArrayList<>();

        List<Candle> candles = mergeCandle(allCandles, timeFrame);

        if (allCandles.size() < 1)
            return haCandles;

        Candle lastCandle = candles.get(0);
        haCandles.add(lastCandle);
        for (int i = 1; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            double close = 0.25 * (candle.getOpen() + candle.getHigh() + candle.getLow() + candle.getClose());
            double open = 0.5 * (lastCandle.getOpen() + lastCandle.getClose());
            double high = Math.max(Math.max(candle.getHigh(), open), close);
            double low = Math.min(Math.min(candle.getLow(), open), close);
            Candle haCandle = new Candle("", open, close, high, low, candle.getVolume(), candle.getOi());
            haCandles.add(haCandle);
            lastCandle = haCandle;
        }

        return haCandles;
    }
}
