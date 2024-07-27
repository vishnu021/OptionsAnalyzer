package com.vish.fno.manage.orderflow;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.helper.EntryVerifier;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.IndexOrderRequest;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.orderflow.FixedTargetAndStopLossStrategy;
import com.zerodhatech.models.Tick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class OrderHandlerTest {

    @Spy FixedTargetAndStopLossStrategy targetAndStopLossStrategy;
    @Mock private KiteService kiteService;
    @Mock private OrderConfiguration orderConfiguration;
    @Mock private FileUtils fileUtils;
    @Mock private TimeProvider timeProvider;

    private OrderHandler orderHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        final EntryVerifier entryVerifier = spy(new EntryVerifier(orderConfiguration, kiteService, timeProvider));
        orderHandler = new OrderHandler(kiteService, orderConfiguration, fileUtils, timeProvider, entryVerifier, targetAndStopLossStrategy);
    }

    @Test
    void testAppendOpenOrder() {
        //Arrange
        OrderRequest mockOrder = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .target(200)
                .buyThreshold(100)
                .stopLoss(60)
                .callOrder(true)
                .build();
        Tick tick = new Tick();
        tick.setLastTradedPrice(101);
        orderHandler.appendToLatestTick("TEST_SYMBOL", tick);
        when(kiteService.getITMStock("TEST_SYMBOL", 100, true)).thenReturn("TEST_OPTION_SYMBOL");
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
                .target(200)
                .buyThreshold(100)
                .stopLoss(60)
                .callOrder(true)
                .build();

        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .target(200)
                .buyThreshold(100)
                .stopLoss(60)
                .callOrder(true)
                .build();
        Tick tick = new Tick();
        tick.setLastTradedPrice(101);
        orderHandler.appendToLatestTick("TEST_SYMBOL", tick);
        when(kiteService.getITMStock("TEST_SYMBOL", 100, true)).thenReturn("TEST_OPTION_SYMBOL");
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
                .target(200)
                .buyThreshold(100)
                .stopLoss(60)
                .callOrder(true)
                .build();
        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL2", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL2")
                .target(200)
                .buyThreshold(100)
                .stopLoss(60)
                .callOrder(true)
                .build();

        Tick tick = new Tick();
        tick.setLastTradedPrice(101);
        Tick tick2 = new Tick();
        tick2.setLastTradedPrice(101);
        orderHandler.appendToLatestTick("TEST_SYMBOL", tick);
        orderHandler.appendToLatestTick("TEST_SYMBOL2", tick2);

        when(kiteService.getInstrument("TEST_SYMBOL")).thenReturn(1L);
        when(kiteService.getInstrument("TEST_SYMBOL2")).thenReturn(2L);
        when(kiteService.getInstrument("TEST_OPTION_SYMBOL")).thenReturn(3L);
        when(kiteService.getInstrument("TEST_OPTION_SYMBOL2")).thenReturn(4L);
        when(kiteService.getITMStock("TEST_SYMBOL", 100, true)).thenReturn("TEST_OPTION_SYMBOL");
        when(kiteService.getITMStock("TEST_SYMBOL2", 100, true)).thenReturn("TEST_OPTION_SYMBOL2");

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
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest1, 1, 10, TimeUtils.getTodayDate());
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
        when(timeProvider.currentTimeStampIndex()).thenReturn(timestamp);

        //Act
        orderHandler.removeExpiredOpenOrders();

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
        when(timeProvider.currentTimeStampIndex()).thenReturn(timestamp);

        //Act
        orderHandler.removeExpiredOpenOrders();

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
        when(timeProvider.currentTimeStampIndex()).thenReturn(timestamp);

        //Act
        orderHandler.removeExpiredOpenOrders();

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
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
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
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
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
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
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
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
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
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
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
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
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

    @Test
    void testGetActiveOrderToSellForEmptyActiveOrders() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(89);

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 ,List.of());

        // Assert
        verify(targetAndStopLossStrategy, never()).isTargetAchieved(any(), anyDouble());
        verify(targetAndStopLossStrategy, never()).isStopLossHit(any(), anyDouble());
    }

    @Test
    void testGetActiveOrderToSellForActiveOrdersOfDifferentTick() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(89);
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .quantity(1)
                .build();
        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 , activeOrders);

        // Assert
        verify(kiteService, never()).sellOrder(any(), anyDouble(), anyInt(), any(), anyBoolean());
    }

    @Test
    void testGetActiveOrderToSellForTargetHitForCall() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(106);

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .stopLoss(90)
                .target(105)
                .quantity(1)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 , activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                tick.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForCall() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(89);

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .quantity(1)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 ,activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                tick.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }

    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForCall() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(91);

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 ,activeOrders);

        // Assert
        verify(kiteService, never()).sellOrder(any(), anyDouble(), anyInt(), any(), anyBoolean());
    }

    @Test
    void testGetActiveOrderToSellForTargetHitForPut() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(89);

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .stopLoss(115)
                .target(90)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 , activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                tick.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForPut() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(112);

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .target(85)
                .stopLoss(110)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 , activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                tick.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }


    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForPut() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        Tick tick = new Tick();
        tick.setLastTradedPrice(91);

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .target(90)
                .stopLoss(105)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(tickSymbol, tick, 115 ,activeOrders);

        // Assert
        verify(kiteService, never()).sellOrder(any(), anyDouble(), anyInt(), any(), anyBoolean());
    }
}
