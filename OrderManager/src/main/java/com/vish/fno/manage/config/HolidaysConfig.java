package com.vish.fno.manage.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
@ConfigurationProperties(prefix = "holidays")
public class HolidaysConfig {

    @Autowired
    private Properties additionalProperties;

    @Getter
    private final List<String> holidays = new ArrayList<>();

    @PostConstruct
    public void init() {

        for (int i = 0; true; i++) {
            String holiday = additionalProperties.getProperty("holidays[" + i + "]");
            if (holiday == null) {
                break;
            }
            holidays.add(holiday);
        }
    }
}
