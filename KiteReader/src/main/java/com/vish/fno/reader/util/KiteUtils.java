package com.vish.fno.reader.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KiteUtils {
    public static final String NIFTY_BANK = "NIFTY BANK";
    public static final String NIFTY_50 = "NIFTY 50";
    public static final  String NIFTY_FIN_SERVICE = "NIFTY FIN SERVICE";

    public static final String MINUTE = "minute";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final static ObjectMapper mapper = new ObjectMapper();

    public static String getFormattedOrderParams(OrderParams orderParams) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderParams);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse order params for order with symbol: {} and tag: {}",
                    orderParams.tradingsymbol, orderParams.tag);
        }
        return "";
    }

    public static String getFormattedOrder(Order order) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse order params for order with id: {} ", order.orderId);
        }
        return "";
    }

    public static String getFormattedObject(Object order) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse object", e);
        }
        return "";
    }

    public static String getStringDate(Date date) {
        if(date == null) {
            return "";
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        return dateFormatter.format(date);
    }

    public static Date getOpeningTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 15);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getClosingTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
