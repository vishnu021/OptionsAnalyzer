package com.vish.fno.manage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.manage.dao.CandlestickRepository;
import com.vish.fno.manage.model.ApexChart;
import com.vish.fno.manage.model.ApexChartSeries;
import com.vish.fno.manage.strategy.CustomIndexService;
import com.vish.fno.model.CandleMetaData;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.Candle;
import com.vish.fno.model.SymbolData;
import com.vish.fno.technical.indicators.ma.ExponentialMovingAverage;
import com.vish.fno.technical.util.TimeFrameUtils;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.HistoricalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.vish.fno.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.AvoidThrowingRawExceptionTypes"})
public class CandlestickService {
    private final CandlestickRepository candlestickRepository;
    private final CustomIndexService customIndexService;
    private final FileUtils fileUtils;
    private final KiteService kiteService;
    private final CalendarService calendarService;

    public Optional<SymbolData> getEntireDayHistoryData(String date, String symbol) {
        return getEntireDayHistoryData(date, symbol, "minute");
    }

    public Optional<List<Candle>> getHistoryData(String from, String to, String symbol, String interval) {
        Date fromDate = TimeUtils.appendOpeningTimeToDate(TimeUtils.getDateObject(from));
        Date toDate = TimeUtils.appendClosingTimeToDate(TimeUtils.getDateObject(to));
        HistoricalData data =  kiteService.getHistoricalData(fromDate, toDate, symbol, interval, false);
        try {
            log.info("hist data : {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        List<Candle> value = data.dataArrayList.stream()
                .map(h -> new Candle(h.timeStamp, h.open, h.high, h.low, h.close, h.volume, h.oi))
                .toList();

        log.info("fromDate: {}, toDate: {}", fromDate, toDate);
        log.info("hist data value: {}", value);

        return Optional.of(value);
    }

    public Optional<SymbolData> getEntireDayHistoryData(String date, String symbol, String interval) {
        return getCandlestickData(date, symbol, interval);
    }

    public Optional<ApexChart> getEntireDayHistoryApexChart(String date, String symbol) {
        return getEntireDayHistoryData(date, symbol, MINUTE)
                .filter(symbolData -> symbolData.getData() != null && !symbolData.getData().isEmpty())
                .map(symbolData -> createChartData(date, symbol, symbolData));
    }

    public Optional<ApexChart> getEntireDayHistoryApexChart(String date, String symbol, String interval) {

        return getEntireDayHistoryData(date, symbol, MINUTE)
                .map(c -> {
                    List<Candle> data = TimeFrameUtils.mergeIntradayCompleteCandle(c.getData(), Integer.parseInt(interval));
                    return new SymbolData(c.getRecord(), data);
                })
                .filter(symbolData -> symbolData.getData() != null && !symbolData.getData().isEmpty())
                .map(symbolData -> createChartData(date, symbol, symbolData));
    }

    private ApexChart createChartData(String date, String symbol, SymbolData symbolData) {
        List<ApexChartSeries> seriesCharts = new ArrayList<>();
        seriesCharts.add(new ApexChartSeries(STOCK_PRICE, CANDLESTICK, symbolData));

        try{
            List<Double> ema9 = new ExponentialMovingAverage(9).calculate(symbolData.getData());
            List<Double> ema15 = new ExponentialMovingAverage(15).calculate(symbolData.getData());
            seriesCharts.add(new ApexChartSeries("EMA9", LINE, ema9, symbolData.getData()));
            seriesCharts.add(new ApexChartSeries("EMA15", LINE, ema15, symbolData.getData()));
        } catch (Exception e) {
            log.warn("Unable to calculate EMA {}", e.getMessage());
        }

        String instrument = String.valueOf(kiteService.getInstrument(symbol));
        return new ApexChart(date, symbol, instrument, symbolData.getData().size(), seriesCharts, getBarCharts(symbolData));
    }

    private List<ApexChartSeries> getBarCharts(SymbolData symbolData) {
        List<Long> volume = symbolData.getData().stream().map(Candle::getVolume).toList();
        return List.of(
                new ApexChartSeries(VOLUME, BAR, volume.stream().map(Long::doubleValue).toList(), symbolData.getData()));
    }

    private Optional<SymbolData> getCandlestickData(String date, String symbol, String interval) {
        return candlestickRepository.findByRecordDateAndRecordSymbol(date, symbol)
                .filter(candle -> candle.getData() != null && candle.getData().size() >= 375)
                .or(() -> getCandlesticksFromBroker(symbol, date, interval))
                .map(candle -> {
                    fileUtils.saveCandlestickData(candle.getData(), symbol, date);
                    candlestickRepository.save(candle);
                    return candle;
                });
    }

    @NotNull
    public Optional<SymbolData> getCandlesticksFromBroker(String symbol, String date, String interval) {
        if(calendarService.getHolidays().contains(date)) {
            log.debug("{} is a holiday date", date);
            return Optional.empty();
        }

        Date lastTradingDay = TimeUtils.getDateObject(date);
        Date fromDate = TimeUtils.appendOpeningTimeToDate(lastTradingDay);
        Date toDate = TimeUtils.appendClosingTimeToDate(lastTradingDay);

        HistoricalData data = kiteService.getEntireDayHistoricalData(fromDate, toDate , symbol, interval);
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
        if( data.open !=0 || data.high !=0 || data.low !=0 || data.close !=0 || data.volume !=0 || data.oi !=0) {
            log.error("open: {}, high: {}, low: {}, close: {}, volume: {}, oi: {}",
                    data.open,  data.high,  data.low,  data.close, data.volume, data.oi);
        }

        return Optional.of(createSymbolData(data, symbol, date));
    }

    private SymbolData createSymbolData(HistoricalData hd, String symbol, String date) {
        CandleMetaData record = new CandleMetaData(symbol, date);
        List<Candle> data = hd.dataArrayList.stream()
                .map(h -> new Candle(h.timeStamp, h.open, h.high, h.low, h.close, h.volume, h.oi))
                .toList();
        return new SymbolData(record, data);
    }
}
