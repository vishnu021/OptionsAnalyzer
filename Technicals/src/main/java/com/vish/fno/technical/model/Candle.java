package com.vish.fno.technical.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private long oi;
}
