package com.vish.fno.manage.helper;

import com.vish.fno.manage.model.StrategyTasks;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.ActiveOrderFactory;
import com.vish.fno.model.order.IndexOrderRequest;
import com.vish.fno.model.order.OrderRequest;
import com.vish.fno.util.orderflow.FixedTargetAndStopLossStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopLossAndTargetHandlerTest {

    @Spy
    FixedTargetAndStopLossStrategy targetAndStopLossStrategy;
    @InjectMocks private StopLossAndTargetHandler stopLossAndTargetHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetActiveOrderToSellForEmptyActiveOrders() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 89;

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,List.of());

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }

    @Test
    void testGetActiveOrderToSellForActiveOrdersOfDifferentTick() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 89;
        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .optionSymbol("TEST_OPTION_SYMBOL")
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .quantity(10)
                .build();
        List<ActiveOrder> activeOrders = List.of(ActiveOrderFactory.createOrder(orderRequest, 0d, 1));

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,activeOrders);

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }

    @Test
    void testGetActiveOrderToSellForTagetHitForCall() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 106;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(101)
                .stopLoss(90)
                .target(105)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(orderRequest, 0d, 1);

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForCall() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 89;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(orderRequest, 0d, 1);

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForCall() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 91;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(true)
                .buyThreshold(101)
                .target(105)
                .stopLoss(90)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(orderRequest, 0d, 1);

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,List.of(activeOrders));

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }

    @Test
    void testGetActiveOrderToSellForTargetHitForPut() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 89;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(100)
                .stopLoss(115)
                .target(90)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(orderRequest, 0d, 1);

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellForStopLossHitForPut() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 112;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(100)
                .target(85)
                .stopLoss(110)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(orderRequest, 0d, 1);

        // Act
        ActiveOrder activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,List.of(activeOrders)).get();

        // Assert
        assertEquals(activeOrderOptional, activeOrders);
    }

    @Test
    void testGetActiveOrderToSellWhenPriceBetweenTargetAndStopLossForPut() {
        // Arrange
        String tickSymbol = "TEST_SYMBOL";
        double ltp = 91;

        OrderRequest orderRequest = IndexOrderRequest.builder("TEST_TAG", "TEST_SYMBOL", new StrategyTasks())
                .callOrder(false)
                .buyThreshold(100)
                .target(90)
                .stopLoss(105)
                .build();

        ActiveOrder activeOrders = ActiveOrderFactory.createOrder(orderRequest, 0d, 1);

        // Act
        Optional<ActiveOrder> activeOrderOptional = stopLossAndTargetHandler.getActiveOrderToSell(tickSymbol, ltp, 115 ,List.of(activeOrders));

        // Assert
        assertTrue(activeOrderOptional.isEmpty());
    }
}