package com.vish.fno.reader.util;

import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.OrderParams;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderUtils {

    public static OrderParams createOrderWithParameters(String symbol,
                                                        double price,
                                                        int orderSize,
                                                        String transactionType,
                                                        String tag) {
        OrderParams orderParams = new OrderParams();
        orderParams.quantity = orderSize;
        orderParams.orderType = Constants.ORDER_TYPE_MARKET;
        orderParams.tradingsymbol = symbol;
        orderParams.product = Constants.PRODUCT_MIS;
        orderParams.exchange = Constants.EXCHANGE_NFO;
        orderParams.validity = Constants.VALIDITY_DAY;
        orderParams.transactionType = transactionType;
        orderParams.price = price;
        orderParams.triggerPrice = 0.0;
        orderParams.tag = tag;
        return orderParams;
    }
}
