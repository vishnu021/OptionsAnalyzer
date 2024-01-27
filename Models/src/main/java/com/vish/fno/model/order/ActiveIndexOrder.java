package com.vish.fno.model.order;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.vish.fno.util.TimeUtils.getStringDate;
import static com.vish.fno.util.TimeUtils.timeArray;
import static com.vish.fno.util.Utils.round;

@Getter
@Setter
public class ActiveIndexOrder implements ActiveOrder {

    private static final int estimated_buffer_size = 125;
    private final String tag;
    private String index;
    private String optionSymbol;
    private Date date;
    private int entryTimeStamp;
    private int exitTimeStamp;
    private double buyThreshold;
    private double buyPrice;
    private double sellPrice;
    private double target;
    private double stopLoss;

    private double buyOptionPrice;
    private double sellOptionPrice;
    private Date entryDatetime;
    private Date exitDatetime;
    private int quantity;
    private boolean isActive;
    private boolean callOrder;
    private Map<String, String> extraData;

    public void closeOrder(double closePrice, int timestamp) {
        setActive(false);
        setExitTimeStamp(timestamp);
        setSellPrice(closePrice);
    }

    public ActiveIndexOrder(OpenIndexOrder openOrder, double buyPrice, int timestamp) {
        this.index = openOrder.getIndex();
        this.optionSymbol = openOrder.getOptionSymbol();
        this.date = openOrder.getDate();
        this.entryTimeStamp = timestamp;
        this.buyThreshold = openOrder.getBuyThreshold();
        this.buyPrice = buyPrice;
        this.target = openOrder.getTarget();
        this.stopLoss = openOrder.getStopLoss();
        this.quantity = openOrder.getQuantity();
        this.callOrder = openOrder.isCallOrder();
        this.extraData = openOrder.getExtraData();
        this.tag = openOrder.getTag();
        this.isActive = true;
    }

    public double getProfit() {
        if(isCallOrder()) {
            return (getSellPrice() - getBuyPrice()) * getQuantity();
        } else {
            return (getBuyPrice() - getSellPrice()) * getQuantity();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(estimated_buffer_size);
        sb.append("\nActiveOrder{")
                .append("index='").append(index).append("'")
                .append(", optionSymbol='").append(optionSymbol).append("'")
                .append(", buyPrice=").append(buyPrice)
                .append(", target=").append(round(target))
                .append(", stopLoss=").append(round(stopLoss))
                .append("}");

        return sb.toString();
    }

    public String csvHeader() {
        final int estimatedBufferSize = 250; // Adjust based on expected number of columns and length of extraData keys
        final StringBuilder sb = new StringBuilder(estimatedBufferSize);

        sb.append("symbol").append(",")
                .append("entryTimeStamp").append(",")
                .append("exitTimeStamp").append(",")
                .append("buyThreshold").append(",")
                .append("buyPrice").append(",")
                .append("target").append(",")
                .append("sellPrice").append(",")
                .append("stopLoss").append(",")
                .append("profit").append(",")
                .append("quantity").append(",")
                .append("reward").append(",")
                .append("call");

        if (extraData != null) {
            for (String key : extraData.keySet()) {
                sb.append(",").append(key);
            }
        }

        return sb.toString();
    }


    public String toCSV() {
        final StringBuilder sb = new StringBuilder(estimated_buffer_size);
        sb.append(index)
                .append(',')
                .append(' ').append(getStringDate(date))
                .append(' ').append(timeArray.get(entryTimeStamp))
                .append(',')
                .append(' ').append(getStringDate(date))
                .append(' ').append(timeArray.get(exitTimeStamp))
                .append(',')
                .append(' ').append(round(buyThreshold))
                .append(',')
                .append(' ').append(round(buyPrice))
                .append(',')
                .append(' ').append(round(target))
                .append(',')
                .append(' ').append(round(sellPrice))
                .append(',')
                .append(' ').append(round(stopLoss))
                .append(',')
                .append(' ').append(round(getProfit()))
                .append(',')
                .append(' ').append(round(quantity));

        if (isCallOrder()) {
            sb.append(',')
                    .append(' ').append(round(target - buyThreshold));
        } else {
            sb.append(',')
                    .append(' ').append(round(buyThreshold - target));
        }

        sb.append(',')
                .append(' ').append(isCallOrder());

        if (extraData != null) {
            for (String key : extraData.keySet()) {
                sb.append(',')
                        .append(' ').append(extraData.get(key));
            }
        }

        return sb.toString();
    }


    public String orderLog() {
        final StringBuilder sb = new StringBuilder(estimated_buffer_size);
        sb.append("OrderLog{")
                .append("index='").append(index).append("'")
                .append(",\tentry=").append(getStringDate(date)).append(" ").append(timeArray.get(entryTimeStamp))
                .append(",\texit=").append(timeArray.get(exitTimeStamp))
                .append(",\tbuy=").append(round(buyPrice))
                .append(",\ttarget=").append(round(target))
                .append(",\tsell=").append(round(sellPrice))
                .append(",\tcall=").append(isCallOrder());

        if (getProfit() > 0) {
            sb.append(",\tprofit=");
        } else {
            sb.append(",\tloss=");
        }

        sb.append(round(getProfit())).append("}");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)  {
            return true;
        }
        if (o == null || getClass() != o.getClass())  {
            return false;
        }
        ActiveIndexOrder that = (ActiveIndexOrder) o;
        return Objects.equals(tag, that.tag) && Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, index);
    }
}
