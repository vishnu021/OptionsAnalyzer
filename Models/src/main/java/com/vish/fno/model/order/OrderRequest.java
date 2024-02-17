package com.vish.fno.model.order;

import com.vish.fno.model.Task;

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
}
