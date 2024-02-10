package com.vish.fno.model;

import com.zerodhatech.models.HistoricalData;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "minute_history_data")
public class SymbolData {
    private static final String FULL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final SimpleDateFormat formatterMilliSecond = new SimpleDateFormat(FULL_DATE_FORMAT, Locale.ENGLISH);

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
