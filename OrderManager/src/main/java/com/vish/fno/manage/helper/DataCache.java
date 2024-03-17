package com.vish.fno.manage.helper;

import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.util.helper.HistoricDataCache;
import com.vish.fno.model.Candle;
import com.vish.fno.model.SymbolData;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.CandleStickCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class DataCache {
    private final String todaysDate;
    private final CandlestickService candlestickService;

    private final CandleStickCache minuteDataCache;
    private final HistoricDataCache historicDataCache;

    public DataCache(CandlestickService candlestickService) {
        this.candlestickService = candlestickService;
        this.minuteDataCache = new CandleStickCache();
        this.historicDataCache = new HistoricDataCache();
        this.todaysDate = TimeUtils.getTodayDate();
    }

    public List<Candle> updateAndGetMinuteData(String symbol) {
        // TODO: only if latest data not present
        updateIntradayCache(symbol);
        return minuteDataCache.get(symbol);
    }

    private void updateIntradayCache(String symbol) {
        log.info("updating intraday cache for: {} ", symbol);
        Optional<SymbolData> candleStickData = candlestickService.getEntireDayHistoryData(todaysDate, symbol);
        minuteDataCache.update(symbol, candleStickData);
    }
}
