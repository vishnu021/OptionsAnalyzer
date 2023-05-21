package com.vish.fno.manage.util;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;

public interface Constants {
    String directory = "instrument_cache";
    String MINUTE = "minute";
    String STOCK_PRICE = "Stock Price";
    String CANDLESTICK = "candlestick";
    String VOLUME = "Volume";
    String LINE = "line";
    String BAR = "bar";
    String EMA14 = "EMA14";
    String filePath = Paths.get(".").normalize().toAbsolutePath() + "\\" + directory + "\\";

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    SimpleDateFormat formatterForFile = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
    SimpleDateFormat timeSecondFormatter = new SimpleDateFormat("HH:mm:ss.SSS");

}
