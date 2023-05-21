package com.vish.fno.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Getter
@ToString
@NoArgsConstructor
public class CandleMetaData {
    private String symbol;
    private String date;

    public CandleMetaData(String symbol, String date) {
        this.symbol = symbol;
        this.date = date;
    }
}
