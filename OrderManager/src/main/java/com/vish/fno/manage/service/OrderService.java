package com.vish.fno.manage.service;

import com.vish.fno.reader.service.KiteService;
import com.vish.fno.util.JsonUtils;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD")
public class OrderService {
    private final KiteService kiteService;

    public void openPositions() {
        log.info("Loaded orders from Kite server : {}", JsonUtils.getFormattedObject(kiteService.getOrders()));
    }

    public void openOrders() {
        log.info("Loaded positions from Kite server : {}", JsonUtils.getFormattedObject(kiteService.getPositions()));
    }

    // TODO: simplify
    public Optional<Order> placeOrder(Map<String, String> orderProps) {
        OrderParams orderParams = new OrderParams();
        for(String key: orderProps.keySet()) {
            if(key.equals("quantity")) {
                orderParams.quantity = Integer.parseInt(orderProps.get(key));
            }
            if(key.equals("orderType")) {
                orderParams.orderType = orderProps.get(key);
            }
            if(key.equals("tradingsymbol")) {
                orderParams.tradingsymbol = orderProps.get(key);
            }
            if(key.equals("product")) {
                orderParams.product = orderProps.get(key);
            }
            if(key.equals("exchange")) {
                orderParams.exchange = orderProps.get(key);
            }
            if(key.equals("validity")) {
                orderParams.validity = orderProps.get(key);
            }
            if(key.equals("transactionType")) {
                orderParams.transactionType = orderProps.get(key);
            }
            if(key.equals("price")) {
                orderParams.price = Double.parseDouble(orderProps.get(key));
            }
            if(key.equals("triggerPrice")) {
                orderParams.triggerPrice = Double.parseDouble(orderProps.get(key));
            }
            if(key.equals("tag")) {
                orderParams.tag = orderProps.get(key);
            }
        }

        Order order = kiteService.placeOptionOrder(orderParams);

        if(order == null) {
            return Optional.empty();
        }
        return Optional.of(order);
    }
}
