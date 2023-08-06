package com.vish.fno.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    private String time;
    private double open;
    private double high;
    private double low;
    private double close;
    @ToString.Exclude
    private Long volume;
    @ToString.Exclude
    private Long oi;

    public boolean isBullish() {
        return close > open;
    }

    public boolean isBearish() {
        return close < open;
    }

    public double getBodyLength() {
        if(isBullish()) {
            return this.getClose() - this.getOpen();
        }
        return this.getOpen() - this.getClose();
    }

    public double getTotalLength() {
        return this.getHigh() - this.getLow();
    }

    public double getUpperWick() {
        if(isBullish()) {
            return this.getHigh() - this.getClose();
        }
        return this.getHigh() - this.getOpen();
    }

    public double getLowerWick() {
        if(isBullish()) {
            return this.getOpen() - this.getLow();
        }
        return this.getClose() - this.getLow();
    }
}
