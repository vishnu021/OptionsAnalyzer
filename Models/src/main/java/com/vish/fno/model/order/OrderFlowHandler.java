package com.vish.fno.model.order;

import com.vish.fno.model.Ticker;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.IndexOrderRequest;
import com.vish.fno.model.order.orderrequest.OrderRequest;

import java.util.Optional;

public interface OrderFlowHandler {
    void placeOrder(Ticker tick, OrderRequest order);
    void sellOrder(OrderSellDetailModel exitCondition, Ticker tick, ActiveOrder order);

    Optional<OrderRequest> verifyBuyThreshold(Ticker tick, IndexOrderRequest indexOrderRequest);
    boolean hasMoveAlreadyHappened(double ltp, OrderRequest order);
}
