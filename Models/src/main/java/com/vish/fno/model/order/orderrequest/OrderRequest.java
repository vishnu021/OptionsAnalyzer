package com.vish.fno.model.order.orderrequest;

import com.vish.fno.model.Task;
import com.vish.fno.model.Ticker;

import java.util.Optional;

public interface OrderRequest {
    String getIndex();
    String getOptionSymbol();
    void setOptionSymbol(String optionSymbol);
    double getBuyThreshold();
    double getTarget();
    int getQuantity();
    int getExpirationTimestamp();
    String getTag();
    boolean isCallOrder();
    Task getTask();
    void placeOrder(Ticker ticker);
    Optional<OrderRequest> verifyBuyThreshold(Ticker tick);
}
