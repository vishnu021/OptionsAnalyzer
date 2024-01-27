package com.vish.fno.manage.config.task;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@EnableConfigurationProperties(TaskProperties.class)
public class TaskConfig {

    @Autowired
    private TaskProperties taskProperties;
}
