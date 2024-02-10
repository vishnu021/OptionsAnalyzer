package com.vish.fno.manage.helper;

import com.vish.fno.model.Candle;
import com.vish.fno.model.SymbolData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CandleStickCache {

    private final Map<String, List<Candle>> candlesCache = new HashMap<>();

    public List<Candle> get(String symbol) {
        return candlesCache.get(symbol);
    }

    public void update(String symbol, Optional<SymbolData> candleStickData) {
        candlesCache.remove(symbol);
        candleStickData.ifPresent(d -> candlesCache.put(symbol, d.getData()));
    }
}
