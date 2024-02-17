package com.vish.fno.manage.helper;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.model.Task;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.OrderRequest;
import com.vish.fno.reader.service.KiteService;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public final class EntryVerifier {

    public static final String ORDER_EXECUTED = "orderExecuted";
    private double availableCash;
    private final KiteService kiteService;
    private final TimeProvider timeProvider;

    public EntryVerifier(OrderConfiguration orderConfiguration, KiteService kiteService, TimeProvider timeProvider) {
        this.availableCash = orderConfiguration.getAvailableCash();
        this.kiteService = kiteService;
        this.timeProvider = timeProvider;
    }

    public Optional<OrderRequest> checkEntryInOpenOrders(Tick tick, List<OrderRequest> orderRequests, List<ActiveOrder> activeOrders) {
        if(orderRequests.isEmpty()) {
            return Optional.empty();
        }

        String tickSymbol = kiteService.getSymbol(tick.getInstrumentToken());
        List<OrderRequest> tickSymbolOrders = orderRequests
                .stream()
                .filter(e -> e.getIndex().contentEquals(tickSymbol) && isNotInActiveOrders(activeOrders, e))
                .toList();

        for (OrderRequest order : tickSymbolOrders) {
            Optional<OrderRequest> openOrderOptional = verifyAndPlaceOrder(tick, order);
            if(!orderRequests.isEmpty()) {
                return openOrderOptional;
            }
        }
        return Optional.empty();
    }


    // TODO: dont stop sell order by price (but the condition is only for buyPrice :thinking)
    public boolean isPlaceOrder(ActiveOrder order, boolean isBuy) {
        // check if the strategy is enabled for expiry day
        Task task = order.getTask();
        // TODO: add test case
        if(!task.isExpiryDayOrders() && kiteService.isExpiryDayForOption(order.getOptionSymbol(), timeProvider.todayDate())) {
            log.info("Expiry day orders is not enabled for task: {} ", order.getTask());
            return false;
        }


        if(!order.getTask().isEnabled()) {
            log.info("The following task({}) has not been enabled, not placing order: {}", order.getTask(), order);
            return false;
        }

        if(isBuy) {
            if(availableCash > order.getBuyOptionPrice() * order.getQuantity()){
                order.appendExtraData(ORDER_EXECUTED, String.valueOf(true)); // todo: move after order placed in kite
                availableCash -= order.getBuyOptionPrice() * order.getQuantity();
                log.info("Placing order as the symbol is : {} or amount is: {}",
                        order.getIndex(), (order.getQuantity() * order.getBuyOptionPrice()));
                log.info("Reducing available cash to {}, order : {}", availableCash, order);
                return true;
            } else {
                log.info("Not placing order as the symbol is : {} or amount is: {}. Available amount: {}",
                        order.getIndex(), (order.getQuantity() * order.getBuyOptionPrice()), availableCash);
            }
        }

        if(!isBuy) {
            Map<String, String> extraData = order.getExtraData();
            if(extraData != null && extraData.containsKey(ORDER_EXECUTED)) {
                boolean wasExecuted = Boolean.parseBoolean(extraData.get(ORDER_EXECUTED));
                if(wasExecuted) {
                    availableCash += order.getBuyOptionPrice() * order.getQuantity(); // todo update to
                    log.info("Updating available cash back to {}, order : {}", availableCash, order);
                }
                return Boolean.parseBoolean(extraData.get(ORDER_EXECUTED));
            } else {
                log.info("no need to place order to kite server as the extra data doesn't have order_executed : {}, order: {}", order.getExtraData(), order);
            }
        }
        return false;
    }

    public boolean hasMoveAlreadyHappened(double ltp, ActiveOrder order) {

        if(order.getBuyOptionPrice() < 0.1) {
            log.info("ltp price not set, not placing order for {}. ltp: {}, order: {}", order.getIndex(), ltp, order);
            return true;
        }

        final double buyAt = order.getBuyPrice();
        final double target = order.getTarget();

        if(order.isCallOrder()) {
            final double priceAlreadyCrossed = ltp - buyAt;
            final double targetRemaining = target - ltp;
            // to ensure risk:reward is still 1:1, for 1:1.5 initial target
            if(targetRemaining > (priceAlreadyCrossed * 5)) {
                return false;
            }
        } else {
            final double priceAlreadyCrossed =  buyAt - ltp;
            final double targetRemaining = ltp - target;
            if(targetRemaining > (priceAlreadyCrossed * 5)) {
                return false;
            }
        }
        log.info("Price movement has already happened for {}, ltp : {}, order buy: {}, target: {}",
                order.getIndex(), ltp, order.getBuyThreshold(), order.getTarget());

        return true;
    }

    public boolean isNotInActiveOrders(List<ActiveOrder> activeOrders, OrderRequest tickOrderRequest) {
        boolean isNotInActiveOrder = activeOrders
                .stream()
                .noneMatch(a -> a.getTag().equalsIgnoreCase(tickOrderRequest.getTag()) && a.getIndex().equalsIgnoreCase(tickOrderRequest.getIndex()));
        if(!isNotInActiveOrder) {
            log.info("Already an active order present for symbol: {}, open order : {}", tickOrderRequest.getIndex(), tickOrderRequest);
        }
        return isNotInActiveOrder;
    }

    private Optional<OrderRequest> verifyAndPlaceOrder(Tick tick, OrderRequest order) {

        if(order.isCallOrder()) {
            if (tick.getLastTradedPrice() > order.getBuyThreshold()) {
                log.info("tick ltp: {} is greater than buy threshold {}, placing order({}) : {}",
                        tick.getLastTradedPrice(), order.getBuyThreshold(), order.getOptionSymbol(), order);
                return Optional.of(order);
            }
        } else {
            if (tick.getLastTradedPrice() < order.getBuyThreshold()) {
                log.info("tick ltp: {} is lesser than buy threshold {}, placing order({}) : {}",
                        tick.getLastTradedPrice(), order.getBuyThreshold(), order.getOptionSymbol(), order);
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }
}
