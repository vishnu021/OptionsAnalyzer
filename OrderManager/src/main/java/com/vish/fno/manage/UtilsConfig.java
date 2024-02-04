package com.vish.fno.manage;

import com.vish.fno.util.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class UtilsConfig {

    @Bean
    public FileUtils fileUtils() {
        return new FileUtils();
    }
}
