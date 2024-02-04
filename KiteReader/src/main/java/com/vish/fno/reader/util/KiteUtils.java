package com.vish.fno.reader.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KiteUtils {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final static ObjectMapper mapper = new ObjectMapper();

    public static String getFormattedOrderParams(OrderParams orderParams) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderParams);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse order params for order with symbol: {} and tag: {}",
                    orderParams.tradingsymbol, orderParams.tag);
        }
        return null;
    }

    public static String getFormattedOrder(Order order) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse order params for order with id: {} ", order.orderId);
        }
        return null;
    }

    public static String getFormattedObject(Object order) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse object", e);
        }
        return null;
    }

    public static String getStringDate(Date date) {
        if(date==null) {
            return "";
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        return dateFormatter.format(date);
    }
}
