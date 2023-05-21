package com.vish.fno.manage.helper;

import com.vish.fno.manage.util.FileUtils;
import com.vish.fno.reader.service.KiteService;
import com.zerodhatech.models.Instrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DataCache {
    @Value("${order.additionalSymbols}")
    private String[] additionalSymbols;
    @Value("${order.symbolsPath}")
    private String symbolsPath;

    private final static String NSE = "NSE";
    private final static String NFO = "NFO";
    private static final String DELIMITER = ",";
    private final FileUtils fileUtils;
    private final KiteService kiteService;

    private List<Instrument> instruments;
    private Map<String, Long> symbolMap;
    private Map<Long, String> instrumentMap;

    public  List<Instrument> getInstruments() {

        if (instruments != null)
            return instruments;

        instruments = kiteService.getAllInstruments();
        fileUtils.saveInstrumentCache(instruments);

        List<Instrument> allInstruments = getInstruments();

        List<String> nifty100Symbols = getNifty100Stocks();

        symbolMap = allInstruments.stream()
                .filter(i -> i.getExchange().contentEquals(NSE) || i.getExchange().contentEquals(NFO) && i.expiry != null)
                .filter(i -> nifty100Symbols.contains(i.getTradingsymbol())
                        || nifty100Symbols.contains(i.getName()))
                .collect(Collectors.toMap(Instrument::getTradingsymbol,
                        Instrument::getInstrument_token, (token, symbol) -> token, TreeMap::new));

        fileUtils.saveFilteredInstrumentCache(symbolMap);
        log.info("Filtered instrument count : {}", symbolMap.size());
        instrumentMap = symbolMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        return instruments;
    }

    public Long getInstrument(String script) {
        getInstruments();
        return this.symbolMap.get(script);
    }

    public String getSymbol(String instrument) {
        getInstruments();
        return this.instrumentMap.get(Long.parseLong(instrument));
    }

    public String getSymbol(long instrument) {
        getInstruments();
        return this.instrumentMap.get(instrument);
    }

    public Set<String> getAllSymbols() {
        return getInstruments().stream()
                .map(Instrument::getName)
                .collect(Collectors.toSet());
    }

    public Map<String, String> getFilteredSymbols(String filter) {
        return getInstruments().stream()
                .filter(i -> i.getExchange().contains(filter.toUpperCase()))
                .filter(i -> i.getName() !=null)
                .sorted(Comparator.comparing(Instrument::getName))
                .collect(Collectors.toMap(Instrument::getTradingsymbol, Instrument::getName, (k1, k2) ->  k1, LinkedHashMap::new ));
    }

    public Map<String, Long> getFilteredTokens(String filter) {
        return getInstruments().stream()
                .filter(i -> i.getExchange().contains(filter.toUpperCase()))
                .filter(i -> i.getName() !=null)
                .sorted(Comparator.comparing(Instrument::getName))
                .collect(Collectors.toMap(Instrument::getName, Instrument::getInstrument_token, (k1,k2) ->  k1, LinkedHashMap::new ));
    }

    public String getInstrumentForSymbol(String symbol) {
        Long instrument = getInstrument(symbol);
        if(instrument==null) {
            log.warn("Invalid symbol : {}, not available in cache", symbol);
            return null;
        }
        return String.valueOf(instrument);
    }

    private List<String> getNifty100Stocks() {
        List<String> nifty100Symbols = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(symbolsPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                nifty100Symbols.add(values[2]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Symbols : {}", Arrays.asList(additionalSymbols));
        nifty100Symbols.addAll(Arrays.asList(additionalSymbols));
        return nifty100Symbols;
    }
}
