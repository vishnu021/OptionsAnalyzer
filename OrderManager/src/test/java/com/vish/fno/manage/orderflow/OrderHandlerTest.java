package com.vish.fno.manage.orderflow;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.helper.IndexEntryVerifier;
import com.vish.fno.model.helper.OrderCache;
import com.vish.fno.model.Ticker;
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
    @Mock private IndexOrderFlowHandler indexOrderFlowHandler;

    private OrderHandler orderHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderHandler = new OrderHandler(kiteService, orderConfiguration, fileUtils, timeProvider, targetAndStopLossStrategy, new OrderCache(0L));
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
                .orderFlowHandler(indexOrderFlowHandler)
                .build();
        Ticker ticker = Ticker.builder().lastTradedPrice(101).build();
        orderHandler.getOrderCache().appendToLatestTick("TEST_SYMBOL", ticker);
        when(kiteService.getITMStock("TEST_SYMBOL", 100, true)).thenReturn("TEST_OPTION_SYMBOL");
        //Act
        orderHandler.appendOpenOrder(mockOrder);
        //Assert
        assertEquals(orderHandler.getOrderCache().getOrderRequests().size(), 1);
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
                .orderFlowHandler(indexOrderFlowHandler)
                .build();

        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .target(200)
                .buyThreshold(100)
                .stopLoss(60)
                .callOrder(true)
                .orderFlowHandler(indexOrderFlowHandler)
                .build();
        Ticker ticker = Ticker.builder().lastTradedPrice(101).build();
        orderHandler.getOrderCache().appendToLatestTick("TEST_SYMBOL", ticker);
        when(kiteService.getITMStock("TEST_SYMBOL", 100, true)).thenReturn("TEST_OPTION_SYMBOL");
        //Act
        orderHandler.appendOpenOrder(orderRequest1);
        orderHandler.appendOpenOrder(orderRequest2);
        //Assert
        assertEquals(orderHandler.getOrderCache().getOrderRequests().size(), 1);
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
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();
        OrderRequest orderRequest2 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL2", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL2")
                .target(200)
                .buyThreshold(100)
                .stopLoss(60)
                .callOrder(true)
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();

        Ticker ticker = Ticker.builder().lastTradedPrice(101).build();
        Ticker ticker2 = Ticker.builder().lastTradedPrice(101).build();

        orderHandler.getOrderCache().appendToLatestTick("TEST_SYMBOL", ticker);
        orderHandler.getOrderCache().appendToLatestTick("TEST_SYMBOL2", ticker2);

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
        assertEquals(orderHandler.getOrderCache().getOrderRequests().size(), 2);
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
        orderHandler.getOrderCache().getActiveOrders().add(activeOrder);

        //Act
        orderHandler.appendOpenOrder(orderRequest2);
        //Assert
        assertEquals(orderHandler.getOrderCache().getOrderRequests().size(), 0);
    }

    @Test
    void testRemoveExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .expirationTimestamp(24)
                .build();
        orderHandler.getOrderCache().getOrderRequests().add(orderRequest1);
        when(timeProvider.currentTimeStampIndex()).thenReturn(timestamp);

        //Act
        orderHandler.removeExpiredOpenOrders();

        //Assert
        assertEquals(orderHandler.getOrderCache().getOrderRequests().size(), 0);
    }

    @Test
    void testNotRemoveNonExpiredOrders() {
        //Arrange
        int timestamp = 25;
        OrderRequest orderRequest1 = IndexOrderRequest.builder("TestStrategyTag", "TEST_SYMBOL", new StrategyTasks())
                .expirationTimestamp(26)
                .build();
        orderHandler.getOrderCache().getOrderRequests().add(orderRequest1);
        when(timeProvider.currentTimeStampIndex()).thenReturn(timestamp);

        //Act
        orderHandler.removeExpiredOpenOrders();

        //Assert
        assertEquals(orderHandler.getOrderCache().getOrderRequests().size(), 1);
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
        orderHandler.getOrderCache().getOrderRequests().add(orderRequest1);
        orderHandler.getOrderCache().getOrderRequests().add(orderRequest2);
        when(timeProvider.currentTimeStampIndex()).thenReturn(timestamp);

        //Act
        orderHandler.removeExpiredOpenOrders();

        //Assert
        assertEquals(orderHandler.getOrderCache().getOrderRequests().size(), 1);
    }

    @Test
    void testHandleTickersNoActiveOrder() {
        // Arrange
        ArrayList<Ticker> Tickers = new ArrayList<>();
        Ticker ticker = Ticker.builder().instrumentToken(1L).lastTradedPrice(100).build();
        Tickers.add(ticker);

        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        orderHandler.handleTicks(Tickers);
        // Assert
        verify(kiteService, never()).sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean());
    }

    @Test
    void testHandleTickers_call_target_achieved() {
        // Arrange
        ArrayList<Ticker> tickers = new ArrayList<>();
        Ticker ticker = Ticker.builder().instrumentToken(1L).lastTradedPrice(100).build();
        tickers.add(ticker);

        //active order
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
        orderHandler.getOrderCache().getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(tickers);
        // Assert
        assertTrue(orderHandler.getOrderCache().getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTickers_put_target_achieved() {
        // Arrange
        ArrayList<Ticker> tickers = new ArrayList<>();
        Ticker ticker = Ticker.builder().instrumentToken(1L).lastTradedPrice(100).build();
        tickers.add(ticker);

        //active order
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .target(101)
                .quantity(10)
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
        orderHandler.getOrderCache().getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(tickers);
        // Assert
        assertTrue(orderHandler.getOrderCache().getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTickers_call_target_achieved_failed_to_sell() {
        // Arrange
        ArrayList<Ticker> tickers = new ArrayList<>();
        Ticker ticker = Ticker.builder().instrumentToken(1L).lastTradedPrice(100).build();
        tickers.add(ticker);

        //active order
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .orderFlowHandler(indexOrderFlowHandler)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
        orderHandler.getOrderCache().getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(false).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(tickers);
        // Assert
        assertFalse(orderHandler.getOrderCache().getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTickers_put_target_achieved_failed_to_sell() {
        // Arrange
        ArrayList<Ticker> tickers = new ArrayList<>();
        Ticker ticker = Ticker.builder().instrumentToken(1L).lastTradedPrice(100).build();
        tickers.add(ticker);

        //active order
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(101)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
        orderHandler.getOrderCache().getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(false).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);

        // Act
        orderHandler.handleTicks(tickers);
        // Assert
        assertFalse(orderHandler.getOrderCache().getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTickers_call_target_achieved_exception_while_Selling() {
        // Arrange
        ArrayList<Ticker> tickers = new ArrayList<>();
        Ticker ticker = Ticker.builder().instrumentToken(1L).lastTradedPrice(100).build();
        tickers.add(ticker);

        //active order
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .target(99)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
        orderHandler.getOrderCache().getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);
        doThrow(new RuntimeException()).when(fileUtils).logCompletedOrder(any(ActiveOrder.class));

        // Act
        orderHandler.handleTicks(tickers);
        // Assert
        assertFalse(orderHandler.getOrderCache().getActiveOrders().isEmpty());
    }

    @Test
    void testHandleTickers_put_target_achieved_exception_while_Selling() {
        // Arrange
        ArrayList<Ticker> tickers = new ArrayList<>();
        Ticker ticker = Ticker.builder().instrumentToken(1L).lastTradedPrice(100).build();
        tickers.add(ticker);

        //active order
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .target(101)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, 1d, 1, TimeUtils.getTodayDate());
        orderHandler.getOrderCache().getActiveOrders().add(activeOrder);

        when(kiteService.sellOrder(anyString(), anyDouble(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(Optional.of(KiteOpenOrder.builder().isOrderPlaced(true).build()));
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");
        when(timeProvider.currentTimeStampIndex()).thenReturn(300);
        doThrow(new RuntimeException()).when(fileUtils).logCompletedOrder(any(ActiveOrder.class));

        // Act
        orderHandler.handleTicks(tickers);
        // Assert
        assertFalse(orderHandler.getOrderCache().getActiveOrders().isEmpty());
    }

    @Test
    void test_Handle_Tickers_throws_error() {
        // Arrange
        Ticker mockTicker = mock(Ticker.class);
        ArrayList<Ticker> tickers = new ArrayList<>();
        tickers.add(mockTicker);
        when(mockTicker.getLastTradedPrice()).thenThrow(new RuntimeException());
        // Act
        orderHandler.handleTicks(tickers);
        // Assert
        verify(fileUtils, never()).appendTickToFile(anyString(), any());

    }

    @Test
    void testGetActiveOrderToSellForEmptyActiveOrders() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(89).build();

        // Act
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 ,List.of());

        // Assert
        verify(targetAndStopLossStrategy, never()).isTargetAchieved(any(), anyDouble());
        verify(targetAndStopLossStrategy, never()).isStopLossHit(any(), anyDouble());
    }

    @Test
    void testGetActiveOrderToSellForActiveOrdersOfDifferentTicker() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(89).build();
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
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 , activeOrders);

        // Assert
        verify(kiteService, never()).sellOrder(any(), anyDouble(), anyInt(), any(), anyBoolean());
    }

    @Test
    void testGetActiveOrderToSellForTargetHitForCall() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(106).build();

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .stopLoss(90)
                .target(105)
                .quantity(1)
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 , activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                ticker.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForCall() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(89).build();

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .quantity(1)
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 ,activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                ticker.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }

    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForCall() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(91).build();

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .orderFlowHandler(indexOrderFlowHandler)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 ,activeOrders);

        // Assert
        verify(kiteService, never()).sellOrder(any(), anyDouble(), anyInt(), any(), anyBoolean());
    }

    @Test
    void testGetActiveOrderToSellForTargetHitForPut() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(89).build();

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .stopLoss(115)
                .target(90)
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 , activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                ticker.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForPut() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(112).build();

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .target(85)
                .stopLoss(110)
                .orderFlowHandler(new IndexOrderFlowHandler(kiteService, timeProvider, orderHandler, fileUtils))
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 , activeOrders);

        // Assert
        verify(kiteService, times(1)).sellOrder("TEST_OPTION_SYMBOL",
                ticker.getLastTradedPrice(), orderRequest.getQuantity(), orderRequest.getTag(), false);
    }


    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForPut() {
        // Arrange
        String TickerSymbol = "TEST_SYMBOL";
        Ticker ticker = Ticker.builder().lastTradedPrice(91).build();

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .target(90)
                .stopLoss(105)
                .orderFlowHandler(indexOrderFlowHandler)
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1, TimeUtils.getTodayDate()));

        // Act
        orderHandler.getActiveOrderToSell(TickerSymbol, ticker, 115 ,activeOrders);

        // Assert
        verify(kiteService, never()).sellOrder(any(), anyDouble(), anyInt(), any(), anyBoolean());
    }
}
