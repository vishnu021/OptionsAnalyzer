package com.vish.fno.model;

import com.vish.fno.reader.util.TimeUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


@Getter
@ToString
@NoArgsConstructor
public class SymbolData {
    private String symbol;
    private String date;
    private String time;

    private static final String FULL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final SimpleDateFormat formatterMilliSecond = new SimpleDateFormat(FULL_DATE_FORMAT);


    public SymbolData(String symbol, String timeStamp) throws ParseException {
        this.symbol = symbol;
        Date date = formatterMilliSecond.parse(timeStamp);
        this.date = TimeUtils.getStringDate(date);
        this.time = TimeUtils.getTime(date);
    }
}
