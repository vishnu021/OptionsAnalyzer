package com.vish.fno.manage.helper;

import com.vish.fno.model.Ticker;
import com.zerodhatech.models.Tick;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TickMapper {
    public static Ticker mapTick(Tick tick, String tickSymbol) {
        return mapTicker(tick, tickSymbol);
    }

    private static Ticker mapTicker(Tick tick, String tickSymbol) {
        Map<String, List<Ticker.Depth>> marketDepth = Optional.ofNullable(tick.getMarketDepth())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Optional.ofNullable(entry.getValue())
                                .orElse(new ArrayList<>())
                                .stream()
                                .map(depth -> Ticker.Depth.builder()
                                        .quantity(Optional.ofNullable(depth.getQuantity()).orElse(0))
                                        .price(Optional.ofNullable(depth.getPrice()).orElse(0.0))
                                        .orders(Optional.ofNullable(depth.getOrders()).orElse(0))
                                        .build())
                                .collect(Collectors.toList())
                ));

        return Ticker.builder()
                .instrumentSymbol(tickSymbol)
                .mode(Optional.ofNullable(tick.getMode()).orElse(""))
                .instrumentToken(Optional.ofNullable(tick.getInstrumentToken()).orElse(0L))
                .lastTradedPrice(Optional.ofNullable(tick.getLastTradedPrice()).orElse(0.0))
                .change(Optional.ofNullable(tick.getChange()).orElse(0.0))
                .lastTradedQuantity(Optional.ofNullable(tick.getLastTradedQuantity()).orElse(0.0))
                .averageTradePrice(Optional.ofNullable(tick.getAverageTradePrice()).orElse(0.0))
                .volumeTradedToday(Optional.ofNullable(tick.getVolumeTradedToday()).orElse(0L))
                .totalBuyQuantity(Optional.ofNullable(tick.getTotalBuyQuantity()).orElse(0.0))
                .totalSellQuantity(Optional.ofNullable(tick.getTotalSellQuantity()).orElse(0.0))
                .lastTradedTime(Optional.ofNullable(tick.getLastTradedTime()).orElse(new Date(0)))
                .oi(Optional.ofNullable(tick.getOi()).orElse(0.0))
                .openInterestDayHigh(Optional.ofNullable(tick.getOpenInterestDayHigh()).orElse(0.0))
                .openInterestDayLow(Optional.ofNullable(tick.getOpenInterestDayLow()).orElse(0.0))
                .tickTimestamp(Optional.ofNullable(tick.getTickTimestamp()).orElse(new Date(0)))
                .depth(marketDepth)
                .build();
    }
}
