package com.vish.fno.reader.helper;

import com.vish.fno.reader.service.KiteService;
import com.vish.fno.reader.util.InstrumentFileUtils;
import com.vish.fno.reader.util.KiteUtils;
import com.zerodhatech.models.Instrument;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;


/*Only focusing on the 100 stocks of nifty 100 and indices*/
@Slf4j
@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidSynchronizedAtMethodLevel"})
public class InstrumentCache {
    private final static String NSE = "NSE";
    private final static String NFO = "NFO";

    private final KiteService kiteService;
    private final List<String> nifty100Symbols;
    private List<Instrument> filteredInstruments;
    private Map<String, Long> symbolMap;
    private Map<Long, String> instrumentMap;

    public InstrumentCache(List<String> nifty100Symbols, KiteService kiteService) {
        this.nifty100Symbols = nifty100Symbols;
        this.kiteService = kiteService;
    }

    public synchronized List<Instrument> getInstruments() {

        if (filteredInstruments != null) {
            return filteredInstruments;
        }

        List<Instrument> allInstruments = kiteService.getAllInstruments();
        InstrumentFileUtils.saveInstrumentCache(allInstruments);

        filteredInstruments = allInstruments.stream()
                .filter(i -> i.getExchange().contentEquals(NSE) || i.getExchange().contentEquals(NFO) && i.expiry != null)
                .filter(i -> nifty100Symbols.contains(i.getTradingsymbol())
                        || nifty100Symbols.contains(i.getName())).toList();

        symbolMap = filteredInstruments.stream()
                .collect(Collectors.toMap(Instrument::getTradingsymbol,
                        Instrument::getInstrument_token, (token, symbol) -> token, TreeMap::new));

        InstrumentFileUtils.saveFilteredInstrumentCache(symbolMap);
        log.info("Filtered instrument count : {}", symbolMap.size());
        log.info("Filtered instrument expiry dates: {}", getExpiryDates());
        instrumentMap = symbolMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        return filteredInstruments;
    }

    public Set<String> getExpiryDates() {
        return getInstruments().stream()
                .map(Instrument::getExpiry)
                .filter(Objects::nonNull)
                .map(KiteUtils::getStringDate).collect(Collectors.toSet());
    }

    public Long getInstrument(String script) {
        getInstruments();
        if(script == null) {
            return null;
        }

        return this.symbolMap.get(script.toUpperCase(Locale.ENGLISH));
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

    public List<Map<String, String>> getAllInstruments() {
        List<Map<String, String>> allInstrumentData = new ArrayList<>();
        getInstruments().stream()
                .sorted(Comparator.comparing(Instrument::getName))
                .forEach( i -> allInstrumentData.add(
                        Map.of(
                                "exchange", i.getExchange(),
                                "symbol", i.getTradingsymbol(),
                                "expiry", KiteUtils.getStringDate(i.getExpiry()))));
        return allInstrumentData;
    }

    public Map<String, String> getFilteredSymbols() {
        return getInstruments().stream()
                .sorted(Comparator.comparing(Instrument::getName))
                .collect(Collectors.toMap(Instrument::getTradingsymbol, Instrument::getName, (k1, k2) ->  k1, LinkedHashMap::new ));
    }

    public String getInstrumentForSymbol(String symbol) {
        Long instrument = getInstrument(symbol);
        if(instrument == null) {
            log.warn("Invalid symbol : {}, not available in cache", symbol);
            return null;
        }
        return String.valueOf(instrument);
    }
}
