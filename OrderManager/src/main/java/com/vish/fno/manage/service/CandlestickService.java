package com.vish.fno.manage.service;

import com.vish.fno.manage.dao.CandlestickRepository;
import com.vish.fno.manage.helper.DataCache;
import com.vish.fno.manage.model.ApexChart;
import com.vish.fno.manage.model.ApexChartSeries;
import com.vish.fno.manage.util.FileUtils;
import com.vish.fno.model.Candle;
import com.vish.fno.model.SymbolData;
import com.vish.fno.reader.service.HistoricalDataService;
import com.vish.fno.technical.indicators.ma.ExponentialMovingAverage;
import com.zerodhatech.models.HistoricalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.vish.fno.manage.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandlestickService {
    private final CandlestickRepository candlestickRepository;
    private final DataCache cache;
    private final FileUtils fileUtils;

    public SymbolData getEntireDayHistoryData(String date, String symbol) {
        return getEntireDayHistoryData(date, symbol, "minute");
    }

    public SymbolData getEntireDayHistoryData(String date, String symbol, String interval) {
        return getCandlestickData(date, symbol, interval);
    }

    public ApexChart getEntireDayHistoryApexChart(String date, String symbol) {
        SymbolData symbolData = getEntireDayHistoryData(date, symbol, MINUTE);

        if(symbolData ==null || symbolData.getData()==null || symbolData.getData().size()==0)
            return null;

        return createChartData(symbolData);
    }

    private ApexChart createChartData(SymbolData symbolData) {
        List<ApexChartSeries> seriesCharts = new ArrayList<>();
        seriesCharts.add(new ApexChartSeries(STOCK_PRICE, CANDLESTICK, symbolData));

        try{
            List<Double> ema = new ExponentialMovingAverage().calculate(symbolData.getData());
            seriesCharts.add(new ApexChartSeries(EMA14, LINE, ema, symbolData.getData()));
        } catch (Exception e) {
            log.warn("Unable to calculate EMA {}", e.getMessage());
        }

        return new ApexChart(seriesCharts, getBarCharts(symbolData));
    }

    private List<ApexChartSeries> getBarCharts(SymbolData symbolData) {
        List<Long> volume = symbolData.getData().stream().map(Candle::getVolume).toList();
        return List.of(
                new ApexChartSeries(VOLUME, BAR, volume.stream().map(Long::doubleValue).toList(), symbolData.getData()));
    }

    private SymbolData getCandlestickData(String date, String symbol, String interval) {
        symbol = symbol.toUpperCase();
        String instrument = cache.getInstrumentForSymbol(symbol);

        if(instrument==null) {
            return null;
        }

        SymbolData candle = candlestickRepository.findByRecordDateAndRecordSymbol(date, symbol);

        if(candle==null || candle.getData().size()==0 || candle.getData().size() < 375) {
            log.debug("Getting candle data from broker as no data available for symbol: {}, date: {}", symbol, date);
            candle = getCandlesticksFromBroker(symbol, date, interval, instrument);
        }

        assert candle != null;
        fileUtils.saveJson(candle.getData(), String.format("data/%s_%s.json", symbol, date));
        candlestickRepository.save(candle);
        return candle;
    }


    private final HistoricalDataService historicalDataService;

    @Nullable
    public SymbolData getCandlesticksFromBroker(String symbol, String date, String interval, String instrument) {
        log.info("Returning data for symbol : {}, instrument : {}, date : {}, interval : {}",
                symbol, instrument, date, interval);
        HistoricalData data = historicalDataService.getEntireDayHistoricalData(date, instrument, interval);
        if (data == null) {
            log.warn("No data available for symbol : {} and date: {}", symbol, date);
            return null;
        }
        return verifyAndTransform(symbol, date, data);
    }


    @NotNull
    private SymbolData verifyAndTransform(String symbol, String date, HistoricalData data) {
        // This service has been created with few assumptions regarding the candlestick data coming from broker,
        // in case these conditions are not met, This service needs to be recreated.
        if( data.open !=0 || data.high !=0 || data.low !=0 || data.close !=0 || data.volume !=0 || data.oi !=0)
            log.error("open: {}, high: {}, low: {}, close: {}, volume: {}, oi: {}",
                    data.open,  data.high,  data.low,  data.close, data.volume, data.oi);

        try {
            return new SymbolData(data, symbol, date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
