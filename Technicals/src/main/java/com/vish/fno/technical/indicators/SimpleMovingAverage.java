package com.vish.fno.technical.indicators;

import com.vish.fno.technical.model.Candlestick;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@NoArgsConstructor
public class SimpleMovingAverage implements Indicator {

    private int duration = 14;
    public SimpleMovingAverage(int duration) {
        this.duration = duration;
    }

    @Override
    public List<Double> calculate(List<Candlestick> candles) {
        List<Double> sma = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            sma.add(movingAverage(candles, i));
        }
        return sma;
    }

    private Double movingAverage(List<Candlestick> candles, int i) {
        int startIndex = i >= duration ? i - duration + 1 : 0;
        return IntStream.rangeClosed(startIndex, i)
                .mapToDouble(k -> candles.get(k).getClose())
                .average().orElse(0);
    }
}
