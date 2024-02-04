package com.vish.fno.model.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelUtils {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static double round(double price) {
        return Math.round(price / 0.05) * 0.05;
    }

    public static String getStringDate(Date date) {
        if(date==null) {
            return "";
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        return dateFormatter.format(date);
    }
}
