package com.vish.fno.manage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(HolidaysConfig.class)
public class YamlConfig {

    private static final String HOLIDAYS_YML = "holidays.yml";

    @Bean
    public Properties additionalProperties() {
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        Resource yamlResource = new ClassPathResource(HOLIDAYS_YML);
        yamlFactory.setResources(yamlResource);

        return yamlFactory.getObject();
    }
}

