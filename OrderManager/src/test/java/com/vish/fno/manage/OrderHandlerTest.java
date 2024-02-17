package com.vish.fno.manage;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.helper.EntryVerifier;
import com.vish.fno.manage.helper.StopLossAndTargetHandler;
import com.vish.fno.manage.helper.TimeProvider;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.orderflow.sl.FixedStopLossHandler;
import com.vish.fno.util.orderflow.sl.StopLossHandler;
import com.vish.fno.util.orderflow.target.FixedTargetHandler;
import com.vish.fno.util.orderflow.target.TargetHandler;
import com.zerodhatech.models.Tick;
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
import java.util.List;
import java.util.Optional;

class OrderHandlerTest {

    @Mock private KiteService kiteService;
    @Mock private OrderConfiguration orderConfiguration;
    @Mock private FileUtils fileUtils;
    @Mock private TimeProvider timeProvider;

    private OrderHandler orderHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        final EntryVerifier entryVerifier = spy(new EntryVerifier(orderConfiguration, kiteService, timeProvider));
        final TargetHandler targetHandler = spy(new FixedTargetHandler());
        final StopLossHandler stopLossHandler = spy(new FixedStopLossHandler());
        final StopLossAndTargetHandler stopLossAndTargetHandler = spy(new StopLossAndTargetHandler(timeProvider, kiteService, targetHandler, stopLossHandler));
        orderHandler = new OrderHandler(kiteService, orderConfiguration, fileUtils, timeProvider, entryVerifier, stopLossAndTargetHandler);
    }

    @Test
    void testAppendOpenOrder() {
        //Arrange
        OrderRequest mockOrder = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        //Act
        orderHandler.appendOpenOrder(mockOrder);
        //Assert
        assertEquals(orderHandler.getOrderRequests().size(), 1);
    }

    @Test
    void testAppendOpenOrderIfSameOpenOrderPresent() {
        //Arrange
        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        //Act
        orderHandler.appendOpenOrder(orderRequest1);
        orderHandler.appendOpenOrder(orderRequest2);
        //Assert
        assertEquals(orderHandler.getOrderRequests().size(), 1);
    }

    @Test
    void testAppendOpenOrderForMultipleOpenOrders() {
        //Arrange
        ArgumentCaptor<List<String>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        List<String> expectedFirstInvocationArgument = List.of("TEST_SYMBOL", "TEST_OPTION_SYMBOL");
        List<String> expectedSecondInvocationArgument = List.of("TEST_SYMBOL2", "TEST_OPTION_SYMBOL2");

        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .tag("TestStrategyTag")
                .index("TEST_SYMBOL2")
                .optionSymbol("TEST_OPTION_SYMBOL2")
                .build();

        when(kiteService.getInstrument("TEST_SYMBOL")).thenReturn(1L);
        when(kiteService.getInstrument("TEST_SYMBOL2")).thenReturn(2L);
        when(kiteService.getInstrument("TEST_OPTION_SYMBOL")).thenReturn(3L);
        when(kiteService.getInstrument("TEST_OPTION_SYMBOL2")).thenReturn(4L);

        //Act
        orderHandler.appendOpenOrder(orderRequest1);
        orderHandler.appendOpenOrder(orderRequest2);

        //Assert
        assertEquals(orderHandler.getOrderRequests().size(), 2);
        verify(kiteService, times(2)).appendWebSocketSymbolsList(argumentCaptor.capture(), anyBoolean());

        List<List<String>> allValues = argumentCaptor.getAllValues();
        assertEquals(expectedFirstInvocationArgument, allValues.get(0));
        assertEquals(expectedSecondInvocationArgument, allValues.get(1));
    }

    @Test
    void testAppendOpenOrderIfSameActiveOrderPresent() {
        //Arrange
        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest1, 1, 10);
        orderHandler.getActiveOrders().add(activeOrder);

        //Act
        orderHandler.appendOpenOrder(orderRequest2);
        //Assert
        assertEquals(orderHandler.getOrderRequests().size(), 0);
    }

    @Test
    void testRemoveExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .expirationTimestamp(24)
                .build();
        orderHandler.getOrderRequests().add(orderRequest1);

        //Act
        orderHandler.removeExpiredOpenOrders(timestamp);

        //Assert
        assertEquals(orderHandler.getOrderRequests().size(), 0);
    }

    @Test
    void testNotRemoveNonExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .expirationTimestamp(26)
                .build();
        orderHandler.getOrderRequests().add(orderRequest1);

        //Act
        orderHandler.removeExpiredOpenOrders(timestamp);

        //Assert
        assertEquals(orderHandler.getOrderRequests().size(), 1);
    }

    @Test
    void testRemoveOnlyExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .expirationTimestamp(26)
                .build();
        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .expirationTimestamp(24)
                .build();
        orderHandler.getOrderRequests().add(orderRequest1);
        orderHandler.getOrderRequests().add(orderRequest2);

        //Act
        orderHandler.removeExpiredOpenOrders(timestamp);

        //Assert
        assertEquals(orderHandler.getOrderRequests().size(), 1);
    }

    @Test
    void testHandleTicksNoActiveOrder() {
        // Arrange
        ArrayList<Tick> ticks = new ArrayList<>();
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100);
        ticks.add(tick);

        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");

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
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
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
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .target(101)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
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
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(false).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
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
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(101)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(false).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
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
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
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
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .target(101)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1);
        orderHandler.getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
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
