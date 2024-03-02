package com.vish.fno.manage.config.kite;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.reader.exception.InitialisationException;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.manage.util.BrowserLauncher;
import com.vish.fno.manage.util.ConsoleAuthenticator;
import com.vish.fno.util.TimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Getter
@Configuration
@EnableConfigurationProperties(KiteClientProperties.class)
public class KiteClientConfiguration {

    private static final String DELIMITER = ",";

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
    public ConsoleAuthenticator prepareConsoleAuthenticator( KiteService kiteService) {
        return new ConsoleAuthenticator(kiteService, kiteClientProperties.getAuthenticationUrl());
    }

    @Bean
    public KiteService kiteService(@Value("${order.placeOrders}") boolean placeOrders,
                                   @Value("${order.connectToWebSocket}") boolean connectToWebSocket,
                                   OrderConfiguration orderConfiguration) {
        return new KiteService(kiteClientProperties.getApiSecret(),
                kiteClientProperties.getApiKey(),
                kiteClientProperties.getUserId(),
                getNifty100Stocks(orderConfiguration),
                placeOrders, connectToWebSocket);
    }

    private List<String> getNifty100Stocks(OrderConfiguration orderConfiguration) {
        List<String> nifty100Symbols = new ArrayList<>();
        log.info("symbolsPath : {}", orderConfiguration.getSymbolsPath());
        try (BufferedReader br = new BufferedReader(new FileReader(orderConfiguration.getSymbolsPath()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                nifty100Symbols.add(values[2]);
            }
        } catch (IOException e) {
            throw new InitialisationException("Exception while fetching nifty 100 symbols", e);
        }
        log.info("Symbols : {}", Arrays.asList(orderConfiguration.getAdditionalSymbols()));
        nifty100Symbols.addAll(Arrays.asList(orderConfiguration.getAdditionalSymbols()));
        return nifty100Symbols;
    }

    public List<String> getHolidayDates() {
        return kiteClientProperties.getHolidays2023();
    }
}
