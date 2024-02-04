package com.vish.fno.manage.helper;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.ActiveOrderFactory;
import com.vish.fno.model.order.OpenIndexOrder;
import com.vish.fno.model.order.OpenOrder;
import com.vish.fno.reader.helper.InstrumentCache;
import com.zerodhatech.models.Tick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static com.vish.fno.util.Constants.NIFTY_50;
import static com.vish.fno.util.Constants.NIFTY_BANK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class OpenOrderVerifierTest {

    private static final String ORDER_EXECUTED = "orderExecuted";
    @Mock
    private OrderConfiguration orderConfiguration;
    @Mock
    private InstrumentCache instrumentCache;
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

        openOrderVerifier = new OpenOrderVerifier(orderConfiguration, instrumentCache);
    }



    @Test
    void testIsPlaceOrderWhenSymbolIsNotInAllowedSymbolsForBuy() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index(NIFTY_BANK)
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index(NIFTY_BANK)
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        activeOrder.setBuyOptionPrice(200);
        //Act
        boolean isPlaceOrder = openOrderVerifier.isPlaceOrder(activeOrder, true);
        //Assert
        assertTrue(isPlaceOrder);
    }

    @Test
    void testIsPlaceOrderWhenExtraDataIsNotAvailableForSell() {
        //Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index(NIFTY_BANK)
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index(NIFTY_BANK)
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index(NIFTY_BANK)
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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

        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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

        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL_2")
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

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

        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

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

        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL_2")
                .callOrder(false)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

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

        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL_2")
                .callOrder(false)
                .buyThreshold(120)
                .target(110)
                .quantity(50)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

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

        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL_2")
                .callOrder(true)
                .buyThreshold(100)
                .target(110)
                .quantity(50)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

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

        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(false)
                .buyThreshold(110)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .buyThreshold(120)
                .target(110)
                .quantity(50)
                .tag("TEST_TAG")
                .build();
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        //Act
        Optional<OpenOrder> openOrderEntry = openOrderVerifier.checkEntryInOpenOrders(tick, List.of(newOpenOrder), List.of(activeOrder));

        //Assert
        assertTrue(openOrderEntry.isEmpty());
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBelowStopLoss() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertTrue(continueOrder);

    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBetweenStopLossAndBuyAt() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(95);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustAboveBuyAt() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(100.1);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustBelowTarget() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(114);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsAboveTarget() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(116);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .target(115)
                .buyThreshold(100)
                .stopLoss(90)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBelowStoplossForPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(111);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertTrue(continueOrder);

    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsBetweenStopLossAndBuyAtforPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(105);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustAboveBuyAtForPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(99.9);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertTrue(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsJustBelowTargetForPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(86);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testHasMoveAlreadyHappenedIfLTPIsAboveTargetForPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(84);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .target(85)
                .buyThreshold(100)
                .stopLoss(110)
                .build();

        //Act
        boolean continueOrder = openOrderVerifier.hasNotMoveAlreadyHappened(tick, openOrder);

        // Assert
        assertFalse(continueOrder);
    }

    @Test
    void testIsNotInActiveOrdersWhenNoActiveOrderIsPresent() {
        // Arrange
        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();

        //Act
        boolean isNotInActiveOrder = openOrderVerifier.isNotInActiveOrders(List.of(), openOrder);

        // Assert
        assertTrue(isNotInActiveOrder);
    }

    @Test
    void testIsNotInActiveOrdersWhenDiffActiveOrderIsPresent() {
        // Arrange
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL_2")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
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
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG_2")
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
        OpenOrder existingOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();
        OpenOrder newOpenOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .buyThreshold(145)
                .target(110)
                .quantity(50)
                .tag("TEST_TAG")
                .build();

        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(existingOpenOrder, 0d, 1);

        //Act
        boolean isNotInActiveOrder = openOrderVerifier.isNotInActiveOrders(List.of(activeOrder), newOpenOrder);

        // Assert
        assertFalse(isNotInActiveOrder);
    }
}