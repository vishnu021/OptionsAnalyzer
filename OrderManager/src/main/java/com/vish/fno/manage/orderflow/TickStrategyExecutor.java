package com.vish.fno.manage.orderflow;

import com.vish.fno.model.Ticker;
import com.vish.fno.model.strategy.TickBasedStrategy;
import com.vish.fno.util.helper.TimeProvider;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class TickStrategyExecutor  {
    private final List<TickBasedStrategy> strategies;
    private final TimeProvider timeProvider;


    public void update(Ticker ticker) {
        strategies.forEach(s -> executeStrategy(s, ticker));
    }

    private void executeStrategy(TickBasedStrategy tickBasedStrategy, Ticker ticker) {
        String index = tickBasedStrategy.getTask().getIndex();
        String tickerSymbol = ticker.getInstrumentSymbol();
        if(index.equals(tickerSymbol)) {
            tickBasedStrategy.test(List.of(ticker), timeProvider.currentTimeStampIndex());
        }
    }
}
