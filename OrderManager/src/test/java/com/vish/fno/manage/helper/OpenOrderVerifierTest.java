package com.vish.fno.manage.helper;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.ActiveOrderFactory;
import com.vish.fno.model.order.OpenIndexOrder;
import com.vish.fno.model.order.OpenOrder;
import com.vish.fno.reader.service.KiteService;
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

class OpenOrderVerifierTest {

    private static final String ORDER_EXECUTED = "orderExecuted";
    @Mock private OrderConfiguration orderConfiguration;
    @Mock private KiteService kiteService;
    @Spy private TimeProvider timeProvider;
    private OpenOrderVerifier openOrderVerifier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        String[] additionalSymbols = new String[] {
                NIFTY_50, NIFTY_BANK, "FINNIFTY", "NIFTY FIN SERVICE", "BANKNIFTY", "NIFTY"
        };
        when(orderConfiguration.getSymbolsPath()).thenReturn("ind_nifty100list.csv");
        when(orderConfiguration.getAdditionalSymbols()).thenReturn(additionalSymbols);
        when(orderConfiguration.getAvailableCash()).thenReturn(25000d);

        openOrderVerifier = new OpenOrderVerifier(orderConfiguration, kiteService, timeProvider);
    }



    @Test
    void testIsPlaceOrderWhenSymbolIsNotInAllowedSymbolsForBuy() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);

        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenOrderAmountIsGreaterThanBuy() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        activeOrder.setBuyOptionPrice(2600);
        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenOrderAmountIsMoreThanAvailableCashForBuy() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);

        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenOrderAmountIsInLimitThanBuy() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks("","", true, true))
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 102, 1);
        activeOrder.setBuyOptionPrice(200);
        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertTrue(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenExtraDataIsNotAvailableForSell() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        activeOrder.setBuyOptionPrice(200);
        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, false);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenExtraDataNotSetForSell() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        activeOrder.setBuyOptionPrice(200);
        activeOrder.appendExtraData("test", "");

        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, false);
        //Assert
        assertFalse(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenExtraDataSetForSell() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", NIFTY_BANK, new StrategyTasks("","", true, true))
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        activeOrder.setBuyOptionPrice(200);
        activeOrder.appendExtraData(ORDER_EXECUTED, "true");

        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, false);
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
        Optional<OpenOrder> openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(), List.of());

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderIsEmpty() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();

        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        //Act
        Optional<OpenOrder> openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbols() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        OpenOrder openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(newOpenOrder), List.of(activeOrder)).get();

        //Assert
        assertEquals(openOrderEntry, newOpenOrder);
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToSameSymbols() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        //Act
        Optional<OpenOrder> openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(newOpenOrder), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbolsForPut() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(99);

        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        OpenOrder openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(newOpenOrder), List.of(activeOrder)).get();

        //Assert
        assertEquals(openOrderEntry, newOpenOrder);
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbolsWithoutBuyTriggeredForPut() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(121);

        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(120)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        Optional<OpenOrder> openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(newOpenOrder), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToDifferentSymbolsWithoutBuyTriggered() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        //Act
        OpenOrder openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(newOpenOrder), List.of(activeOrder)).get();

        //Assert
        assertEquals(openOrderEntry, newOpenOrder);
    }

    @Test
    void testCheckEntryInOpenOrdersIfOpenOrderAndActiveBelongToSameSymbolsForPut() {
        //Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(101);

        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(120)
                .target(110)
                .quantity(50)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(kiteService.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        //Act
        Optional<OpenOrder> openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(newOpenOrder), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBelowStopLoss() {
        // Arrange
        double ltp = 89;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);
        activeOrder.setBuyOptionPrice(1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertTrue(continueOrder);

    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBetweenStopLossAndBuyAt() {
        // Arrange
        double ltp = 95;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);
        activeOrder.setBuyOptionPrice(1);
        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustAboveBuyAt() {
        // Arrange
        double ltp = 100.1;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);
        activeOrder.setBuyOptionPrice(1);
        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustBelowTarget() {
        // Arrange
        double ltp = 114;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsAboveTarget() {
        // Arrange
        double ltp = 116;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBelowStoplossForPut() {
        // Arrange
        double ltp = 111;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);
        activeOrder.setBuyOptionPrice(1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertTrue(continueOrder);

    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBetweenStopLossAndBuyAtForPut() {
        // Arrange
        double ltp = 105;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);
        activeOrder.setBuyOptionPrice(1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustAboveBuyAtForPut() {
        // Arrange
        double ltp = 99.9;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);
        activeOrder.setBuyOptionPrice(1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustBelowTargetForPut() {
        // Arrange
        double ltp = 86;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsAboveTargetForPut() {
        // Arrange
        double ltp = 84;

        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(openOrder, ltp, 1);

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(ltp, activeOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testIsNotInActiveOrdersWhenNoActiveOrderIsPresent() {
        // Arrange
        OpenOrder openOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();

        //Act
        boolean isNotInActiveOrder = openOrderVerifier.isNotInActiveOrders(List.of(), openOrder);

        // Assert
        assertTrue(isNotInActiveOrder);
    }

    @Test
    void testIsNotInActiveOrdersWhenDiffActiveOrderIsPresent() {
        // Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL_2", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);

        //Act
        boolean isNotInActiveOrder = openOrderVerifier.isNotInActiveOrders(List.of(activeOrder), newOpenOrder);

        // Assert
        assertTrue(isNotInActiveOrder);
    }

    @Test
    void testIsNotInActiveOrdersWhenDiffActiveOrderTagIsPresent() {
        // Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG_2", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);

        //Act
        boolean isNotInActiveOrder = openOrderVerifier.isNotInActiveOrders(List.of(activeOrder), newOpenOrder);

        // Assert
        assertTrue(isNotInActiveOrder);
    }

    @Test
    void testIsNotInActiveOrdersWheSameOrderAndSymbolIsPresent() {
        // Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(145)
                .target(110)
                .quantity(50)
                .build();

        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);

        //Act
        boolean isNotInActiveOrder = openOrderVerifier.isNotInActiveOrders(List.of(activeOrder), newOpenOrder);

        // Assert
        assertFalse(isNotInActiveOrder);
    }
}