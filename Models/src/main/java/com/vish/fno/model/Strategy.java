package com.vish.fno.model;

import com.vish.fno.model.Task;
import com.vish.fno.model.Candle;
import com.vish.fno.model.order.OpenIndexOrder;
import com.vish.fno.model.order.OpenOrder;

import java.util.List;
import java.util.Optional;

public interface Strategy {
    void initialise(Task task);
    Task getTask();
    Optional<OpenOrder> test(List<Candle> candles, int timestamp);
    String getTag();
}
