package com.vish.fno.manage.config.order;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("order")
public class OrderProperties {
    private int maximumOrderCount;
    private double availableCash;
    private boolean placeOrders;
    private boolean putOrders;
    private boolean callOrders;
    private boolean tradeInLastHour;
    private int orderSize;
    private double maximumBuyPrice;
    private double minimumBuyPrice;
    private int slmDeltaPrice;
    private String[] additionalSymbols;
    private String symbolsPath;
    private String[] webSocketDefaultSymbols;
}
