package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.service.OrderService;
import com.riceflour.shop.backend.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public static class OrderInput {
        public String deviceId;
        public Integer quantity;
        public String instructions;
        public String date; // "today" or "tomorrow"
        public String slot; // "morning" or "evening"
    }

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderInput input) {
        if (input == null || input.deviceId == null || input.deviceId.isBlank()) {
            return ResponseEntity.badRequest().body("deviceId is required");
        }
        if (input.quantity != null && input.quantity < 1) {
            return ResponseEntity.badRequest().body("Quantity must be at least 1");
        }
        if (input.date == null || !(input.date.equals("today") || input.date.equals("tomorrow"))) {
            return ResponseEntity.badRequest().body("Date must be 'today' or 'tomorrow'");
        }
        if (input.slot == null || !(input.slot.equals("morning") || input.slot.equals("evening"))) {
            return ResponseEntity.badRequest().body("Slot must be 'morning' or 'evening'");
        }

        try {
            Order order = orderService.placeOrMergeOrder(input.deviceId, input.quantity, input.instructions, input.date, input.slot);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    @GetMapping
    public List<Order> all() {
        return orderRepository.findAll();
    }
}
