package com.vish.fno.manage.config.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(OrderProperties.class)
public class OrderConfiguration {

    private final OrderProperties orderProperties;

    public String getSymbolsPath() {
        return orderProperties.getSymbolsPath();
    }

    public double getAvailableCash() {
        return orderProperties.getAvailableCash();
    }

    public String[] getAdditionalSymbols() {
        return orderProperties.getAdditionalSymbols();
    }

    public String[] getWebSocketDefaultSymbols() {
        return orderProperties.getWebSocketDefaultSymbols();
    }
}
