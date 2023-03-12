package com.vish.fno.technical.indicators;

import com.vish.fno.technical.model.Candle;

import java.util.List;

public class MovingAverageConvergenceDivergence implements Indicator {

    int fastLength = 12;
    int slowLength = 26;

    public MovingAverageConvergenceDivergence(int fastLength, int slowLength) {
        this.fastLength = fastLength;
        this.slowLength = slowLength;
    }

    @Override
    public List<Double> calculate(List<Candle> candles) {
        // TODO
        return null;
    }
}
