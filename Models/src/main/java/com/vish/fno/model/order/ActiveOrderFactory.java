package com.vish.fno.model.order;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActiveOrderFactory {

    public static ActiveOrder createOrder(OrderRequest orderRequest, double ltp, int timestamp) {
        ActiveOrder order = null;

        if(orderRequest instanceof IndexOrderRequest indexOrder) {
            order = new ActiveIndexOrder(indexOrder, ltp, timestamp);
        }
        return order;
    }
}
