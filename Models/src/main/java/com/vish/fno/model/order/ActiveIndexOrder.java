package com.vish.fno.model.order;

import com.vish.fno.model.Task;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Objects;

import static com.vish.fno.model.util.ModelUtils.*;

@Getter
@Setter
public class ActiveIndexOrder extends AbstractActiveOrder {
    private static final int estimated_buffer_size = 125;
    private final Task task;
    private String index;
    private String optionSymbol;
    private double buyOptionPrice;
    private double sellOptionPrice;
    private int lotSize;
    private boolean isActive;
    private boolean callOrder;

    public ActiveIndexOrder(IndexOrderRequest openOrder, double buyPrice, int timestampIndex, String timestamp) {
        super(openOrder.getTag(),
                openOrder.getDate(),
                timestampIndex,
                openOrder.getBuyThreshold(),
                buyPrice,
                openOrder.getQuantity(),
                openOrder.getTarget(),
                openOrder.getStopLoss());
        this.index = openOrder.getIndex();
        this.optionSymbol = openOrder.getOptionSymbol();
        this.date = openOrder.getDate();
        this.entryTimeStamp = timestampIndex;
        this.buyThreshold = openOrder.getBuyThreshold();
        this.callOrder = openOrder.isCallOrder();
        this.extraData = openOrder.getExtraData();
        this.task = openOrder.getTask();
        this.isActive = true;
        if(this.extraData == null) {
            this.extraData = new HashMap<>();
        }
        this.extraData.put("entryDateTime", timestamp);
    }

    public void closeOrder(double closePrice, int timeIndex, String timestamp) {
        setActive(false);
        setExitTimeStamp(timeIndex);
        setSellPrice(closePrice);
        this.extraData.put("exitDateTime", timestamp);
    }

    @Override
    public void appendExtraData(String key, String value) {
        if(extraData == null) {
            extraData = new HashMap<>();
        }
        extraData.put(key, value);
    }

    public double getProfit() {
        if(isCallOrder()) {
            return (getSellPrice() - getBuyPrice()) * this.getBuyQuantity();
        } else {
            return (getBuyPrice() - getSellPrice()) * this.getBuyQuantity();
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
                .append(' ').append(entryTimeStamp)
                .append(',')
                .append(' ').append(getStringDate(date))
                .append(' ').append(exitTimeStamp)
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
                .append(' ').append(round(buyQuantity));

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
                .append(",\ttag=").append(tag)
                .append(",\tentry=").append(getStringDateTime(date))
                .append(",\texit=").append(exitTimeStamp)
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
