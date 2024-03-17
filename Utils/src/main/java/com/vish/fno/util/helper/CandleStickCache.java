package com.vish.fno.util.helper;

import com.vish.fno.model.Candle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for caching intraday candlestick data for symbols.
 */
public class CandleStickCache {

    private final Map<String, List<Candle>> candlesCache = new HashMap<>();

    public List<Candle> get(String symbol) {
        return candlesCache.get(symbol);
    }

    public void update(String symbol, List<Candle> data) {
        candlesCache.put(symbol, data);
    }

    public void clear(String symbol) {
        candlesCache.remove(symbol);
    }
}
