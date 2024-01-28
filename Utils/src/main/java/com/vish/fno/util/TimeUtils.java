package com.vish.fno.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.vish.fno.util.Constants.*;

@Slf4j
@SuppressWarnings("PMD.AvoidCatchingGenericException")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtils {
    public static final List<String> timeArray = new ArrayList<>();


    static {
        int hour = 9;
        int minute = 15;
        for (int i = 0; i < 375; i++) {
            timeArray.add(toTimeValue(hour) + ":" + toTimeValue(minute));
            minute++;
            if (minute == 60) {
                hour++;
                minute = 0;
            }
        }
    }

    public static String getTimeStringForZonedDateString(String date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_SEC_T_FORMAT, Locale.ENGLISH);
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(date, formatter);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT, Locale.ENGLISH);
            return timeFormatter.format(zonedDateTime);
        } catch (Exception e) {
            log.error("Failed to format date string: {} to pattern: {}", date, DATE_TIME_SEC_T_FORMAT, e);
        }
        return null;
    }

    public static String getDateTimeStringForZonedDateString(String date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_SEC_T_FORMAT, Locale.ENGLISH);
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(date, formatter);

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT, Locale.ENGLISH);
            return timeFormatter.format(zonedDateTime);
        } catch (Exception e) {
            log.error("Failed to format date string: {} to pattern: {}", date, DATE_TIME_SEC_T_FORMAT, e);
        }
        return null;
    }

    public  static int getIndexOfTime(String time) {
            return timeArray.indexOf(time);
    }

    private static String toTimeValue(int timeVal) {
        if (timeVal >= 0 && timeVal <= 9) {
            return ("0" + timeVal);
        }
        return String.valueOf(timeVal);
    }

    public static Date appendOpeningTimeToDate(Date day) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_SEC_FORMAT, Locale.ENGLISH);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        try {
            return formatter.parse(dateFormatter.format(day) + " 09:15:00");
        } catch (ParseException e) {
            log.error("Failed to parse date",e);
        }
        return day;
    }

    public static Date appendClosingTimeToDate(Date day) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_SEC_FORMAT, Locale.ENGLISH);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        try {
            return formatter.parse(dateFormatter.format(day) + " 15:30:00");
        } catch (ParseException e) {
            log.error("",e);
        }
        return day;
    }

    public static Date getNDaysBefore(long n) {
        return new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(n));
    }

    public static Date currentTime() {
        return new Date(System.currentTimeMillis());
    }

    public static String getTime() {
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        return timeFormatter.format(currentTime());
    }

    public static String getTime(Date timeStamp) {
        if (timeStamp == null) {
            return null;
        }
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        return timeFormatter.format(timeStamp);
    }

    public static int getIndexOfTimeStamp(Date timeStamp) {
        if (timeStamp == null) {
            return -1;
        }
        return getIndexOfTime(getTime(timeStamp));
    }

    public static Date getDateTimeObjectMinute(String date) {
        SimpleDateFormat formatterMilliSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        try {
            return formatterMilliSecond.parse(date);
        } catch (ParseException e) {
            log.error("Failed to parse date",e);
        }
        return null;
    }

    public static Date getDateObject(String date) {
        SimpleDateFormat formatterMilliSecond = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        try {
            return formatterMilliSecond.parse(date);
        } catch (ParseException e) {
            log.error("Failed to parse to date of format yyyy-MM-dd",e);
        }
        return null;
    }

    public static String getTodayDate() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        return dateFormatter.format(currentTime());
    }

    public static String getStringDate(Date date) {
        if(date==null) {
            return "";
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        return dateFormatter.format(date);
    }

    public static String getStringDateTime(Date timeStamp) {
        if (timeStamp == null) {
            return null;
        }
        SimpleDateFormat formatterMilliSecond = new SimpleDateFormat(DATE_TIME_MS_FORMAT, Locale.ENGLISH);
        return formatterMilliSecond.format(timeStamp);
    }
}
