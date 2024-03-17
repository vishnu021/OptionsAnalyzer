package com.vish.fno.manage.config;

import com.vish.fno.manage.helper.DataCache;
import com.vish.fno.manage.service.CalendarService;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.util.FileUtils;
import com.vish.fno.util.helper.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class UtilsConfig {

    @Bean
    public FileUtils fileUtils() {
        return new FileUtils();
    }

    @Bean
    public TimeProvider timeProvider() {
        return new TimeProvider();
    }

    @Bean
    public DataCache candleStickCache(CandlestickService candlestickService, CalendarService calendarService) {
        return new DataCache(candlestickService, calendarService);
    }
}
