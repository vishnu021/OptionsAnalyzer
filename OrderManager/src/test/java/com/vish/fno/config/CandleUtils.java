package com.vish.fno.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.model.Candle;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class CandleUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Tick> parseTickerFile(String filePath) throws IOException {
        List<Tick> tickers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Tick ticker = objectMapper.readValue(line, Tick.class);
                if(ticker != null && ticker.getTickTimestamp() != null) {
                    tickers.add(ticker);
                }
            }
        }

        // Sort the list of tickers by tickTimestamp in increasing order
        tickers.sort(Comparator.comparing(Tick::getTickTimestamp));
        return tickers;
    }

    public static List<Candle> parseCandleFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<Candle> candles = objectMapper.readValue(content, new TypeReference<List<Candle>>() {});

        // Sort the list of candles by time in increasing order
        candles.sort(Comparator.comparing(Candle::getTime));

        return candles;
    }

    public static List<Instrument> parseInstrumentFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<Instrument> candles = objectMapper.readValue(content, new TypeReference<List<Instrument>>() {});

        return candles;
    }
}
