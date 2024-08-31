package com.vish.fno.manage.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vish.fno.model.Ticker;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class TickMapperTest {

    private static final String TICK_JSON = """
        {
            "mode": "full",
            "instrumentToken": 13259778,
            "lastTradedPrice": 153.9,
            "change": -43.53329664281783,
            "lastTradedQuantity": 75.0,
            "averageTradePrice": 153.63,
            "volumeTradedToday": 1421325,
            "totalBuyQuantity": 299575.0,
            "totalSellQuantity": 74925.0,
            "lastTradedTime": 1722570360000,
            "oi": 1077275.0,
            "openInterestDayHigh": 1077275.0,
            "openInterestDayLow": 752125.0,
            "tickTimestamp": 1722570360000,
            "marketDepth": {
                "buy": [
                    {"quantity": 75, "price": 153.6, "orders": 2},
                    {"quantity": 25, "price": 153.55, "orders": 1},
                    {"quantity": 850, "price": 153.5, "orders": 4},
                    {"quantity": 450, "price": 153.45, "orders": 5},
                    {"quantity": 600, "price": 153.4, "orders": 6}
                ],
                "sell": [
                    {"quantity": 150, "price": 153.9, "orders": 1},
                    {"quantity": 300, "price": 153.95, "orders": 2},
                    {"quantity": 500, "price": 154.0, "orders": 1},
                    {"quantity": 450, "price": 154.05, "orders": 2},
                    {"quantity": 1000, "price": 154.1, "orders": 5}
                ]
            }
        }
        """;

    @Test
    void testMapTicks_withJsonTick() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        Tick tick = objectMapper.readValue(TICK_JSON, Tick.class);

        List<Tick> tickList = List.of(tick);

        // When
        Ticker ticker = TickMapper.mapTick(tick, "");

        // Then
        assertEquals(153.9, ticker.getLastTradedPrice());
        assertEquals(13259778, ticker.getInstrumentToken());
        assertEquals(-43.53329664281783, ticker.getChange());

        // Assert Market Depth (buy)
        assertNotNull(ticker.getDepth());
        List<Ticker.Depth> buyDepth = ticker.getDepth().get("buy");
        assertEquals(5, buyDepth.size());

        assertEquals(75, buyDepth.get(0).getQuantity());
        assertEquals(153.6, buyDepth.get(0).getPrice());
        assertEquals(2, buyDepth.get(0).getOrders());

        assertEquals(600, buyDepth.get(4).getQuantity());
        assertEquals(153.4, buyDepth.get(4).getPrice());
        assertEquals(6, buyDepth.get(4).getOrders());

        // Assert Market Depth (sell)
        List<Ticker.Depth> sellDepth = ticker.getDepth().get("sell");
        assertEquals(5, sellDepth.size());

        assertEquals(150, sellDepth.get(0).getQuantity());
        assertEquals(153.9, sellDepth.get(0).getPrice());
        assertEquals(1, sellDepth.get(0).getOrders());

        assertEquals(1000, sellDepth.get(4).getQuantity());
        assertEquals(154.1, sellDepth.get(4).getPrice());
        assertEquals(5, sellDepth.get(4).getOrders());
    }

}