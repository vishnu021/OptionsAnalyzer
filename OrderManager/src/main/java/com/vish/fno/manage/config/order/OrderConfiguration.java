package com.vish.fno.manage.config.order;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Configuration
@SuppressWarnings("PMD.UnusedPrivateField")
@EnableConfigurationProperties(OrderProperties.class)
public class OrderConfiguration {

    @Autowired
    private OrderProperties orderProperties;

}
