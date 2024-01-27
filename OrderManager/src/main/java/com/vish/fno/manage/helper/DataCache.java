package com.vish.fno.manage.helper;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.util.FileUtils;
import com.vish.fno.manage.util.OptionPriceUtils;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.Instrument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.vish.fno.manage.util.Constants.*;


/*Only focusing on the 100 stocks of nifty 100 and indices*/
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidSynchronizedAtMethodLevel"})
public class DataCache {
    private final static String NSE = "NSE";
    private final static String NFO = "NFO";
    private static final String DELIMITER = ",";

    private final OrderConfiguration orderConfiguration;
    private final FileUtils fileUtils;
    private final KiteService kiteService;
    private List<Instrument> filteredInstruments;
    private Map<String, Long> symbolMap;
    private Map<Long, String> instrumentMap;

    public synchronized List<Instrument> getInstruments() {

        if (filteredInstruments != null) {
            return filteredInstruments;
        }

        List<Instrument> allInstruments = kiteService.getAllInstruments();
        fileUtils.saveInstrumentCache(allInstruments);

        List<String> nifty100Symbols = getNifty100Stocks();

        filteredInstruments = allInstruments.stream()
                .filter(i -> i.getExchange().contentEquals(NSE) || i.getExchange().contentEquals(NFO) && i.expiry != null)
                .filter(i -> nifty100Symbols.contains(i.getTradingsymbol())
                        || nifty100Symbols.contains(i.getName())).toList();

        symbolMap = filteredInstruments.stream()
                .collect(Collectors.toMap(Instrument::getTradingsymbol,
                        Instrument::getInstrument_token, (token, symbol) -> token, TreeMap::new));

        fileUtils.saveFilteredInstrumentCache(symbolMap);
        log.info("Filtered instrument count : {}", symbolMap.size());
        log.info("Filtered instrument expiry dates: {}", getExpiryDates());
        instrumentMap = symbolMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        return filteredInstruments;
    }

    public Set<String> getExpiryDates() {
        return getInstruments().stream()
                .map(Instrument::getExpiry)
                .filter(Objects::nonNull)
                .map(TimeUtils::getStringDate).collect(Collectors.toSet());
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
                                "expiry", TimeUtils.getStringDate(i.getExpiry()))));
        return allInstrumentData;
    }

    public Map<String, String> getFilteredSymbols() {
        return getInstruments().stream()
                .sorted(Comparator.comparing(Instrument::getName))
                .collect(Collectors.toMap(Instrument::getTradingsymbol, Instrument::getName, (k1, k2) ->  k1, LinkedHashMap::new ));
    }

    public String getITMStock(String index, double buyThreshold, boolean callOrder) {
        return OptionPriceUtils.getITMStock(index, buyThreshold, callOrder, getInstruments());
    }

    public String getInstrumentForSymbol(String symbol) {
        Long instrument = getInstrument(symbol);
        if(instrument == null) {
            log.warn("Invalid symbol : {}, not available in cache", symbol);
            return null;
        }
        return String.valueOf(instrument);
    }

    private List<String> getNifty100Stocks() {
        List<String> nifty100Symbols = new ArrayList<>();
        log.info("symbolsPath : {}", orderConfiguration.getSymbolsPath());
        try (BufferedReader br = new BufferedReader(new FileReader(orderConfiguration.getSymbolsPath()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                nifty100Symbols.add(values[2]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Symbols : {}", Arrays.asList(orderConfiguration.getAdditionalSymbols()));
        nifty100Symbols.addAll(Arrays.asList(orderConfiguration.getAdditionalSymbols()));
        return nifty100Symbols;
    }
}
