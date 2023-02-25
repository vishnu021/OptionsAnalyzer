package com.vish.fno.reader.service;

import com.vish.fno.reader.util.TimeUtils;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;

@Slf4j
public class HistoricalDataService {
    private final KiteConnect kiteSdk;

    public HistoricalDataService(KiteService KiteService) {
        this.kiteSdk = KiteService.getKiteSdk();
    }

    public HistoricalData getLastDayHistoricalData(String date, String token) {
        Date lastTradingDay = TimeUtils.getLastTradingDay(TimeUtils.getDateObject(date));
        Date fromDate = TimeUtils.appendOpeningTimeToDate(lastTradingDay);
        Date toDate = TimeUtils.appendClosingTimeToDate(lastTradingDay);
        return getHistoricalData(fromDate, toDate, token, "60minute", false);
    }
    public HistoricalData getLastDayHistoricalData(String date, String token, String interval) {
        Date lastTradingDay = TimeUtils.getLastTradingDay(TimeUtils.getDateObject(date));
        Date fromDate = TimeUtils.appendOpeningTimeToDate(lastTradingDay);
        Date toDate = TimeUtils.appendClosingTimeToDate(lastTradingDay);
        return getHistoricalData(fromDate, toDate, token, interval, false);
    }

    public HistoricalData getEntireDayHistoricalData(String date, String token, String interval) {
        Date lastTradingDay = TimeUtils.getDateObject(date);
        Date fromDate = TimeUtils.appendOpeningTimeToDate(lastTradingDay);
        Date toDate = TimeUtils.appendClosingTimeToDate(lastTradingDay);
        return getHistoricalData(fromDate, toDate, token, interval, false);
    }

    public HistoricalData getHistoricalData(Date from, Date to, String token, String interval, boolean continuous) {
        try {
            log.debug("Collecting data for {} from: {} to: {}", token, from, to);
            return kiteSdk.getHistoricalData(from, to, token, interval, continuous, true);
        } catch (JSONException | IOException | KiteException e) {
            log.error("Error while requesting historical data (from: {}, to: {}, symbol: {})", from, to, token, e);
        }
        return null;
    }
}
