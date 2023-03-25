package com.vish.fno.model;

import com.zerodhatech.models.HistoricalData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.text.ParseException;

@Slf4j
@Getter
@ToString
@NoArgsConstructor
@Document(collection = "minute_history_data")
public class CandleStick {
    @Id
    private SymbolData record;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private long oi;

    public CandleStick(HistoricalData hd, String symbol) throws ParseException {
        this.record = new SymbolData(symbol, hd.timeStamp);
        this.open = hd.open;
        this.high = hd.high;
        this.low = hd.low;
        this.close = hd.close;
        this.volume = hd.volume;
        this.oi = hd.oi;

        // This model has been created with few assumptions regarding the candlestick data coming from broker,
        // in case these conditions are not met, This model needs to be recreated.
        if(hd.dataArrayList!=null && hd.dataArrayList.size()>0) {
            log.error("Invalid model for, symbol : {}, dataArrayList : {}", symbol, hd.dataArrayList);
            hd.dataArrayList.forEach(h ->
                    log.error("oi: {}, open: {}, high: {}, low: {}, close: {}, volume: {}, oi: {}, dataArrayList : {}",
                            hd.oi,  hd.open,  hd.high,  hd.low,  hd.close, hd.volume, hd.oi, hd.dataArrayList));
            throw new IllegalStateException("Data Array List is not null, recreate model");
        }
    }
}
