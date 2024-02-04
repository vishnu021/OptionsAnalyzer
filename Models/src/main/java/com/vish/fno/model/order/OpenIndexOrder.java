package com.vish.fno.model.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.vish.fno.model.util.ModelUtils.round;

@Getter
@Builder
public class OpenIndexOrder implements OpenOrder {

    private static final int estimated_buffer_size = 100;
    private String tag;
    private String index;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(estimated_buffer_size);
        sb.append("\nOpenIndexOrder{")
                .append("date=").append(date)
                .append(", index=").append(index)
                .append(", expiry=").append(expirationTimestamp)
                .append(", buyAt=").append(round(buyThreshold))
                .append(", target=").append(round(target))
                .append(", stopLoss=").append(round(stopLoss))
                .append(", CE=").append(isCallOrder())
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
        OpenIndexOrder that = (OpenIndexOrder) o;
        return Objects.equals(tag, that.tag) && Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, index);
    }
}
