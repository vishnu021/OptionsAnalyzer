package com.vish.fno.manage.service;

import com.vish.fno.manage.dao.CandlestickRepository;
import com.vish.fno.manage.helper.DataCache;
import com.vish.fno.manage.util.FileUtils;
import com.vish.fno.reader.service.HistoricalDataService;
import com.vish.fno.technical.model.Candlestick;
import com.zerodhatech.models.HistoricalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandlestickService {

    private final HistoricalDataService historicalDataService;
    private final CandlestickRepository candlestickRepository;
    private final DataCache cache;
    private final FileUtils fileUtils;

    public String getInstrumentForSymbol(String symbol) {
        Long instrument = cache.getInstrument(symbol);
        if(instrument==null) {
            log.warn("Invalid symbol : {}", symbol);
            return null;
        }
        return String.valueOf(instrument);
    }

    public List<Candlestick> getEntireDayHistoryData(String date, String symbol) {
        return getEntireDayHistoryData(date, symbol, "minute");
    }

    public List<Candlestick> getEntireDayHistoryData(String date, String symbolVal,  String interval) {
        final String symbol = symbolVal.toUpperCase();

        List<Candlestick> candles = getCandlestickData(date, symbol, interval);
        if (candles == null) return null;
        candlestickRepository.saveAll(candles);
        return candles;
    }

    private List<Candlestick> getCandlestickData(String date, String symbol,  String interval) {
        String instrument = getInstrumentForSymbol(symbol);
        if(instrument==null) {
            log.warn("Symbol {} not available in instrument cache", symbol);
            return null;
        }

        List<Candlestick> candles = candlestickRepository.findByRecordDateAndRecordSymbol(date, symbol);

        if(candles==null || candles.size()==0) {
            log.debug("Getting candle data from broker as no data available for symbol: {}, date: {}", symbol, date);
            candles = getCandlesticksFromBroker(symbol, date, interval, instrument);
        }
        fileUtils.saveJson(candles, "mongo.json");
        return candles;
    }

    @Nullable
    private List<Candlestick> getCandlesticksFromBroker(String symbol, String date, String interval, String instrument) {
        log.info("Returning data for symbol : {}, instrument : {}, date : {}, interval : {}",
                symbol, instrument, date, interval);
        HistoricalData data = historicalDataService.getEntireDayHistoricalData(date, instrument, interval);
        if (data == null) {
            log.warn("No data available for symbol : {} and date: {}", symbol, date);
            return null;
        }
        // This service has been created with few assumptions regarding the candlestick data coming from broker,
        // in case these conditions are not met, This service needs to be recreated.
        if( data.open !=0 || data.high !=0 || data.low !=0 || data.close !=0 || data.volume !=0 || data.oi !=0)
            log.error("open: {}, high: {}, low: {}, close: {}, volume: {}, oi: {}",
                    data.open,  data.high,  data.low,  data.close, data.volume, data.oi);

        return data.dataArrayList.stream().map(h -> {
            try {
                return new Candlestick(h, symbol);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
