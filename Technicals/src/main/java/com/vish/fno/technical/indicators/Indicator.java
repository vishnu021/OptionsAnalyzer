package com.vish.fno.technical.indicators;

import com.vish.fno.technical.model.Candle;

import java.util.List;

public interface Indicator {
    List<Double> calculate(List<Candle> candles);
}
