package com.vish.fno.util.helper;

import com.vish.fno.model.Candle;
import com.vish.fno.util.helper.CandleStickCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HistoricDataCache {
    // date // symbol //  data
    private final Map<String, Map<String, List<Candle>>> dataCache = new HashMap<>();

    public List<Candle> getData(String date, String symbol) {
        return dataCache.get(date).get(symbol);
    }
}
