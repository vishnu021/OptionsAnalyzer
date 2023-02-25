package com.vish.fno.manage.config.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrderProperties.class)
public class OrderConfiguration {

    @Autowired
    private OrderProperties orderProperties;
}
