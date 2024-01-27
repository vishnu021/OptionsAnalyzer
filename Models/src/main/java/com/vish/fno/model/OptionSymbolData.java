package com.vish.fno.model;

import com.zerodhatech.models.HistoricalData;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@Document(collection = "minute_option_data")
public class OptionSymbolData {
    private static final String FULL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final SimpleDateFormat formatterMilliSecond = new SimpleDateFormat(FULL_DATE_FORMAT, Locale.ENGLISH);

    @Id
    private OptionMetaData record;
    private List<Candle> data;

    public OptionSymbolData(HistoricalData hd, String symbol, String date, String expiryDate) {
        this.record = new OptionMetaData(symbol, date, expiryDate);
        this.data = hd.dataArrayList.stream()
                .map(h -> new Candle(h.timeStamp, h.open, h.high, h.low, h.close, h.volume, h.oi))
                .toList();
    }
}
