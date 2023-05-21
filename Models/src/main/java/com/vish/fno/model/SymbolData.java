package com.vish.fno.model;

import com.zerodhatech.models.HistoricalData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
@Getter
@ToString
@NoArgsConstructor
@Document(collection = "minute_history_data")
public class SymbolData {
    private static final String FULL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final SimpleDateFormat formatterMilliSecond = new SimpleDateFormat(FULL_DATE_FORMAT);

    @Id
    private CandleMetaData record;
    private List<Candle> data;

    public SymbolData(HistoricalData hd, String symbol, String date) throws ParseException {
        this.record = new CandleMetaData(symbol, date);
        this.data = hd.dataArrayList.stream()
                .map(h -> new Candle(h.timeStamp, h.open, h.high, h.low, h.close, h.volume, h.oi))
                .toList();
    }
}
