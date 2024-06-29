package com.vish.fno.manage.helper;

import com.vish.fno.manage.service.CalendarService;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.util.helper.DataCache;
import com.vish.fno.util.helper.HistoricDataCache;
import com.vish.fno.model.Candle;
import com.vish.fno.model.SymbolData;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.CandleStickCache;
import com.vish.fno.util.helper.TimeProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DataCacheImpl implements DataCache {
    private final String todaysDate;
    private final CandlestickService candlestickService;

    private final CandleStickCache minuteDataCache; //today's cache
    private final HistoricDataCache historicDataCache; // historical cache
    private final CalendarService calendarService;
    private final TimeProvider timeProvider;

    public DataCacheImpl(CandlestickService candlestickService, CalendarService calendarService, TimeProvider timeProvider) {
        this.candlestickService = candlestickService;
        this.calendarService = calendarService;
        this.timeProvider = timeProvider;
        this.minuteDataCache = new CandleStickCache();
        this.historicDataCache = new HistoricDataCache();
        this.todaysDate = timeProvider.getTodaysDateString();
    }

    public List<Candle> updateAndGetMinuteData(String symbol) {
        updateIntradayCache(symbol);
        return minuteDataCache.get(symbol);
    }

    public List<Candle> updateAndGetHistoryMinuteData(String date, String symbol) {
        updateHistoricCache(date, symbol);
        return historicDataCache.getData(date, symbol);
    }

    public List<Candle> getNCandles(final String symbol, final Date date, final int n) {
        List<Candle> todaysCandles = minuteDataCache.get(symbol);
        List<Candle> allCandles = new ArrayList<>();
        int remainingCandles = n;

        Date currentDay = date;
        while (remainingCandles > 0) {
            List<Candle> currentDayCandles = currentDay.equals(date) ? todaysCandles : updateAndGetHistoryMinuteData(todaysDate, symbol);
            if (currentDayCandles.size() > remainingCandles) {
                allCandles.addAll(0, currentDayCandles.subList(currentDayCandles.size() - remainingCandles, currentDayCandles.size()));
                break;
            } else {
                allCandles.addAll(0, currentDayCandles);
                remainingCandles -= currentDayCandles.size();
            }
            currentDay = calendarService.getPreviousNonHolidayDate(currentDay);
        }
        return allCandles.subList(0, Math.min(allCandles.size(), n));
    }

    @Override
    public List<Candle> getNCandles(String symbol, Date date, int n, List<Candle> todaysCandles) {
        return getNCandles(symbol, date, n);
    }

    private void updateIntradayCache(String symbol) {
        if (isDataAvailable(symbol)) {
            return;
        }

        log.info("updating intraday cache for: {} ", symbol);
        Optional<SymbolData> data = candlestickService.getEntireDayHistoryData(todaysDate, symbol);
        data.ifPresent(d -> {
            minuteDataCache.clear(symbol);
            minuteDataCache.update(symbol, d.getData());
        });
    }

    private boolean isDataAvailable(String symbol) {
        final Candle latestCandle = minuteDataCache.getLatestCandle(symbol);
        if(latestCandle == null) {
            return false;
        }
        int latestIndexOfTime = TimeUtils.getIndexOfTimeStamp(TimeUtils.getDateTimeForZonedDateString(latestCandle.getTime()));
        int currentIndexOfTime = timeProvider.currentTimeStampIndex();
        return latestIndexOfTime == currentIndexOfTime - 1;
    }

    private void updateHistoricCache(String date, String symbol) {
        if(historicDataCache.getData(date, symbol) == null || historicDataCache.getData(date, symbol).isEmpty()) {
            log.info("updating intraday cache for date: {}, symbol: {} ", date, symbol);
            Optional<SymbolData> candleStickData = candlestickService.getEntireDayHistoryData(date, symbol, "minute");
            candleStickData.ifPresent(d -> historicDataCache.update(date, symbol, d.getData()));
        }
    }
}
