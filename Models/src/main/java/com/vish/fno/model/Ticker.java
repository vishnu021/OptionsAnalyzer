package com.vish.fno.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ticker {
    private String mode;
    private long instrumentToken;
    private double lastTradedPrice;
    @ToString.Exclude
    private double change;
    @ToString.Exclude
    private double lastTradedQuantity;
    @ToString.Exclude
    private double averageTradePrice;
    @ToString.Exclude
    private long volumeTradedToday;
    @ToString.Exclude
    private double totalBuyQuantity;
    @ToString.Exclude
    private double totalSellQuantity;
    @ToString.Exclude
    private Date lastTradedTime;
    @ToString.Exclude
    private double oi;
    @ToString.Exclude
    private double oiDayHigh;
    @ToString.Exclude
    private double oiDayLow;
    private Date tickTimestamp;
    @ToString.Exclude
    private Map<String, List<Depth>> marketDepth;

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Depth {
        private int quantity;
        private double price;
        private int orders;
    }
}
