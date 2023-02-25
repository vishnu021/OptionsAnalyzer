package com.vish.fno.technical.indicators;

import com.vish.fno.technical.model.Candlestick;

import java.util.List;

public interface Indicator {
    List<Double> calculate(List<Candlestick> candles);
}
