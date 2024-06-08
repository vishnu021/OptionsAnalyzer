package com.vish.fno.reader.service;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;

@Slf4j
@AllArgsConstructor
class HistoricalDataService {
    private final KiteService kiteService;
    private final InstrumentCache instrumentCache;

    HistoricalData getEntireDayHistoricalData(Date fromDate, Date toDate, String symbol, String interval) {
        return getHistoricalData(fromDate, toDate, symbol, interval, false);
    }

    HistoricalData getHistoricalData(Date from, Date to, String symbol, String interval, boolean continuous) {

        String instrument = getInstrumentToken(symbol);
        if (instrument == null) {
            return null;
        }

        if(!kiteService.isInitialised()) {
            log.warn("Kite service is not initialised yet");
            return null;
        }

        try {
            log.debug("Collecting data for {} from: {}, to: {}, interval: {}", instrument, from, to, interval);
            return kiteService.getKiteSdk().getHistoricalData(from, to, instrument, interval, continuous, true);
        } catch (JSONException | IOException | KiteException e) {
            log.error("Error while requesting historical data (from: {}, to: {}, symbol: {})", from, to, instrument, e);
        }
        return null;
    }

    private String getInstrumentToken(String symbol) {
        String instrument = String.valueOf(instrumentCache.getInstrument(symbol));

        if(instrument==null || instrument.equalsIgnoreCase("null")) {
            log.warn("No instrument available for symbol {}", symbol);
            return null;
        }
        return instrument;
    }
}
