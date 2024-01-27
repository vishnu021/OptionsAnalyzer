package com.vish.fno.model.order;

public interface OpenOrder {

    String getIndex();
    String getOptionSymbol();
    void setOptionSymbol(String optionSymbol);
    double getBuyThreshold();
    int getQuantity();
    int getExpirationTimestamp();
    String getTag();
    boolean isCallOrder();
}
