package com.vish.fno.technical.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.technical.model.Candlestick;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CandleUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Candlestick> getCandleData() {
        try {
            String candles = readFile(".//src//test//resources//sample_candles.json");
            return mapper.readValue(candles,  new TypeReference<List<Candlestick>>(){});
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
