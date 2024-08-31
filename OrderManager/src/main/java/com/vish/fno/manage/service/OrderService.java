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
public class OrderService {
    private final KiteService kiteService;

    public void openPositions() {
        log.info("Loaded orders from Kite server : {}", JsonUtils.getFormattedObject(kiteService.getOrders()));
    }

    public void openOrders() {
        log.info("Loaded positions from Kite server : {}", JsonUtils.getFormattedObject(kiteService.getPositions()));
    }

    public Optional<Order> placeOrder(Map<String, String> orderProps) {
        final OrderParams orderParams = new OrderParams();
        orderProps.forEach((key, value) -> {
            switch (key) {
                case "quantity" -> orderParams.quantity = Integer.parseInt(value);
                case "orderType" -> orderParams.orderType = value;
                case "tradingsymbol" -> orderParams.tradingsymbol = value;
                case "product" -> orderParams.product = value;
                case "exchange" -> orderParams.exchange = value;
                case "validity" -> orderParams.validity = value;
                case "transactionType" -> orderParams.transactionType = value;
                case "price" -> orderParams.price = Double.parseDouble(value);
                case "triggerPrice" -> orderParams.triggerPrice = Double.parseDouble(value);
                case "tag" -> orderParams.tag = value;
                default -> log.warn("unknown property: {}", key);
            }
        });

        return Optional.ofNullable(kiteService.placeOptionOrder(orderParams));
    }
}
