package com.vish.fno.manage.controllers;

import com.vish.fno.manage.service.OrderService;
import com.zerodhatech.models.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/placeOrder")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> placeOrder(
            final @RequestBody Map<String, String> orderProps) {
        Optional<Order> orderResponse = orderService.placeOrder(orderProps);
        if(orderResponse.isPresent()) {
            return ResponseEntity.ok(orderResponse.get());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to place order");
    }

    @GetMapping("/orders")
    public ResponseEntity<String> orders() {
        orderService.openOrders();
        return ResponseEntity.ok("Done");
    }

    @GetMapping("/positions")
    public ResponseEntity<String> positions() {
        orderService.openPositions();
        return ResponseEntity.ok("Done");
    }
}
