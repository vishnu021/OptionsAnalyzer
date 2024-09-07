package com.vish.fno.manage.orderflow;

import com.vish.fno.manage.helper.DataCacheImpl;
import com.vish.fno.model.cache.OrderCache;
import com.vish.fno.manage.service.CalendarService;
import com.vish.fno.model.strategy.MinuteStrategy;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.manage.service.CandlestickService;
import com.vish.fno.model.SymbolData;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.reader.service.KiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class StrategyExecutorTest {

    public static final String TEST_STRATEGY = "test_strategy";
    @Mock private KiteService kiteService;
    @Mock private CandlestickService candlestickService;
    @Mock private OrderHandler orderHandler;
    @Mock private MinuteStrategy mockStrategy1;
    @Mock private MinuteStrategy mockStrategy2;
    @Mock private MinuteStrategy mockStrategy3;
    private TimeProvider timeProvider;
    List<MinuteStrategy> indexStratgies;
    List<MinuteStrategy> optionStrategies;
    OrderCache orderCache;
    @InjectMocks
    private StrategyExecutor strategyExecutor;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderCache = spy(new OrderCache(0L));
        indexStratgies = List.of(mockStrategy1, mockStrategy2);
        optionStrategies = List.of(mockStrategy3);

        when(mockStrategy1.getTask()).thenReturn(new StrategyTasks(TEST_STRATEGY, "BANK NIFTY", true, true));
        when(mockStrategy2.getTask()).thenReturn(new StrategyTasks(TEST_STRATEGY, "NIFTY 50", false, false));
        when(mockStrategy3.getTask()).thenReturn(new StrategyTasks(TEST_STRATEGY, "NIFTY 50", false, false));
        CalendarService calendarService = mock(CalendarService.class);
        timeProvider = spy(TimeProvider.class);
        when(timeProvider.currentTimeStampIndex()).thenReturn(1);
        when(orderHandler.getOrderCache()).thenReturn(orderCache);

        DataCacheImpl dataCache = spy(new DataCacheImpl(candlestickService, calendarService, timeProvider));
        strategyExecutor = new StrategyExecutor(kiteService,
                orderHandler,
                dataCache,
                orderCache,
                indexStratgies,
                optionStrategies,
                timeProvider);
    }

    @Test
    public void testUpdate_outSideTradingHours() {
        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 8, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);

        strategyExecutor.update();

        verify(orderCache, never()).removeExpiredOpenOrders(anyInt());
    }

    @Test
    public void testUpdate_kite_Not_Initialised() {
        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 10, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(false);

        strategyExecutor.update();

        verify(orderCache, never()).removeExpiredOpenOrders(anyInt());
    }

    @Test
    public void testUpdate_WithinTradingHours_without_candleCache() {
        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 10, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);

        strategyExecutor.update();

        for (MinuteStrategy strategy : indexStratgies) {
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

        // Verify Strategy.test is called exactly once for each strategy
        for (MinuteStrategy strategy : indexStratgies) {
            verify(strategy, times(1)).test(anyList(), anyInt());
        }
    }

    @Test
    public void testUpdate_WithinTradingHours_StrategiesTested_returning_openOrder() {
        // Arrange
        SymbolData mockSymbolData = mock(SymbolData.class);
        OrderRequest orderRequest = mock(OrderRequest.class);

        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 11, 0);

        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString())).thenReturn(Optional.of(mockSymbolData));
        when(mockStrategy1.test(anyList(), anyInt())).thenReturn(Optional.of(orderRequest));

        // Act
        strategyExecutor.update();

        // Assert
        for (MinuteStrategy strategy : indexStratgies) {
            verify(strategy, times(1)).test(anyList(), anyInt());
        }
        verify(orderHandler, times(1)).appendOpenOrder(any());
    }

    @Test
    public void testUpdate_WithinTradingHours_StrategiesTested_returning_openOrders() {
        // Arrange
        SymbolData mockSymbolData = mock(SymbolData.class);
        OrderRequest orderRequest = mock(OrderRequest.class);

        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 11, 0);

        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString())).thenReturn(Optional.of(mockSymbolData));
        when(mockStrategy1.test(anyList(), anyInt())).thenReturn(Optional.of(orderRequest));
        when(mockStrategy2.test(anyList(), anyInt())).thenReturn(Optional.of(orderRequest));

        // Act
        strategyExecutor.update();

        // Assert
        for (MinuteStrategy strategy : indexStratgies) {
            verify(strategy, times(1)).test(anyList(), anyInt());
        }
        verify(orderHandler, times(2)).appendOpenOrder(any());
    }

    @Test
    public void testUpdate_WithinTradingHours_StrategiesTested_verify_order_logging() {
        // Arrange
        SymbolData mockSymbolData = mock(SymbolData.class);
        OrderRequest orderRequest = mock(OrderRequest.class);

        LocalDateTime mockTime = LocalDateTime.of(2024, 1, 24, 11, 0);
        when(timeProvider.now()).thenReturn(mockTime);
        when(kiteService.isInitialised()).thenReturn(true);
        when(candlestickService.getEntireDayHistoryData(anyString(), anyString())).thenReturn(Optional.of(mockSymbolData));
        when(mockStrategy1.test(anyList(), anyInt())).thenReturn(Optional.of(orderRequest));
        when(mockStrategy2.test(anyList(), anyInt())).thenReturn(Optional.of(orderRequest));

        // Act
        strategyExecutor.update();

        // Assert
        for (MinuteStrategy strategy : indexStratgies) {
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
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 9, 16); // Time at the start of trading hours
        assertTrue(strategyExecutor.isWithinTradingHours(testTime));
    }

    @Test
    void testIsWithinTradingHoursAtEnd() {
        LocalDateTime testTime = LocalDateTime.of(2024, 1, 1, 15, 30); // Time at the end of trading hours
        assertTrue(strategyExecutor.isWithinTradingHours(testTime));
    }
}