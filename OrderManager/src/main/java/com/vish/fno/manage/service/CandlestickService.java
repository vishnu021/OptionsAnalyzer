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
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.vish.fno.manage.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandlestickService {
    private final CandlestickRepository candlestickRepository;
    private final DataCache cache;
    private final FileUtils fileUtils;
    private final HistoricalDataService historicalDataService;

    public Optional<SymbolData> getEntireDayHistoryData(String date, String symbol) {
        return getEntireDayHistoryData(date, symbol, "minute");
    }

    public Optional<SymbolData> getEntireDayHistoryData(String date, String symbol, String interval) {
        return getCandlestickData(date, symbol, interval);
    }

    public Optional<ApexChart> getEntireDayHistoryApexChart(String date, String symbol) {
        return getEntireDayHistoryData(date, symbol, MINUTE)
                .filter(symbolData -> symbolData.getData() != null && !symbolData.getData().isEmpty())
                .map(symbolData -> createChartData(date, symbol, symbolData));
    }

    private ApexChart createChartData(String date, String symbol, SymbolData symbolData) {
        List<ApexChartSeries> seriesCharts = new ArrayList<>();
        seriesCharts.add(new ApexChartSeries(STOCK_PRICE, CANDLESTICK, symbolData));

        try{
            List<Double> ema = new ExponentialMovingAverage().calculate(symbolData.getData());
            seriesCharts.add(new ApexChartSeries(EMA14, LINE, ema, symbolData.getData()));
        } catch (Exception e) {
            log.warn("Unable to calculate EMA {}", e.getMessage());
        }

        String instrument = cache.getInstrumentForSymbol(symbol);
        return new ApexChart(date, symbol, instrument, symbolData.getData().size(), seriesCharts, getBarCharts(symbolData));
    }

    private List<ApexChartSeries> getBarCharts(SymbolData symbolData) {
        List<Long> volume = symbolData.getData().stream().map(Candle::getVolume).toList();
        return List.of(
                new ApexChartSeries(VOLUME, BAR, volume.stream().map(Long::doubleValue).toList(), symbolData.getData()));
    }

    private Optional<SymbolData> getCandlestickData(String date, String symbol, String interval) {
        String instrument = cache.getInstrumentForSymbol(symbol);

        if(instrument==null) {
            log.warn("No instrument available for symbol {}", symbol);
            return Optional.empty();
        }

        return candlestickRepository.findByRecordDateAndRecordSymbol(date, symbol)
                .filter(candle -> candle.getData() != null && candle.getData().size() >= 375)
                .or(() -> {
                    log.debug("Getting candle data from broker as no data available for symbol: {}, date: {}", symbol, date);
                    return getCandlesticksFromBroker(symbol, date, interval, instrument);
                })
                .map(candle -> {
                    fileUtils.saveJson(candle.getData(), String.format("data/%s_%s.json", symbol, date));
                    candlestickRepository.save(candle);
                    return candle;
                });
    }

    @NotNull
    public Optional<SymbolData> getCandlesticksFromBroker(String symbol, String date, String interval, String instrument) {
        log.info("Returning data for symbol : {}, instrument : {}, date : {}, interval : {}",
                symbol, instrument, date, interval);
        HistoricalData data = historicalDataService.getEntireDayHistoricalData(date, instrument, interval);
        if (data == null) {
            log.warn("No data available for symbol : {} and date: {}", symbol, date);
            return Optional.empty();
        }
        return verifyAndTransform(symbol, date, data);
    }

    @NotNull
    private Optional<SymbolData> verifyAndTransform(String symbol, String date, HistoricalData data) {
        // This service has been created with few assumptions regarding the candlestick data coming from broker,
        // in case these conditions are not met, This service needs to be recreated.
        if( data.open !=0 || data.high !=0 || data.low !=0 || data.close !=0 || data.volume !=0 || data.oi !=0)
            log.error("open: {}, high: {}, low: {}, close: {}, volume: {}, oi: {}",
                    data.open,  data.high,  data.low,  data.close, data.volume, data.oi);

        try {
            return Optional.of(new SymbolData(data, symbol, date));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
