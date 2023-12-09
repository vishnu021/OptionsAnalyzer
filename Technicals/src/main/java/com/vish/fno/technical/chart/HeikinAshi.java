package com.vish.fno.technical.chart;

import com.vish.fno.model.Candle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.vish.fno.technical.util.TimeFrameUtils.mergeCandle;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class HeikinAshi {

    public static List<Candle> getCandles(List<Candle> allCandles) {
        return getCandles(allCandles, 1);
    }

    public static List<Candle> getCandles(List<Candle> allCandles, int timeFrame) {
        List<Candle> candles = mergeCandle(allCandles, timeFrame);

        List<Candle> heikinAshiCandles = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            Candle currentCandle = candles.get(i);

            double haOpen, haHigh, haLow, haClose;

            if (i == 0) {
                haOpen = currentCandle.getOpen();
                haClose = currentCandle.getClose();
            } else {
                Candle previousHA = heikinAshiCandles.get(i - 1);
                haOpen = (previousHA.getOpen() + previousHA.getClose()) / 2;
                haClose = (currentCandle.getOpen() + currentCandle.getHigh() + currentCandle.getLow() + currentCandle.getClose()) / 4;
            }

            haHigh = Math.max(Math.max(currentCandle.getHigh(), haOpen), haClose);
            haLow = Math.min(Math.min(currentCandle.getLow(), haOpen), haClose);

            heikinAshiCandles.add(new Candle(currentCandle.getTime(), haOpen, haHigh, haLow, haClose, currentCandle.getVolume(), currentCandle.getOi()));
        }

        return heikinAshiCandles;
    }
}
