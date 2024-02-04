package com.vish.fno.manage.helper;

import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.ActiveOrderFactory;
import com.vish.fno.model.order.OpenIndexOrder;
import com.vish.fno.model.order.OpenOrder;
import com.vish.fno.reader.helper.InstrumentCache;
import com.zerodhatech.models.Tick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class StopLossAndTargetHandlerTest {

    @Mock private TimeProvider timeProvider;
    @Mock private InstrumentCache instrumentCache;
    @InjectMocks private StopLossAndTargetHandler stopLossAndTargetHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetActiveOrderToSellForEmptyActiveOrders() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, List.of());

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }

    @Test
    void testGetActiveOrderToSellForActiveOrdersOfDifferentTick() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .tag("TEST_TAG")
                .build();

        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(openOrder, 0d, 1));
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL_2");

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, activeOrders);

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }

    @Test
    void testGetActiveOrderToSellForTagetHitForCall() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(106);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .stopLoss(90)
                .target(105)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(openOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForCall() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(openOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForCall() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(91);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(openOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, List.of(activeOrders));

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }

    @Test
    void testGetActiveOrderToSellForTargetHitForPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(89);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .stopLoss(115)
                .target(90)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(openOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(112);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .target(85)
                .stopLoss(110)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(openOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForPut() {
        // Arrange
        Tick tick = new Tick();
        tick.setInstrumentToken(1L);
        tick.setLastTradedPrice(91);

        OpenOrder openOrder = OpenIndexOrder.builder()
                .index("TEST_SYMBOL")
                .callOrder(false)
                .buyThreshold(100)
                .target(90)
                .stopLoss(105)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(openOrder, 0d, 1);
        when(instrumentCache.getSymbol(1L)).thenReturn("TEST_SYMBOL");

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tick, List.of(activeOrders));

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }
}