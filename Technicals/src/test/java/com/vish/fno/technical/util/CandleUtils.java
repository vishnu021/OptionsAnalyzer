package com.vish.fno.technical.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.technical.model.Candle;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CandleUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Candle> getCandleData() {
        try {
            // Getting data for full day of RELIANCE for date 2023-02-17
            String candles = readFile(".//src//test//resources//sample_candles.json");
            return mapper.readValue(candles,  new TypeReference<List<Candle>>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(String filename) throws IOException {
        Path path = Paths.get(filename);
        log.info("path : {}", path.toAbsolutePath());
        Stream<String> lines = Files.lines(path);
        String content = lines.collect(Collectors.joining("\n"));
        lines.close();
        return content;
    }
}
