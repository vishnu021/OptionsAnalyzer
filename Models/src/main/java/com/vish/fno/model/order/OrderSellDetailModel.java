package com.vish.fno.model.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderSellDetailModel {
    private boolean sellOrder;
    private int quantity;
    private ActiveOrder order;

    public OrderSellDetailModel(boolean sellOrder) {
        if(sellOrder) {
            throw new IllegalStateException("Please provide quantity and order as well");
        }
        this.sellOrder = sellOrder;
    }
}
