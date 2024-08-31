package com.vish.fno.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.vish.fno.model.util.ModelUtils.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ticker implements Comparable<Ticker> {
    private String mode;
    private boolean tradable;
    private long instrumentToken;
    private String instrumentSymbol;
    private double lastTradedPrice;
    private double highPrice;
    private double lowPrice;
    private double openPrice;
    private double closePrice;
    private double change;
    private double lastTradedQuantity;
    private double averageTradePrice;
    private long volumeTradedToday;
    private double totalBuyQuantity;
    private double totalSellQuantity;
    private Date lastTradedTime;
    private double oi;
    private double openInterestDayHigh;
    private double openInterestDayLow;
    private Date tickTimestamp;
    private Map<String, List<Depth>> depth;

    @Builder
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Depth {
        private int quantity;
        private double price;
        private int orders;
    }

    @Override
    public int compareTo(Ticker other) {
        return this.tickTimestamp.compareTo(other.tickTimestamp);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(70);
        sb.append("Ticker{")
          .append("mode='").append(mode).append('\'')
          .append(", token=").append(instrumentToken)
          .append(", symbol=").append(instrumentSymbol)
          .append(", ltp=").append(roundTo5Paise(lastTradedPrice))
          .append(", time=").append(getStringTime(tickTimestamp))
          .append('}');
        return sb.toString();
    }
}
