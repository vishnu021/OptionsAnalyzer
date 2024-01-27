package com.vish.fno.model.order;

import java.util.Date;

public interface ActiveOrder {

    String getIndex();
    Date getDate();
    String getOptionSymbol();
    double getTarget();
    double getStopLoss();
    double getBuyPrice();
    double getSellPrice();
    void setBuyOptionPrice(double buyOptionPrice);
    double getBuyOptionPrice();
    void setSellOptionPrice(double sellOptionPrice);
    void setSellPrice(double sellPrice);
    void setExitTimeStamp(int exitTimeStamp);
    void setEntryTimeStamp(int entryTimeStamp);
    void setEntryDatetime(Date entryDatetime);
    void setExitDatetime(Date exitDatetime);
    int getQuantity();
    String getTag();
    boolean isActive();
    boolean isCallOrder();
    String csvHeader();
    String toCSV();
    String orderLog();
    void setActive(boolean active);
    void closeOrder(double closePrice, int timestamp);

}
