package com.vish.fno.manage.config.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(TaskProperties.class)
public class TaskConfig {
    private final TaskProperties taskProperties;
}
