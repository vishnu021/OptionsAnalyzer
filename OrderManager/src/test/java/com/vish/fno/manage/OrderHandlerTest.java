package com.vish.fno.manage;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.reader.helper.InstrumentCache;
import com.vish.fno.manage.helper.OpenOrderVerifier;
import com.vish.fno.manage.helper.StopLossAndTargetHandler;
import com.vish.fno.manage.helper.TimeProvider;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
class OrderHandlerTest {

    @Mock private KiteService kiteService;
    @Mock private InstrumentCache instrumentCache;
    @Mock private OrderConfiguration orderConfiguration;
    @Mock private FileUtils fileUtils;
    @Mock private TimeProvider timeProvider;

    private OrderHandler orderHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        final OpenOrderVerifier openOrderVerifier = spy(new OpenOrderVerifier(orderConfiguration, instrumentCache));
        final StopLossAndTargetHandler stopLossAndTargetHandler = spy(new StopLossAndTargetHandler(timeProvider, instrumentCache));
        orderHandler = new OrderHandler(kiteService, instrumentCache, orderConfiguration, fileUtils, timeProvider, openOrderVerifier, stopLossAndTargetHandler);
    }

    @Test
    void testAppendOpenOrder() {
        //Arrange
        OpenOrder mockOrder = mock(OpenOrder.class);
        //Act
        orderHandler.appendOpenOrder(mockOrder);
        //Assert
        assertEquals(orderHandler.getOpenOrders().size(), 1);
    }

    @Test
    void testAppendOpenOrderIfSameOpenOrderPresent() {
        //Arrange
        OpenOrder openOrder1 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        OpenOrder openOrder2 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        //Act
        orderHandler.appendOpenOrder(openOrder1);
        orderHandler.appendOpenOrder(openOrder2);
        //Assert
        assertEquals(orderHandler.getOpenOrders().size(), 1);
    }

    @Test
    void testAppendOpenOrderForMultipleOpenOrders() {
        //Arrange
        ArgumentCaptor<ArrayList<Long>> argumentCaptor = ArgumentCaptor.forClass(ArrayList.class);
        ArrayList<Long> expectedFirstInvocationArgument = new ArrayList<>(Arrays.asList(1L, 3L));
        ArrayList<Long> expectedSecondInvocationArgument = new ArrayList<>(Arrays.asList(2L, 4L));

        OpenOrder openOrder1 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        OpenOrder openOrder2 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL2")
                .optionSymbol("TEST_OPTION_SYMBOL2")
                .build();

        when(instrumentCache.getInstrument("TEST_SYMBOL")).thenReturn(1L);
        when(instrumentCache.getInstrument("TEST_SYMBOL2")).thenReturn(2L);
        when(instrumentCache.getInstrument("TEST_OPTION_SYMBOL")).thenReturn(3L);
        when(instrumentCache.getInstrument("TEST_OPTION_SYMBOL2")).thenReturn(4L);

        //Act
        orderHandler.appendOpenOrder(openOrder1);
        orderHandler.appendOpenOrder(openOrder2);

        //Assert
        assertEquals(orderHandler.getOpenOrders().size(), 2);
        verify(kiteService, times(2)).appendWebSocketTokensList(argumentCaptor.capture());

        List<ArrayList<Long>> allValues = argumentCaptor.getAllValues();
        assertEquals(expectedFirstInvocationArgument, allValues.get(0));
        assertEquals(expectedSecondInvocationArgument, allValues.get(1));
    }

    @Test
    void testAppendOpenOrderIfSameActiveOrderPresent() {
        //Arrange
        OpenOrder openOrder1 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        OpenOrder openOrder2 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder1, 1, 10);
        orderHandler.getActiveOrders().add(activeOrder);

        //Act
        orderHandler.appendOpenOrder(openOrder2);
        //Assert
        assertEquals(orderHandler.getOpenOrders().size(), 0);
    }

    @Test
    void testRemoveExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OpenOrder openOrder1 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .expirationTimestamp(24)
                .build();
        orderHandler.getOpenOrders().add(openOrder1);

        //Act
        orderHandler.removeExpiredOpenOrders(timestamp);

        //Assert
        assertEquals(orderHandler.getOpenOrders().size(), 0);
    }

    @Test
    void testNotRemoveNonExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OpenOrder openOrder1 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .expirationTimestamp(26)
                .build();
        orderHandler.getOpenOrders().add(openOrder1);

        //Act
        orderHandler.removeExpiredOpenOrders(timestamp);

        //Assert
        assertEquals(orderHandler.getOpenOrders().size(), 1);
    }

    @Test
    void testRemoveOnlyExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OpenOrder openOrder1 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .expirationTimestamp(26)
                .build();
        OpenOrder openOrder2 = OpenIndexOrder.builder()
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL")
                .expirationTimestamp(24)
                .build();
        orderHandler.getOpenOrders().add(openOrder1);
        orderHandler.getOpenOrders().add(openOrder2);

        //Act
        orderHandler.removeExpiredOpenOrders(timestamp);

        //Assert
        assertEquals(orderHandler.getOpenOrders().size(), 1);
    }

    @Test
    void testHandleTicksNoActiveOrder() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        verify(kiteService, never()).sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean());
    }

    @Test
    void testHandleTicks_call_target_achieved() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        //active order
        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean())).thenReturn(true);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        assertTrue(orderHandler.getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTicks_put_target_achieved() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        //active order
        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .target(101)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean())).thenReturn(true);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        assertTrue(orderHandler.getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTicks_call_target_achieved_failed_to_sell() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        //active order
        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean())).thenReturn(false);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        assertFalse(orderHandler.getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTicks_put_target_achieved_failed_to_sell() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        //active order
        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(101)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean())).thenReturn(false);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        assertFalse(orderHandler.getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTicks_call_target_achieved_exception_while_Selling() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        //active order
        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean())).thenReturn(true);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);
        doThrow(new RuntimeException()).when(fileUtils).logCompletedOrder(any(ActiveOrder.class));

        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        assertFalse(orderHandler.getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTicks_put_target_achieved_exception_while_Selling() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        //active order
        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .target(101)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean())).thenReturn(true);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);
        doThrow(new RuntimeException()).when(fileUtils).logCompletedOrder(any(ActiveOrder.class));

        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        assertFalse(orderHandler.getActiveOrders().isEmpty());
    }

    @Test
    void test_Handle_Ticks_throws_error() {
        // Arrange
        Tick mockTick = mock(Tick.class);
        ArrayList<Tick> ticks = new ArrayList<>();
        ticks.add(mockTick);
        when(mockTick.getLastTradedPrice()).thenThrow(new RuntimeException());
        // Act
        orderHandler.handleTicks(ticks);
        // Assert
        verify(fileUtils, never()).appendTickToFile(anyString(), any());

    }
}
