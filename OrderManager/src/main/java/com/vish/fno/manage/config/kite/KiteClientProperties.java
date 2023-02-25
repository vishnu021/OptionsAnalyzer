package com.vish.fno.manage.config.kite;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("kite")
public class KiteClientProperties {
    private String apiKey;
    private String userId;
    private String apiSecret;
    private boolean liveData;
    private String authenticationUrl;
    private boolean deployedInCloud;
}
