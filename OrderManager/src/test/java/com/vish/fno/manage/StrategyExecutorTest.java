package com.vish.fno.manage;

import com.vish.fno.manage.helper.TimeProvider;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.Strategy;
import com.vish.fno.model.SymbolData;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.OpenOrder;
import com.vish.fno.reader.service.KiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class StrategyExecutorTest {

    public static final String TEST_STRATEGY = "test_strategy";
    @Mock private KiteService kiteService;
    @Mock private CandlestickService candlestickService;
    @Mock private OrderHandler orderHandler;
    @Mock private Strategy mockStrategy1;
    @Mock private Strategy mockStrategy2;
    @Mock private TimeProvider timeProvider;
    List<Strategy> activeStrategies;
    @InjectMocks
    private StrategyExecutor strategyExecutor;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        activeStrategies = List.of(mockStrategy1, mockStrategy2);

        when(mockStrategy1.getTask()).thenReturn(new StrategyTasks(TEST_STRATEGY, "BANK NIFTY", true));
        when(mockStrategy2.getTask()).thenReturn(new StrategyTasks(TEST_STRATEGY, "NIFTY 50", false));

        List<String> symbolList =  activeStrategies.stream().map(s -> s.getTask().getIndex())
                .collect(Collectors.toCollection(ArrayList::new));
        strategyExecutor = new StrategyExecutor(kiteService, candlestickService, orderHandler, activeStrategies, symbolList, timeProvider);

    }

    @Test
    public void testUpdate_outSideTradingHours() {
        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 8, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);

        strategyExecutor.update();

        verify(orderHandler, never()).removeExpiredOpenOrders(anyInt());
    }

    @Test
    public void testUpdate_kite_Not_Initialised() {
        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 10, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(false);

        strategyExecutor.update();

        verify(orderHandler, never()).removeExpiredOpenOrders(anyInt());
    }

    @Test
    public void testUpdate_WithinTradingHours_without_candleCache() {
        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 10, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);

        strategyExecutor.update();

        verify(orderHandler).removeExpiredOpenOrders(anyInt());
        verify(candlestickService, times(strategyExecutor.getSymbolList().size())).getEntireDayHistoryData(anyString(), anyString());
        for (Strategy strategy :activeStrategies) {
            verify(strategy, times(0)).test(anyList(), anyInt());
        }
    }

    @Test
    public void testUpdate_WithinTradingHours_StrategiesTested() {
        // Arrange
        SymbolData mockSymbolData = mock(SymbolData.class);

        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 11, 0);

        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString())).thenReturn(Optional.of(mockSymbolData));

        strategyExecutor.update();

        verify(orderHandler).removeExpiredOpenOrders(anyInt());
        verify(candlestickService, times(strategyExecutor.getSymbolList().size())).getEntireDayHistoryData(anyString(), anyString());

        // Verify Strategy.test is called exactly once for each strategy
        for (Strategy strategy :activeStrategies) {
            verify(strategy, times(1)).test(anyList(), anyInt());
        }
    }

    @Test
    public void testUpdate_WithinTradingHours_StrategiesTested_returning_openOrder() {
        // Arrange
        SymbolData mockSymbolData = mock(SymbolData.class);
        OpenOrder openOrder = mock(OpenOrder.class);

        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 11, 0);

        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString())).thenReturn(Optional.of(mockSymbolData));
        when(mockStrategy1.test(anyList(), anyInt())).thenReturn(Optional.of(openOrder));

        // Act
        strategyExecutor.update();

        // Assert
        verify(orderHandler).removeExpiredOpenOrders(anyInt());
        verify(candlestickService, times(strategyExecutor.getSymbolList().size())).getEntireDayHistoryData(anyString(), anyString());

        // Verify Strategy.test is called exactly once for each strategy
        for (Strategy strategy : activeStrategies) {
            verify(strategy, times(1)).test(anyList(), anyInt());
        }
            verify(orderHandler, times(1)).appendOpenOrder(any());
    }

    @Test
    public void testUpdate_WithinTradingHours_StrategiesTested_returning_openOrders() {
        // Arrange
        SymbolData mockSymbolData = mock(SymbolData.class);
        OpenOrder openOrder = mock(OpenOrder.class);

        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 11, 0);

        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString())).thenReturn(Optional.of(mockSymbolData));
        when(mockStrategy1.test(anyList(), anyInt())).thenReturn(Optional.of(openOrder));
        when(mockStrategy2.test(anyList(), anyInt())).thenReturn(Optional.of(openOrder));

        // Act
        strategyExecutor.update();

        // Assert
        verify(orderHandler).removeExpiredOpenOrders(anyInt());
        verify(candlestickService, times(strategyExecutor.getSymbolList().size())).getEntireDayHistoryData(anyString(), anyString());
        // Verify Strategy.test is called exactly once for each strategy
        for (Strategy strategy : activeStrategies) {
            verify(strategy, times(1)).test(anyList(), anyInt());
        }
        verify(orderHandler, times(2)).appendOpenOrder(any());
    }

    @Test
    public void testUpdate_WithinTradingHours_StrategiesTested_verify_order_logging() {
        // Arrange
        SymbolData mockSymbolData = mock(SymbolData.class);
        OpenOrder openOrder = mock(OpenOrder.class);
        ActiveOrder activeOrder = mock(ActiveOrder.class);

        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 11, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString())).thenReturn(Optional.of(mockSymbolData));
        when(mockStrategy1.test(anyList(), anyInt())).thenReturn(Optional.of(openOrder));
        when(mockStrategy2.test(anyList(), anyInt())).thenReturn(Optional.of(openOrder));
        when(orderHandler.getOpenOrders()).thenReturn(List.of(openOrder));
        when(orderHandler.getActiveOrders()).thenReturn(List.of(activeOrder));

        // Act
        strategyExecutor.update();

        // Assert
        verify(orderHandler).removeExpiredOpenOrders(anyInt());
        verify(candlestickService, times(strategyExecutor.getSymbolList().size())).getEntireDayHistoryData(anyString(), anyString());

        // Verify Strategy.test is called exactly once for each strategy
        for (Strategy strategy : activeStrategies) {
            verify(strategy, times(1)).test(anyList(), anyInt());
        }
        verify(orderHandler, times(2)).appendOpenOrder(any());
    }

    @Test
    void testIsWithinTradingHoursDuring() {
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 10, 0); // Time during trading hours
        assertTrue(strategyExecutor.isWithinTradingHours(testTime));
    }

    @Test
    void testIsWithinTradingHoursBefore() {
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 8, 0); // Time before trading hours
        assertFalse(strategyExecutor.isWithinTradingHours(testTime));
    }

    @Test
    void testIsWithinTradingHoursAfter() {
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 18, 0); // Time after trading hours
        assertFalse(strategyExecutor.isWithinTradingHours(testTime));
    }

    @Test
    void testIsWithinTradingHoursAtStart() {
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 9, 15); // Time at the start of trading hours
        assertTrue(strategyExecutor.isWithinTradingHours(testTime));
    }

    @Test
    void testIsWithinTradingHoursAtEnd() {
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 15, 30); // Time at the end of trading hours
        assertTrue(strategyExecutor.isWithinTradingHours(testTime));
    }
}