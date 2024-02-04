package com.vish.fno.manage.service;

import com.vish.fno.manage.config.kite.KiteClientConfiguration;
import com.vish.fno.manage.dao.OptionsRepository;
import com.vish.fno.reader.helper.InstrumentCache;
import com.vish.fno.model.OptionMetaData;
import com.vish.fno.model.OptionSymbolData;
import com.vish.fno.model.SymbolData;
import com.vish.fno.util.TimeUtils;
import com.zerodhatech.models.Instrument;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class ArchiverService {

    private final InstrumentCache instrumentCache;
    private final KiteClientConfiguration kiteClientConfiguration;
    private final CandlestickService candlestickService;
    private final OptionsRepository optionsRepository;

    public void getAllNiftyOptions() {

        List<Instrument> filteredInstruments = instrumentCache.getInstruments();

        Map<String, Integer> optionIndicesMap = Map.of("NIFTY", Calendar.THURSDAY, "BANKNIFTY", Calendar.THURSDAY, "FINNIFTY", Calendar.TUESDAY);
        log.info("getHolidays : {}", kiteClientConfiguration.getHolidays());

        for(String index: optionIndicesMap.keySet()) {
            Date nextExpiryDate = getNextExpiryDate(filteredInstruments, index);
            log.info("nextExpiryDate : {}", nextExpiryDate);

            List<String> indexOptions = filteredInstruments.stream()
                    .filter(i -> Objects.equals(index, i.getName()))
                    .filter(i -> i.getExpiry().equals(nextExpiryDate))
                    .map(Instrument::getTradingsymbol)
                    .toList();

            log.info("filteredInstruments : {}", indexOptions);

            Calendar previousExpiryDate = getPreviousExpiry(index, nextExpiryDate, optionIndicesMap.get(index));
            log.info("getPreviousExpiry : {}",  getPreviousExpiry(index, nextExpiryDate, optionIndicesMap.get(index)).getTime());

            previousExpiryDate.add(Calendar.DAY_OF_YEAR, 1);
            Calendar currentDay = Calendar.getInstance(); // Today's date

            while (!previousExpiryDate.after(currentDay)) {
                Date currentDate = previousExpiryDate.getTime();
                log.info("Processing date: " + TimeUtils.getStringDate(currentDate));

                // Make API call here
                for(String indexOption: indexOptions) {
                    Optional<SymbolData> symbolDataOptional = candlestickService.getEntireDayHistoryData(TimeUtils.getStringDate(currentDate), indexOption);
                    symbolDataOptional.ifPresent(optionData -> {
                        OptionSymbolData optionSymbolData = OptionSymbolData.builder()
                                .record(new OptionMetaData(optionData.getRecord().getSymbol(), optionData.getRecord().getDate(), TimeUtils.getStringDate(nextExpiryDate)))
                                .data(optionData.getData())
                                .build();
                        optionsRepository.save(optionSymbolData);
                    });
                }

                // Move to the next day
                previousExpiryDate.add(Calendar.DAY_OF_MONTH, 1);
            }
            // getting data since last expiry to date
        }
    }

    private Date getNextExpiryDate(List<Instrument> filteredInstruments, String index) {
        Set<Date> allExpiry = filteredInstruments.stream().filter(i -> Objects.equals(index, i.getName())).map(Instrument::getExpiry).collect(Collectors.toSet());
        return Collections.min(allExpiry);
    }

    private boolean isLastThursdayOfMonth(Calendar calendar) {
        Calendar nextWeek = (Calendar) calendar.clone();
        nextWeek.add(Calendar.WEEK_OF_YEAR, 1);
        return nextWeek.get(Calendar.MONTH) != calendar.get(Calendar.MONTH);
    }


    private Calendar getPreviousExpiry(String index, Date date, int expiryDay) {
        if(index.equals("BANKNIFTY")){
            return getNiftyBankPreviousExpiryDay(date);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        do {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        } while (calendar.get(Calendar.DAY_OF_WEEK) != expiryDay);

        while (kiteClientConfiguration.getHolidays().contains(calendar.getTime())) {
            // If it's a holiday, move one more day back
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        return calendar;
    }

    private Calendar getNiftyBankPreviousExpiryDay(Date currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        // Define the date for the change in expiry rules
        Calendar ruleChangeDate = Calendar.getInstance();
        ruleChangeDate.set(2023, Calendar.SEPTEMBER, 5); // Note: Month is 0-based in Calendar

        do {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        } while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY);

        Calendar prevExpiryDay ;
        if(isLastThursdayOfMonth(calendar) || calendar.before(ruleChangeDate) ) {
            prevExpiryDay =  calendar;
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            prevExpiryDay = calendar;
        }

        while (kiteClientConfiguration.getHolidays().contains(calendar.getTime())) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        return prevExpiryDay;
    }
}
