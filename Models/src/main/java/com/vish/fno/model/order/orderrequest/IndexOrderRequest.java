package com.vish.fno.model.order.orderrequest;

import com.vish.fno.model.Task;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.vish.fno.model.util.ModelUtils.round;

@Getter
@Builder
public class IndexOrderRequest implements OrderRequest {
    private static final int estimated_buffer_size = 100;
    private final Task task;
    private final String tag;
    private final String index;
    @Setter
    private String optionSymbol;
    private Date date;
    private int timestamp;
    private int expirationTimestamp;
    private double buyThreshold;
    private double target;
    private double stopLoss;
    private int quantity;
    private boolean callOrder;
    private Map<String, String> extraData;
    private int lotSize;

    @Builder(builderMethodName = "builder")
    public IndexOrderRequest(Task task, String tag, String index, String optionSymbol, Date date, int timestamp,
                             int expirationTimestamp, double buyThreshold, double target, double stopLoss, int quantity,
                             boolean callOrder, Map<String, String> extraData, int lotSize) {
        this.task = task;
        this.tag = tag == null ? "" : tag.replaceAll("[a-z]", "");
        this.index = index;
        this.optionSymbol = optionSymbol;
        this.date = date;
        this.timestamp = timestamp;
        this.expirationTimestamp = expirationTimestamp;
        this.buyThreshold = buyThreshold;
        this.target = target;
        this.stopLoss = stopLoss;
        this.quantity = quantity;
        this.callOrder = callOrder;
        this.extraData = extraData;
        this.lotSize = lotSize;
    }

    public static IndexOrderRequestBuilder builder(String tag, String index, Task task) {
        return new IndexOrderRequestBuilder().tag(tag).index(index).task(task);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(estimated_buffer_size);
        sb.append("\nIndexOrderRequest{")
                .append("date=").append(date)
                .append(", index=").append(index)
                .append(", tag=").append(tag)
                .append(", expiry=").append(expirationTimestamp)
                .append(", buyAt=").append(round(buyThreshold))
                .append(", target=").append(round(target))
                .append(", stopLoss=").append(round(stopLoss));

        if(isCallOrder()) {
            sb.append(", risk=").append(round(buyThreshold - stopLoss))
                    .append(", reward=").append(round(target - buyThreshold));
        } else {
            sb.append(", risk=").append(round(stopLoss - buyThreshold))
                    .append(", reward=").append(round(buyThreshold - target));
        }

        sb.append(", CE=").append(isCallOrder())
                .append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final IndexOrderRequest that = (IndexOrderRequest) o;
        return Objects.equals(tag, that.tag)
                && Objects.equals(index, that.index)
                && Objects.equals(callOrder, that.callOrder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, index, callOrder);
    }
}
