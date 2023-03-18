package com.vish.fno.technical.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.technical.model.Candle;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CandleUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String resourcePath = ".//src//test//resources//RELIANCE_2023_03_17//";
    private static final String currentDayFile = "RELIANCE_2023-03-17.json";
    private static final String prevDayFile = "RELIANCE_2023-03-16.json";
    public static final ArrayList<String> timeArray = new ArrayList<>();

    static {
        int hour = 9;
        int minute = 15;
        for (int i = 0; i <= 375; i++) {
            timeArray.add(toTimeValue(hour) + ":" + toTimeValue(minute));
            minute++;
            if (minute == 60) {
                hour++;
                minute = 0;
            }
        }
    }

    private static String toTimeValue(int timeVal) {
        if (timeVal >= 0 && timeVal <= 9)
            return "0" + timeVal;
        return String.valueOf(timeVal);
    }

    public static List<Candle> getCandleData() {
        try {
            String candles = readFile(currentDayFile);
            return mapper.readValue(candles,  new TypeReference<List<Candle>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Candle> getPrevDayCandleData() {
        try {
            String candles = readFile(prevDayFile);
            return mapper.readValue(candles,  new TypeReference<List<Candle>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Double> getSmaData(String fileName) {
        return getIndicatorData(fileName);
    }

    public static List<Double> getEmaData(String fileName) {
        return getIndicatorData(fileName);
    }

    private static List<Double> getIndicatorData(String filePath) {
        try {
            String smaValues = readFile(filePath);
            return Arrays.stream(smaValues.split("\n"))
                    .filter(s -> s.trim().length() > 0)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(String filename) throws IOException {
        Path path = Paths.get(resourcePath + filename);
        log.info("path : {}", path.toAbsolutePath());
        Stream<String> lines = Files.lines(path);
        String content = lines.collect(Collectors.joining("\n"));
        lines.close();
        return content;
    }
}
