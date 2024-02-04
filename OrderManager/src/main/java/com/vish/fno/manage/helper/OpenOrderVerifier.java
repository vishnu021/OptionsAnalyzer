package com.vish.fno.manage.helper;

import com.vish.fno.manage.config.order.OrderConfiguration;
import com.vish.fno.model.order.ActiveOrder;
import com.vish.fno.model.order.OpenOrder;
import com.vish.fno.reader.service.KiteService;
import com.zerodhatech.models.Tick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.vish.fno.util.Constants.NIFTY_50;
import static com.vish.fno.util.Constants.NIFTY_BANK;

@Slf4j
@Component
public final class OpenOrderVerifier {

    private static final String ORDER_EXECUTED = "orderExecuted";
    private static final List<String> allowedSymbols = List.of(NIFTY_BANK, NIFTY_50, "HINDUNILVR", "BAJFINANCE");
    private double availableCash;
    private final KiteService kiteService;

    public OpenOrderVerifier(OrderConfiguration orderConfiguration, KiteService kiteService) {
        this.availableCash = orderConfiguration.getAvailableCash();
        this.kiteService = kiteService;
    }

    public Optional<OpenOrder> checkEntryInOpenOrders(Tick tick, List<OpenOrder> openOrders, List<ActiveOrder> activeOrders) {
        if(openOrders.isEmpty()) {
            return Optional.empty();
        }

        String tickSymbol = kiteService.getSymbol(tick.getInstrumentToken());
        List<OpenOrder> tickSymbolOrders = openOrders
                .stream()
                .filter(e -> e.getIndex().contentEquals(tickSymbol) && isNotInActiveOrders(activeOrders, e))
                .toList();

        for (OpenOrder order : tickSymbolOrders) {
            Optional<OpenOrder> openOrderOptional = verifyAndPlaceOrder(tick, order);
            if(!openOrders.isEmpty()) {
                return openOrderOptional;
            }
        }
        return Optional.empty();
    }


    // TODO: dont stop sell order by price (but the condition is only for buyPrice :thinking)
    public boolean isPlaceOrder(ActiveOrder order, boolean isBuy) {
        String index = order.getIndex();

        if(isBuy) {
            if(availableCash > order.getBuyOptionPrice() * order.getQuantity() && allowedSymbols.contains(index)){
                order.appendExtraData(ORDER_EXECUTED, String.valueOf(true)); // todo: move after order placed in kite
                availableCash -= order.getBuyOptionPrice() * order.getQuantity();
                log.info("Placing order as the symbol is : {} or amount is: {}",
                        order.getIndex(), (order.getQuantity() * order.getBuyOptionPrice()));
                log.info("Reducing available cash to {}, order : {}", availableCash, order);
                return true;
            } else {
                log.info("Not placing order as the symbol is : {} or amount is: {}",
                        order.getIndex(), (order.getQuantity() * order.getBuyOptionPrice()));
            }
        }

        if(!isBuy) {
            Map<String, String> extraData = order.getExtraData();
            if(extraData !=null && extraData.containsKey(ORDER_EXECUTED)) {
                boolean wasExecuted = Boolean.parseBoolean(extraData.get(ORDER_EXECUTED));
                if(wasExecuted) {
                    availableCash += order.getBuyOptionPrice() * order.getQuantity(); // todo update to
                    log.info("Updating available cash back to {}, order : {}", availableCash, order);
                }
                return Boolean.parseBoolean(extraData.get(ORDER_EXECUTED));
            }
        }
        return false;
    }

    public boolean hasNotMoveAlreadyHappened(Tick tick, OpenOrder order) {
        final double buyAt = order.getBuyThreshold();
        final double target = order.getTarget();
        final double ltp = tick.getLastTradedPrice();

        if(order.isCallOrder()) {
            final double priceAlreadyCrossed = ltp - buyAt;
            final double targetRemaining = target - ltp;
            // to ensure risk:reward is still 1:1, for 1:1.5 initial target
            if(targetRemaining > (priceAlreadyCrossed * 5)) {
                return true;
            }
        } else {
            final double priceAlreadyCrossed =  buyAt - ltp;
            final double targetRemaining = ltp - target;
            if(targetRemaining > (priceAlreadyCrossed * 5)) {
                return true;
            }
        }
        log.info("Price movement has already happened, ltp : {}, order buy: {}, target: {}",
                tick.getLastTradedPrice(), order.getBuyThreshold(), order.getTarget());

        return false;
    }

    public boolean isNotInActiveOrders(List<ActiveOrder> activeOrders, OpenOrder tickOpenOrder) {
        boolean isNotInActiveOrder = activeOrders
                .stream()
                .noneMatch(a -> a.getTag().equalsIgnoreCase(tickOpenOrder.getTag()) && a.getIndex().equalsIgnoreCase(tickOpenOrder.getIndex()));
        if(!isNotInActiveOrder) {
            log.info("Already an active order present for symbol: {}, open order : {}", tickOpenOrder.getIndex(), tickOpenOrder);
        }
        return isNotInActiveOrder;
    }

    private Optional<OpenOrder> verifyAndPlaceOrder(Tick tick, OpenOrder order) {
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
