package com.vish.fno.manage.orderflow;

import com.google.common.annotations.VisibleForTesting;
import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.model.cache.OrderCache;
import com.vish.fno.manage.helper.TickMapper;
import com.vish.fno.model.Ticker;
import com.vish.fno.model.order.activeorder.ActiveIndexOrder;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.activeorder.OptionBasedActiveOrder;
import com.vish.fno.model.order.orderrequest.IndexOrderRequest;
import com.vish.fno.model.order.orderrequest.OptionBasedOrderRequest;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import com.vish.fno.reader.model.KiteOpenOrder;
import com.vish.fno.util.JsonUtils;
import com.vish.fno.util.helper.TimeProvider;
import com.vish.fno.util.FileUtils;
import com.vish.fno.model.order.*;
import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.helper.OrderManagerUtils;
import com.vish.fno.util.orderflow.TargetAndStopLossStrategy;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.OnOrderUpdate;
import com.zerodhatech.ticker.OnTicks;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.vish.fno.model.helper.EntryVerifier.ORDER_EXECUTED;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.PrematureDeclaration"})
public class OrderHandler {
    private static final String ORDER_EXIT_DETAILS = "orderExitDetails";
    public static final String REJECTED = "REJECTED";
    public static final String BUY = "BUY";
    private final KiteService kiteService;
    private final OrderConfiguration orderConfiguration;
    private final FileUtils fileUtils;
    private final TimeProvider timeProvider;
    private final TargetAndStopLossStrategy targetAndStopLossStrategy;
    @Getter
    private final OrderCache orderCache;
    private final TickStrategyExecutor tickStrategyExecutor;

    @PostConstruct
    public void initialiseWebSocket() {
        final OnTicks onTickerArrivalListener = ticks -> {
            if (!ticks.isEmpty()) {
                handleTicks(ticks);
            }
        };
        final OnOrderUpdate onOrderUpdateListener = this::onOrderUpdate;
        kiteService.setOnTickerArrivalListener(onTickerArrivalListener);
        kiteService.setOnOrderUpdateListener(onOrderUpdateListener);
        final List<String> initialSymbols = Arrays.stream(orderConfiguration.getWebSocketDefaultSymbols()).toList();
        kiteService.appendWebSocketSymbolsList(initialSymbols, true);
    }

    @VisibleForTesting
    public void handleTicks(List<Tick> ticks) {
        for(Tick tick: ticks) {
            try {
                final String tickSymbol = kiteService.getSymbol(tick.getInstrumentToken());
                fileUtils.appendTickToFile(tickSymbol, tick);
                applyOrderFlow(tick, tickSymbol);
            } catch (Exception e) {
                log.warn("Failed to apply tick information for tick: {}", tick.getInstrumentToken(), e);
            }
        }
    }

    private void applyOrderFlow(final Tick tick, final String tickSymbol) {
        final Ticker ticker = TickMapper.mapTick(tick, tickSymbol);
        tickStrategyExecutor.update(ticker);
        orderCache.appendTick(tickSymbol, ticker);
        getActiveOrderToSell(tickSymbol, ticker);
        orderCache.checkEntryInOpenOrders(ticker, tickSymbol)
                .ifPresent(order -> createActiveOrder(ticker, order));
    }

    private void createActiveOrder(Ticker tick, OrderRequest order) {
        ActiveOrder activeOrder = ActiveOrderFactory.createOrder(order, tick.getLastTradedPrice(), timeProvider.currentTimeStampIndex(), timeProvider.getCurrentStringDateTime());
        if(activeOrder == null) {
            log.warn("Failed to create active order for order request : {}", order);
            return;
        }
        placeBuyOrder(order, activeOrder, tick);
        adjustAvailableCashAfterBuy(activeOrder);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void onOrderUpdate(Order order) {
        final List<ActiveOrder> activeOrders = orderCache.getActiveOrders();
        log.info("Order update complete : {}", JsonUtils.getFormattedObject(order));

        try {
            String orderId = order.orderId;
            List<ActiveOrder> activeOrdersForOrderId = OrderDetailsLogger.getActiveOrdersByOrderId(activeOrders, orderId);
            log.info("Existing active order for order id: {}: {}", orderId, activeOrdersForOrderId);
            OrderDetailsLogger.logOrderLifeCycle(activeOrders, order, orderId);

            if(order.status.equals(REJECTED) && order.transactionType.equals(BUY)) {
                log.warn("Order was rejected with statusMessage: {}, marking it as not complete", order.statusMessage);
                activeOrders.stream().filter(a ->
                        a.getTradingSymbol().equals(order.tradingSymbol) && a.getTag().substring(0,20).equals(order.tag)
                ).forEach(a -> {
                    log.info("marking order executed as false for order: {}", a);
                    a.getExtraData().put(ORDER_EXECUTED, "false");
                });
            }
        } catch (Exception e) {
            log.error("Exception while finding active order for order update", e);
        }
    }

    private void placeBuyOrder(OrderRequest order, ActiveOrder activeOrder, Ticker tick) {
        boolean isExpiryDayForOption = kiteService.isExpiryDayForOption(activeOrder.getTradingSymbol(), timeProvider.todayDate());

        Ticker optionTick = getTickForPlacingOrder(activeOrder);
        if(optionTick == null) {
            log.warn("option tick null for active order: {}", activeOrder);
            return;
        }

        log.info("Got option tick: {} to verify placing buy order: {}", optionTick, activeOrder);
        boolean isPlaceOrder = order.isPlaceOrder(optionTick.getLastTradedPrice(), orderCache.getAvailableCash(), isExpiryDayForOption);

        log.info("Is expiry day for option ({}): {}  isPlaceOrder: {} for orderRequest: {}, activeOrder: {}",
                activeOrder.getTradingSymbol(), isExpiryDayForOption, isPlaceOrder, order, activeOrder);
        OrderDetailsLogger.logMarketDepth(tick);
        Optional<KiteOpenOrder> orderPlacedOptional = kiteService.buyOrder(activeOrder.getTradingSymbol(),
                activeOrder.getBuyQuantity(),
                activeOrder.getTag(), isPlaceOrder);

        if(orderPlacedOptional.isPresent() && orderPlacedOptional.get().isOrderPlaced()) {
            KiteOpenOrder orderPlaced = orderPlacedOptional.get();
            activeOrder.setActive(true);

            if(activeOrder instanceof ActiveIndexOrder activeIndexOrder) {
                activeIndexOrder.setBuyOptionPrice(optionTick.getLastTradedPrice());
            }
            if(orderPlaced.getOrder() != null) {
                activeOrder.appendExtraData("kiteOrderId", orderPlaced.getOrder().orderId);
                activeOrder.appendExtraData(ORDER_EXECUTED, String.valueOf(true));
            }
            orderCache.appendActiveOrder(activeOrder);
            orderCache.removeOrderRequest(order);
            log.debug("Order placed successfully, active orders: {}", orderCache.getActiveOrders());
        } else {
            log.warn("Failed to place order : {}", order);
            // TODO: add back the amount in open order verifier
            // TODO: temporarily continuing the order in mock trade
            activeOrder.appendExtraData(ORDER_EXECUTED, String.valueOf(false));
            activeOrder.setActive(true);
            orderCache.appendActiveOrder(activeOrder);
            orderCache.removeOrderRequest(order);
        }
    }

    private Ticker getTickForPlacingOrder(ActiveOrder activeOrder) {
        Ticker ticker = null;
        if(activeOrder instanceof ActiveIndexOrder activeIndexOrder) {
            ticker = orderCache.getLatestTick(activeIndexOrder.getOptionSymbol());
        } else {
            ticker = orderCache.getLatestTick(activeOrder.getTradingSymbol());
        }
        return ticker;
    }

    public void appendOpenOrder(OrderRequest orderRequest) {

        if(orderRequest instanceof IndexOrderRequest indexOrderRequest) {
            String optionSymbol = kiteService.getITMStock(orderRequest.getIndex(), orderRequest.getBuyThreshold(), indexOrderRequest.isCallOrder());
            indexOrderRequest.setOptionSymbol(optionSymbol);
        }

        final Ticker tick = orderCache.getLatestTick(orderRequest.getIndex());
        if(tick == null) {
            log.info("Not appending order request : {}, as tick is null. Appending {} to websocket", orderRequest, orderRequest.getIndex());
            // TODO: but order is missed here
            kiteService.appendWebSocketSymbolsList(List.of(orderRequest.getIndex()), false);
            return;
        }

        // TODO, make hasMoveAlreadyHappened check before placing and creating active order and add the OrderRequest even after tick null is found
        if(orderCache.isNotInActiveOrders(orderRequest) && !orderRequest.hasMoveAlreadyHappened(tick.getLastTradedPrice())) {
            orderCache.addOrderRequest(orderRequest);
            addTokenToWebsocket(orderRequest);
        }
    }

    private void getActiveOrderToSell(String tickSymbol, Ticker tick) {
        List<ActiveOrder> activeOrdersForTick = orderCache.getActiveOrderForSymbol(tickSymbol);
        if(activeOrdersForTick.isEmpty()) {
            return;
        }

        for(ActiveOrder order : activeOrdersForTick) {
            OrderSellDetailModel exitCondition = OrderManagerUtils.isExitCondition(targetAndStopLossStrategy, tick.getLastTradedPrice(), timeProvider.currentTimeStampIndex(), order);
            if(exitCondition.isSellOrder()) {
                placeSellOrder(exitCondition, order, tick);
                adjustAvailableCashAfterSell(order, exitCondition);
            }
        }
    }

    private void placeSellOrder(OrderSellDetailModel exitCondition, ActiveOrder order, Ticker tick) {
        boolean isPlaceOrder = Boolean.parseBoolean(order.getExtraData().get(ORDER_EXECUTED));
        log.info("Creating sell order with quantity: {}, symbol: {}, tag: {}, isPlaceOrder: {}",
                exitCondition.getQuantity(), order.getTradingSymbol(), order.getTag(), isPlaceOrder);
        OrderDetailsLogger.logMarketDepth(tick);
        final Optional<KiteOpenOrder> orderSoldOptional = kiteService.sellOrder(order.getTradingSymbol(),
                exitCondition.getQuantity(),
                order.getTag(),
                isPlaceOrder);

        KiteOpenOrder kiteOpenOrder = orderSoldOptional.orElse(null);

        if(kiteOpenOrder != null && kiteOpenOrder.getExceptionMessage() == null) {
            final Ticker latestOptionTick = orderCache.getLatestTick(order.getTradingSymbol());
            order.incrementSoldQuantity(exitCondition.getQuantity(), latestOptionTick.getLastTradedPrice());
            appendExtraData(exitCondition, tick, order);
            log.info("Order sold successfully, details : {}, order: {}", kiteOpenOrder, order);
            if(order.getBuyQuantity() == order.getSoldQuantity()) {
                order.closeOrder(tick.getLastTradedPrice(), timeProvider.currentTimeStampIndex(), timeProvider.getCurrentStringDateTime());
                orderCache.removeActiveOrder(order);
                fileUtils.logCompletedOrder(order);
            }
        } else {
            log.error("########################################################");
            log.error("FAILED TO EXIT ORDER, PLEASE EXIT ORDER MANUALLY..., order: {}", order);
            log.error("########################################################");
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void appendExtraData(OrderSellDetailModel exitCondition, Ticker tick, ActiveOrder order) {
        try {
            final String existingExitOrderDetails = order.getExtraData().get(ORDER_EXIT_DETAILS);
            String exitOrderDetails = null;
            if(order instanceof ActiveIndexOrder) {
                final Ticker latestOptionTick = orderCache.getLatestTick(order.getTradingSymbol());
                final double sellOptionPrice = latestOptionTick == null ? 0 : latestOptionTick.getLastTradedPrice();
                exitOrderDetails = (existingExitOrderDetails == null ? "" : existingExitOrderDetails + ",") +
                        Map.of("quantity", exitCondition.getQuantity(),
                                "sellPrice", String.valueOf(tick.getLastTradedPrice()),
                                "sellOptionPrice", String.valueOf(sellOptionPrice));
            } else {
                exitOrderDetails = (existingExitOrderDetails == null ? "" : existingExitOrderDetails + ",") +
                        Map.of("quantity", exitCondition.getQuantity(),
                                "sellPrice", String.valueOf(tick.getLastTradedPrice()));
            }
            order.getExtraData().put(ORDER_EXIT_DETAILS, exitOrderDetails);
        } catch (Exception e) {
            log.warn("failed to put data", e);
        }
    }

    private void adjustAvailableCashAfterSell(ActiveOrder order, OrderSellDetailModel exitCondition) {
        Map<String, String> extraData = order.getExtraData();
        if (extraData != null && extraData.containsKey(ORDER_EXECUTED)) {
            boolean wasExecuted = Boolean.parseBoolean(extraData.get(ORDER_EXECUTED));
            if (wasExecuted) {
                double sellPrice = 0;
                // The profits are only available on next trading day
                if(order instanceof ActiveIndexOrder activeIndexOrder) {
                    sellPrice = Math.min(activeIndexOrder.getBuyOptionPrice(), activeIndexOrder.getSellOptionPrice());
                } else if(order instanceof OptionBasedActiveOrder optionBasedActiveOrder) {
                    sellPrice = Math.min(optionBasedActiveOrder.getBuyPrice(), optionBasedActiveOrder.getSellPrice());
                } else {
                    log.error("Invalid order type while adjusting cash after sell, order: {}", order);
                }
                double availableCache = orderCache.getAvailableCash() + sellPrice * exitCondition.getQuantity();
                orderCache.setAvailableCash(availableCache);
                log.info("Updating available cash back to {}, order : {}", orderCache.getAvailableCash(), order);
            }
        }
    }

    private void adjustAvailableCashAfterBuy(ActiveOrder activeOrder) {
        if(activeOrder.getExtraData().containsKey(ORDER_EXECUTED) && Boolean.parseBoolean(activeOrder.getExtraData().get(ORDER_EXECUTED))) {
            if(activeOrder instanceof ActiveIndexOrder activeIndexOrder) {
                double availableCache = orderCache.getAvailableCash() - activeIndexOrder.getBuyOptionPrice() * activeOrder.getBuyQuantity();
                orderCache.setAvailableCash(availableCache);
            } else if(activeOrder instanceof OptionBasedActiveOrder optionBasedActiveOrder) {
                double availableCache = orderCache.getAvailableCash() - optionBasedActiveOrder.getBuyPrice() * activeOrder.getBuyQuantity();
                orderCache.setAvailableCash(availableCache);
            }
            log.info("Reducing available cash to {}, order : {}", orderCache.getAvailableCash(), activeOrder);
        }
    }

    private void addTokenToWebsocket(OrderRequest order) {
        if(order instanceof IndexOrderRequest indexOrderRequest) {
            kiteService.appendWebSocketSymbolsList(List.of(order.getIndex(), indexOrderRequest.getOptionSymbol()), false);
        }
        if(order instanceof OptionBasedOrderRequest optionBasedOrderRequest) {
            kiteService.appendWebSocketSymbolsList(List.of(order.getIndex(), optionBasedOrderRequest.getIndex()), false);
        }
    }
    // TODO: add an additional check to ensure order is not placed against wrong instrument.
}
