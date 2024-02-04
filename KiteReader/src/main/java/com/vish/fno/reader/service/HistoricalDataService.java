package com.vish.fno.reader.service;

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

    public HistoricalData getEntireDayHistoricalData(Date fromDate, Date toDate, String token, String interval) {
        return getHistoricalData(fromDate, toDate, token, interval, false);
    }

    public HistoricalData getHistoricalData(Date from, Date to, String token, String interval, boolean continuous) {
        try {
            log.debug("Collecting data for {} from: {}, to: {}, interval: {}", token, from, to, interval);
            return kiteSdk.getHistoricalData(from, to, token, interval, continuous, true);
        } catch (JSONException | IOException | KiteException e) {
            log.error("Error while requesting historical data (from: {}, to: {}, symbol: {})", from, to, token, e);
        }
        return null;
    }
}
