package com.vish.fno.manage.config.order;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("order")
public class OrderProperties {
    private int maximumOrderCount;
    private int availableCash;
    private boolean placeOrders;
    private boolean putOrders;
    private boolean callOrders;
    private boolean tradeInLastHour;
    private int orderSize;
    private int maximumBuyPrice;
    private int minimumBuyPrice;
    private int slmDeltaPrice;
}
