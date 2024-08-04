package com.vish.fno.manage.helper;

import com.vish.fno.model.helper.EntryVerifier;
import com.vish.fno.model.helper.OrderCache;
import com.vish.fno.model.Task;
import com.vish.fno.model.order.OrderSellDetailModel;
import com.vish.fno.model.order.activeorder.ActiveOrder;
import com.vish.fno.model.order.orderrequest.OrderRequest;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class IndexEntryVerifier implements EntryVerifier {

    // TODO: dont stop sell order by price (but the condition is only for buyPrice :thinking)
    public boolean isPlaceOrder(ActiveOrder order, boolean isBuy, OrderSellDetailModel exitCondition, OrderCache orderCache, boolean isExpiryDayForOption) {
        // check if the strategy is enabled for expiry day
        Task task = order.getTask();
        // TODO: add test case
        if(!task.isExpiryDayOrders() && isExpiryDayForOption) {
            log.info("Expiry day orders is not enabled for task: {} ", order.getTask());
            return false;
        }

        if(!order.getTask().isEnabled()) {
            log.info("The following task({}) has not been enabled, not placing order: {}", order.getTask(), order);
            return false;
        }

        double availableCash = orderCache.getAvailableCash();
        if(isBuy) {
            if(availableCash > order.getBuyOptionPrice() * order.getBuyQuantity()){
                order.appendExtraData(ORDER_EXECUTED, String.valueOf(true)); // todo: move after order placed in kite
                availableCash -= order.getBuyOptionPrice() * order.getBuyQuantity();
                orderCache.setAvailableCash(availableCash);
                log.info("Placing order as the symbol is : {} or amount is: {}",
                        order.getIndex(), (order.getBuyQuantity() * order.getBuyOptionPrice()));
                log.info("Reducing available cash to {}, order : {}", availableCash, order);
                return true;
            } else {
                log.info("Not placing order as the symbol is : {} or amount is: {}. Available amount: {}",
                        order.getIndex(), (order.getBuyQuantity() * order.getBuyOptionPrice()), orderCache.getAvailableCash());
            }
        }

        if(!isBuy) {
            Map<String, String> extraData = order.getExtraData();
            if(extraData != null && extraData.containsKey(ORDER_EXECUTED)) {
                boolean wasExecuted = Boolean.parseBoolean(extraData.get(ORDER_EXECUTED));
                if(wasExecuted) {
                    availableCash += order.getBuyOptionPrice() * exitCondition.getQuantity();
                    orderCache.setAvailableCash(availableCash);
                    log.info("Updating available cash back to {}, order : {}", availableCash, order);
                }
                return Boolean.parseBoolean(extraData.get(ORDER_EXECUTED));
            } else {
                log.info("no need to place order to kite server as the extra data doesn't have order_executed : {}, order: {}", order.getExtraData(), order);
            }
        }
        return false;
    }

    @Override
    public boolean hasMoveAlreadyHappened(double ltp, OrderRequest order) {

        if(ltp < 0.1) {
            log.info("ltp price not set, not placing order for {}. ltp: {}, order: {}", order.getIndex(), ltp, order);
            return true;
        }

        final double buyAt = order.getBuyThreshold();
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

    // CPD-OFF
    public boolean isNotInActiveOrders(List<ActiveOrder> activeOrders, OrderRequest tickOrderRequest) {
        boolean isNotInActiveOrder = activeOrders
                .stream()
                .noneMatch(a -> a.getTag().equalsIgnoreCase(tickOrderRequest.getTag())
                        && a.getIndex().equalsIgnoreCase(tickOrderRequest.getIndex())
                        && a.isCallOrder() == tickOrderRequest.isCallOrder());
        if(!isNotInActiveOrder) {
            log.info("Already an active order present for symbol: {}, open order: {}, call: {}",
                    tickOrderRequest.getIndex(), tickOrderRequest, tickOrderRequest.isCallOrder());
        }
        return isNotInActiveOrder;
    }
    // CPD-ON

}
