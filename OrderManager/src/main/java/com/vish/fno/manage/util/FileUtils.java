package com.vish.fno.manage.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vish.fno.model.Candle;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.util.Constants;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
@SuppressWarnings({"PMD.UnusedPrivateMethod", "PMD.AvoidCatchingGenericException"})
public class FileUtils implements Constants {

    private static final String CANDLESTICK_PATH = "data";

    protected ObjectMapper indentedMapper = new ObjectMapper();
    protected ObjectMapper mapper = new ObjectMapper();
    String filePath = Paths.get(".").normalize().toAbsolutePath() + "\\" + directory + "\\";
    String tickPath = Paths.get(".").normalize().toAbsolutePath() + "\\" + tick_directory + "\\";
    int bufferLength;

    @PostConstruct
    public void initialise() {
        indentedMapper.enable(SerializationFeature.INDENT_OUTPUT);
        bufferLength = 0;
        createDirectoryIfNotExist(filePath);
    }

    public void saveInstrumentCache(List<Instrument> instruments) {
        try {
            indentedMapper.writeValue(new File(filePath + getInstrumentFileName(0)), instruments);
        } catch (IOException e) {
            log.error("",e);
        }
    }
    public void saveFilteredInstrumentCache(Object instruments) {
        try {
            indentedMapper.writeValue(new File(filePath + "filtered_" + getInstrumentFileName(0)), instruments);
        } catch (IOException e) {
            log.error("Failed to save instrument cache.", e);
        }
    }

    public void saveCandlestickData(List<Candle> candles, String symbol, String date) {
        String path = String.format("%s/%s_%s.json", CANDLESTICK_PATH, symbol, date);
        try {
            createDirectoryIfNotExist(CANDLESTICK_PATH);
            indentedMapper.writeValue(new File(path), candles);
        } catch (IOException e) {
            log.error("Failed to save candlestick data", e);
        }
    }

    public List<Instrument> loadInstrumentCache(int days) {
        List<Instrument> instruments = null;
        String instrumentFileName = getInstrumentFileName(days);
        try {
            instruments = indentedMapper.readValue(new File(getInstrumentFileName(days)),
                    indentedMapper.getTypeFactory().constructCollectionType(List.class, Instrument.class));
            log.info("Loaded instrument cache from file " + instrumentFileName);
        } catch (IOException e) {
            log.error("Instrument file not yet created");
        }
        return instruments;
    }

    public void createDirectoryIfNotExist(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            log.error("Failed to create directory to path : {}", path, e);
        }
    }

    public void saveTickData(String symbol, Tick tick) {
        String path = filePath + getFormattedDate(new Date()) + symbol + ".txt";
        createDirectoryIfNotExist(path);
        try {
            indentedMapper.writeValue(new File(path), tick);
        } catch (IOException e) {
            log.warn("Failed to persist tick value", e);
        }
    }

    public void appendTickToFile(String symbol, Tick tick) {
        String folderPath = tickPath + getFormattedDate(new Date());
        createDirectoryIfNotExist(folderPath);
        String filePath = folderPath + "//" + symbol + ".txt";
        filePath = filePath.replaceAll("\\s", "_");

        try {
            String jsonString = mapper.writeValueAsString(tick);
            appendToFile(jsonString, filePath);
        } catch (IOException e) {
            log.warn("Failed to append tick to file", e);
        }
    }

    private void appendToFile(String content, String filePath) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
             PrintWriter out = new PrintWriter(bufferedWriter)) {
            out.println(content);
        }
    }

    private String candleFileName(String instrument, Date fromDate) {
        createDirectoryIfNotExist(filePath + getFormattedDate(fromDate));
        return filePath + getFormattedDate(fromDate) + "\\" + instrument + ".json";
    }

    private String getFormattedDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date);
    }
    private String getInstrumentFileName(int days) {
        return "instruments_" + getFormattedDate(TimeUtils.getNDaysBefore(days)) + ".json";
    }

    public void logCompletedOrder(ActiveOrder order) {
        try {
            String folder = "orderLog";
            createDirectoryIfNotExist(folder);
            ObjectMapper mapper = getMapper();
            String orderJson = mapper.writeValueAsString(order);
            String fileName = folder + "/" + order.getOptionSymbol() + TimeUtils.getTodayDate() + System.currentTimeMillis() + ".json";

            try (FileWriter fileWriter = new FileWriter(fileName, true);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                 PrintWriter out = new PrintWriter(bufferedWriter)) {
                out.println(orderJson);
            }
        } catch (Exception e) {
            try {
                log.error("Failed to persist order log : {}", indentedMapper.writeValueAsString(order), e);
            } catch (JsonProcessingException ex) {
                log.error("Failed to convert order log to json.", e);
            }
        }
    }

    @NotNull
    private ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
//        SimpleModule module = new SimpleModule();
//        module.addSerializer(Double.class, new CustomDoubleSerializer());
//        module.addSerializer(Float.class, new CustomFloatSerializer());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
        mapper.setDateFormat(dateFormat);

//        mapper.registerModule(module);
        return mapper;
    }

//    static class CustomDoubleSerializer extends JsonSerializer<Double> {
//        @Override
//        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//            if (value != null) {
//                gen.writeNumber(BigDecimal.valueOf(value).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//            }
//        }
//    }
//
//    static class CustomFloatSerializer extends JsonSerializer<Float> {
//
//        @Override
//        public void serialize(Float aFloat, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
//            if (aFloat != null) {
//                jsonGenerator.writeNumber(BigDecimal.valueOf(aFloat).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
//            }
//        }
//    }
}
