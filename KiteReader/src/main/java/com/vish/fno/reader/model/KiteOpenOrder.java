package com.vish.fno.reader.model;

import com.zerodhatech.models.Order;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KiteOpenOrder {
    private Order order;
    private boolean isOrderPlaced;
    private Integer exceptionCode;
    private String exceptionMessage;
}
