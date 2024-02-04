package com.vish.fno.reader.service;

import com.zerodhatech.models.Instrument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OptionPriceUtils {
    private final static String NIFTY_BANK = "NIFTY BANK";
    private final static String NIFTY_50 = "NIFTY 50";
    private final static String NIFTY_FIN_SERVICE = "NIFTY FIN SERVICE";

    public static String getITMStock(String indexSymbol, double price, boolean isCall, List<Instrument> instruments) {

        String symbolsName = getOptionPrefix(indexSymbol);

        String instrumentType = isCall ? "CE" : "PE";

        Map<Date, List<Instrument>> indexSymbolsInstruments = instruments.stream()
                .filter(i -> i.getName().toUpperCase(Locale.ENGLISH).equalsIgnoreCase(symbolsName))
                .filter(instrument -> instrument.exchange.equals("NFO"))
                .filter(i -> i.getInstrument_type().equals(instrumentType))
                .collect(Collectors.groupingBy(Instrument::getExpiry));

        Optional<List<Instrument>> earliestExpiryInstrument = indexSymbolsInstruments.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .findFirst();

        AtomicReference<String> itmSymbol = new AtomicReference<>("");
        earliestExpiryInstrument.ifPresent(expiryInstruments -> {

            Map<Long, String> strikeToSymbolMap = expiryInstruments.stream()
                    .collect(Collectors.toMap(
                            instrument -> {
                                try {
                                    return Long.parseLong(instrument.getStrike());
                                } catch (NumberFormatException e) {
                                    log.error("NumberFormatException while parsing the strike price");
                                    return null;
                                }
                            },
                            Instrument::getTradingsymbol,
                            (existing, replacement) -> existing,
                            TreeMap::new
                    ));

            if(isCall) {
                for(long strikePrice: strikeToSymbolMap.keySet()) {
                    if(strikePrice > price) {
                        break;
                    }
                    itmSymbol.set(strikeToSymbolMap.get(strikePrice));
                }
            } else {
                for(long strikePrice: strikeToSymbolMap.keySet()) {
                    if(strikePrice > price) {
                        itmSymbol.set(strikeToSymbolMap.get(strikePrice));
                        break;
                    }
                }
            }
        });

        String itmSymbolValue = itmSymbol.get();

        if (itmSymbolValue == null || itmSymbolValue.strip().length() == 0) {
            log.error("Unable to find itmSymbol for index: {}, price: {}, call: {}", indexSymbol, price, isCall);
        }
        return itmSymbolValue;
    }

    private static String getOptionPrefix(String indexSymbol) {
        return switch (indexSymbol) {
            case NIFTY_BANK -> "BANKNIFTY";
            case NIFTY_50 -> "NIFTY";
            case NIFTY_FIN_SERVICE -> "FINNIFTY";
            default -> indexSymbol;
        };
    }
}
