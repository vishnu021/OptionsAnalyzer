package com.vish.fno.manage.config.kite;

import com.vish.fno.manage.helper.DataCache;
import com.vish.fno.reader.service.HistoricalDataService;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.manage.util.BrowserLauncher;
import com.vish.fno.manage.util.ConsoleAuthenticator;
import com.vish.fno.manage.util.FileUtils;
import com.vish.fno.reader.util.TimeUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.List;

@Getter
@Configuration
@EnableConfigurationProperties(KiteClientProperties.class)
public class KiteClientConfiguration {

    @Autowired
    private KiteClientProperties kiteClientProperties;

    @Bean
    @ConditionalOnProperty(prefix  = "kite", name = "serviceInCloud", havingValue = "false")
    public BrowserLauncher prepareBrowserLauncher() {
        return new BrowserLauncher(kiteClientProperties.isLiveData(),
                kiteClientProperties.getAuthenticationUrl());
    }

    @Bean
    @ConditionalOnProperty(prefix  = "kite", name = "serviceInCloud", havingValue = "true")
    public ConsoleAuthenticator prepareConsoleAuthenticator(KiteService kiteService) {
        return new ConsoleAuthenticator(kiteService, kiteClientProperties.getAuthenticationUrl());
    }

    @Bean
    public KiteService kiteService(@Value("${order.placeOrders}") boolean placeOrders,
                                   @Value("${order.connectToWebSocket}") boolean connectToWebSocket) {
        return new KiteService(kiteClientProperties.getApiSecret(), kiteClientProperties.getApiKey(),
                kiteClientProperties.getUserId(), placeOrders, connectToWebSocket);
    }

    @Bean
    public HistoricalDataService historyService(KiteService kiteService) {
        return new HistoricalDataService(kiteService);
    }

    @Bean
    public DataCache dataCache(FileUtils fileUtils, KiteService kiteService) {
        return new DataCache(fileUtils, kiteService);
    }

    public List<Date> getHolidays() {
        return kiteClientProperties.getHolidays2023().stream().map(TimeUtils::getDateObject).toList();
    }
}
