package com.vish.fno.model;

import com.vish.fno.model.order.orderrequest.OrderRequest;

import java.util.List;
import java.util.Optional;

public interface Strategy {
    void initialise(Task task);
    Task getTask();
    Optional<OrderRequest> test(List<Candle> candles, int timestamp);
    String getTag();
}
