package com.vish.fno.model.helper;

import com.vish.fno.model.order.OrderSellDetailModel;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.OrderRequest;

import java.util.List;

public interface EntryVerifier {
    String ORDER_EXECUTED = "orderExecuted";
    boolean hasMoveAlreadyHappened(double lastTradedPrice, OrderRequest order);
    boolean isNotInActiveOrders(List<ActiveOrder> activeOrders, OrderRequest order);
    boolean isPlaceOrder(ActiveOrder activeOrder, boolean b, OrderSellDetailModel exitCondition, OrderCache orderCache, boolean isExpiryDayForOption);
}
