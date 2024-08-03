package com.vish.fno.manage.helper;

import com.vish.fno.model.order.OrderSellDetailModel;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.OrderRequest;

import java.util.List;

public interface EntryVerifier {
    boolean hasMoveAlreadyHappened(double lastTradedPrice, OrderRequest order);
    boolean isNotInActiveOrders(List<ActiveOrder> activeOrders, OrderRequest order);
    boolean isPlaceOrder(ActiveOrder activeOrder, boolean b, OrderSellDetailModel exitCondition, OrderCache orderCache);
}
