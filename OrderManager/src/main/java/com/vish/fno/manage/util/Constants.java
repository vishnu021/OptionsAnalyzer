package com.vish.fno.manage.util;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings("PMD.ConstantsInInterface")
public interface Constants {

    String NIFTY_BANK = "NIFTY BANK";
    String NIFTY_50 = "NIFTY 50";
    String NIFTY_FIN_SERVICE = "NIFTY FIN SERVICE";
    String directory = "instrument_cache";
    String tick_directory = "tick";
    String MINUTE = "minute";
    String STOCK_PRICE = "Stock Price";
    String CANDLESTICK = "candlestick";
    String VOLUME = "Volume";
    String LINE = "line";
    String BAR = "bar";
    String filePath = Paths.get(".").normalize().toAbsolutePath() + "\\" + directory + "\\";
    String tickPath = Paths.get(".").normalize().toAbsolutePath() + "\\" + tick_directory + "\\";

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
    SimpleDateFormat formatterForFile = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.ENGLISH);
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    SimpleDateFormat timeSecondFormatter = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH);

}
