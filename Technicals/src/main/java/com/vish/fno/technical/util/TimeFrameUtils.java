package com.vish.fno.technical.util;

import com.vish.fno.model.Candle;

import java.util.ArrayList;
import java.util.List;

public class TimeFrameUtils {
    public static List<Candle> mergeCandle(List<Candle> allCandles, int n) {
        List<Candle> candles = new ArrayList<>();

        for (int i = 0; i < allCandles.size(); i += n) {
            if (allCandles.size() < i + n)
                break;
            Candle mergedCandle = combine(allCandles.subList(i, i + n));
            candles.add(mergedCandle);
        }
        return candles;
    }

    private static Candle combine(List<Candle> candleList) {
        if (candleList == null || candleList.size() == 0)
            return null;

        double open = candleList.get(0).getOpen();
        double close = candleList.get(candleList.size() - 1).getClose();
        double high = candleList.stream().mapToDouble(Candle::getHigh).max().orElse(0d);
        double low = candleList.stream().mapToDouble(Candle::getLow).min().orElse(0d);
        long volume = (long) candleList.stream().mapToDouble(Candle::getVolume).sum();
        long oi = (long) candleList.stream().mapToDouble(Candle::getOi).sum();
        String time = candleList.get(0).getTime();

        return new Candle(time, open, high, low, close, volume, oi);
    }
}
