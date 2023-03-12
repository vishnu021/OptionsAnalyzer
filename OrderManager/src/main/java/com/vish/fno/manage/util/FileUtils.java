package com.vish.fno.manage.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vish.fno.reader.util.TimeUtils;
import com.zerodhatech.models.Instrument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


@Slf4j
@Component
@SuppressWarnings("PMD.UnusedPrivateMethod")
public class FileUtils implements Constants {
    protected ObjectMapper mapper;
    int bufferLength;

    @PostConstruct
    public void initialise() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        bufferLength = 0;
        createDirectoryIfNotExist(filePath);
    }

    public void saveInstrumentCache(List<Instrument> instruments) {
        try {
            mapper.writeValue(new File(filePath + getInstrumentFileName(0)), instruments);
        } catch (IOException e) {
            log.error("",e);
        }
    }
    public void saveFilteredInstrumentCache(Object instruments) {
        try {
            mapper.writeValue(new File(filePath + "filtered_" + getInstrumentFileName(0)), instruments);
        } catch (IOException e) {
            log.error("",e);
        }
    }
    public void saveJson(Object instruments, String path) {
        try {
            mapper.writeValue(new File(path), instruments);
        } catch (IOException e) {
            log.error("",e);
        }
    }

    public List<Instrument> loadInstrumentCache(int days) {
        List<Instrument> instruments = null;
        String instrumentFileName = getInstrumentFileName(days);
        try {
            instruments = mapper.readValue(new File(getInstrumentFileName(days)),
                    mapper.getTypeFactory().constructCollectionType(List.class, Instrument.class));
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
            log.error("",e);
        }
    }

    private String candleFileName(String instrument, Date fromDate) {
        createDirectoryIfNotExist(filePath + dateFormatter.format(fromDate));
        return filePath + dateFormatter.format(fromDate) + "\\" + instrument + ".json";
    }

    private String getInstrumentFileName(int days) {
        return "instruments_" + dateFormatter.format(TimeUtils.getNDaysBefore(days)) + ".json";
    }
}
