package com.vish.fno.model.order;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class AbstractActiveOrder implements ActiveOrder {
    protected final String tag;
    protected Date date;
    protected int entryTimeStamp;
    @Setter
    protected int exitTimeStamp;
    protected double buyThreshold;
    protected double buyPrice;
    protected int buyQuantity;
    @Setter
    protected int soldQuantity;
    @Setter
    protected double sellPrice;
    protected double target;
    protected double stopLoss;
    protected Map<String, String> extraData;
    private int stopLossRevisionCount;

    public AbstractActiveOrder(String tag,
                               Date date,
                               int entryTimeStamp,
                               double buyThreshold,
                               double buyPrice,
                               int buyQuantity,
                               double target,
                               double stopLoss) {
        this.tag = tag;
        this.date = date;
        this.entryTimeStamp = entryTimeStamp;
        this.buyThreshold = buyThreshold;
        this.buyPrice = buyPrice;
        this.buyQuantity = buyQuantity;
        this.soldQuantity = 0;
        this.target = target;
        this.stopLoss = stopLoss;
        this.extraData = new HashMap<>();
        this.stopLossRevisionCount = 0;
    }

    public void setStopLoss(double stopLoss) {
        this.extraData.put("previousStopLoss" + (++stopLossRevisionCount), String.valueOf(stopLoss));
        this.stopLoss = stopLoss;
    }
}
