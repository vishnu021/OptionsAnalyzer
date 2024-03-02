package com.vish.fno.manage.config;

import com.vish.fno.util.helper.CandleStickCache;
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
    public CandleStickCache candleStickCache() {
        return new CandleStickCache();
    }
}
