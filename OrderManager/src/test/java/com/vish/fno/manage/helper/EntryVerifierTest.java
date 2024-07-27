package com.vish.fno.manage.helper;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.ActiveOrderFactory;
import com.vish.fno.model.order.orderrequest.IndexOrderRequest;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.TimeUtils;
import com.vish.fno.util.helper.TimeProvider;
import com.zerodhatech.models.Tick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;
import java.util.Optional;

import static com.vish.fno.util.Constants.NIFTY_50;
import static com.vish.fno.util.Constants.NIFTY_BANK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class EntryVerifierTest {

    private static final String ORDER_EXECUTED = "orderExecuted";
    @Mock private OrderConfiguration orderConfiguration;
    @Mock private KiteService kiteService;
    @Spy private TimeProvider timeProvider;
    private EntryVerifier entryVerifier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        String[] additionalSymbols = new String[] {
                NIFTY_50, NIFTY_BANK, "FINNIFTY", "NIFTY FIN SERVICE", "BANKNIFTY", "NIFTY"
        };
        when(orderConfiguration.getSymbolsPath()).thenReturn("ind_nifty100list.csv");
        when(orderConfiguration.getAdditionalSymbols()).thenReturn(additionalSymbols);
        when(orderConfiguration.getAvailableCash()).thenReturn(25000d);

        entryVerifier = new EntryVerifier(orderConfiguration, kiteService, timeProvider);
    }

    @Test
    void testIsPlaceOrderWhenSymbolIsNotInAllowedSymbolsForBuy() {
        //Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());

        //Act
        boolean isPlaceOrder = entryVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenOrderAmountIsGreaterThanBuy() {
        //Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        activeOrder.setBuyOptionPrice(2600);
        //Act
        boolean isPlaceOrder = entryVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenOrderAmountIsMoreThanAvailableCashForBuy() {
        //Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());

        //Act
        boolean isPlaceOrder = entryVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenOrderAmountIsInLimitThanBuy() {
        //Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks("","", true, true))
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 102, 1, TimeUtils.getTodayDate());
        activeOrder.setBuyOptionPrice(200);
        //Act
        boolean isPlaceOrder = entryVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertTrue(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenExtraDataIsNotAvailableForSell() {
        //Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        activeOrder.setBuyOptionPrice(200);
        //Act
        boolean isPlaceOrder = entryVerifier.isPlaceOrder(activeOrder, false);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenExtraDataNotSetForSell() {
        //Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        activeOrder.setBuyOptionPrice(200);
        activeOrder.appendExtraData("test", "");

        //Act
        boolean isPlaceOrder = entryVerifier.isPlaceOrder(activeOrder, false);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenExtraDataSetForSell() {
        //Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks("","", true, true))
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        activeOrder.setBuyOptionPrice(200);
        activeOrder.appendExtraData(ORDER_EXECUTED, "true");

        //Act
        boolean isPlaceOrder = entryVerifier.isPlaceOrder(activeOrder, false);
        //Assert
        assertTrue(isPlaceOrder);
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrdersAndActiveOrdersAreEmpty() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        //Act
        Optional<OrderRequest> openOrderEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(), List.of());

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderIsEmpty() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();

        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        //Act
        Optional<OrderRequest> openOrderEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbols() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        OrderRequest orderRequestEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(newOrderRequest), List.of(activeOrder)).get();

        //Assert
        assertEquals(orderRequestEntry, newOrderRequest);
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToSameSymbols() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        //Act
        Optional<OrderRequest> openOrderEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(newOrderRequest), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbolsForPut() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(99);

        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        OrderRequest orderRequestEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(newOrderRequest), List.of(activeOrder)).get();

        //Assert
        assertEquals(orderRequestEntry, newOrderRequest);
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbolsWithoutBuyTriggeredForPut() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(121);

        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(120)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        Optional<OrderRequest> openOrderEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(newOrderRequest), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbolsWithoutBuyTriggered() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        OrderRequest orderRequestEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(newOrderRequest), List.of(activeOrder)).get();

        //Assert
        assertEquals(orderRequestEntry, newOrderRequest);
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToSameSymbolsForPut() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(120)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        //Act
        Optional<OrderRequest> openOrderEntry = entryVerifier.checkEntryInOpenOrders(tick, List.of(newOrderRequest), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBelowStopLoss() {
        // Arrange
        double ltp = 89;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertFalse(moveHappened);

    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBetweenStopLossAndBuyAt() {
        // Arrange
        double ltp = 95;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertFalse(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustAboveBuyAt() {
        // Arrange
        double ltp = 100.1;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertFalse(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustBelowTarget() {
        // Arrange
        double ltp = 114;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertTrue(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsAboveTarget() {
        // Arrange
        double ltp = 116;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertTrue(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBelowStoplossForPut() {
        // Arrange
        double ltp = 111;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertFalse(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBetweenStopLossAndBuyAtForPut() {
        // Arrange
        double ltp = 105;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertFalse(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustAboveBuyAtForPut() {
        // Arrange
        double ltp = 99.9;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertFalse(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustBelowTargetForPut() {
        // Arrange
        double ltp = 86;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertTrue(moveHappened);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsAboveTargetForPut() {
        // Arrange
        double ltp = 84;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertTrue(moveHappened);
    }

    @Test
    void testIsNotInActiveOrdersWhenNoActiveOrderIsPresent() {
        // Arrange
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();

        //Act
        boolean isNotInActiveOrder = entryVerifier.isNotInActiveOrders(List.of(), orderRequest);

        // Assert
        assertTrue(isNotInActiveOrder);
    }

    @Test
    void testIsNotInActiveOrdersWhenDiffActiveOrderIsPresent() {
        // Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());

        //Act
        boolean isNotInActiveOrder = entryVerifier.isNotInActiveOrders(List.of(activeOrder), newOrderRequest);

        // Assert
        assertTrue(isNotInActiveOrder);
    }

    @Test
    void testIsNotInActiveOrdersWhenDiffActiveOrderTagIsPresent() {
        // Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG_2", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());

        //Act
        boolean isNotInActiveOrder = entryVerifier.isNotInActiveOrders(List.of(activeOrder), newOrderRequest);

        // Assert
        assertTrue(isNotInActiveOrder);
    }

    @Test
    void testIsNotInActiveOrdersWheSameOrderAndSymbolIsPresent() {
        // Arrange
        OrderRequest existingOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OrderRequest newOrderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(145)
                .target(110)
                .quantity(50)
                .build();

        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOrderRequest, 0d, 1, TimeUtils.getTodayDate());

        //Act
        boolean isNotInActiveOrder = entryVerifier.isNotInActiveOrders(List.of(activeOrder), newOrderRequest);

        // Assert
        assertTrue(isNotInActiveOrder);
    }



    @Test
    void testHasMoveAlreadyHappenedCase1() {
        // Arrange
        double ltp = 1419.9;

        OrderRequest orderRequest = IndexOrderRequest.builder("HDFCBANK", "HDFCBANK24MAR1410CE", new StrategyTasks())
                .callOrder(true)
                .target(1421.95)
                .buyThreshold(1419.8)
                .stopLoss(1418.35)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(orderRequest, ltp, 1, TimeUtils.getTodayDate());
        activeOrder.setBuyOptionPrice(1);

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertFalse(moveHappened);

    }

    @Test
    void testHasMoveAlreadyHappenedCase2() {
        // Arrange
        double ltp = 1421.8;

        OrderRequest orderRequest = IndexOrderRequest.builder("HDFCBANK", "HDFCBANK24MAR1410CE", new StrategyTasks())
                .callOrder(true)
                .target(1421.95)
                .buyThreshold(1419.8)
                .stopLoss(1418.35)
                .build();

        //Act
        boolean moveHappened = entryVerifier.hasMoveAlreadyHappened(ltp, orderRequest);

        // Assert
        assertTrue(moveHappened);
    }
}