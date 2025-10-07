package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.entity.User;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-orders")
@CrossOrigin(origins = "*")
public class UserOrdersController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public UserOrdersController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<?> getUserOrders(@PathVariable String deviceId) {
        Optional<User> maybeUser = userRepository.findByDeviceId(deviceId);
        if (maybeUser.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        User user = maybeUser.get();
        
        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> o.getDeviceId().equals(deviceId))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .collect(Collectors.toList());

        List<Map<String, Object>> response = orders.stream()
            .map(this::orderToMap)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/track/{orderId}")
    public ResponseEntity<?> trackOrder(@PathVariable Long orderId, @RequestParam String deviceId) {
        Optional<Order> maybeOrder = orderRepository.findById(orderId);
        if (maybeOrder.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = maybeOrder.get();
        
        // Verify this order belongs to the device
        if (!order.getDeviceId().equals(deviceId)) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        return ResponseEntity.ok(orderToMap(order));
    }

    private Map<String, Object> orderToMap(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("quantity", order.getQuantity());
        map.put("unitPrice", order.getUnitPrice());
        map.put("totalPrice", order.getTotalPrice());
        map.put("date", order.getDate());
        map.put("slot", order.getSlot());
        map.put("status", order.getStatus().name());
        map.put("paymentStatus", order.getPaymentStatus().name());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("instructions", order.getInstructions());
        map.put("statusNote", order.getStatusNote());
        map.put("createdAt", order.getCreatedAt().toString());
        
        // Add timestamp for each status
        Map<String, Object> timeline = new HashMap<>();
        timeline.put("ordered", order.getCreatedAt().toString());
        if (order.getProcessingAt() != null) {
            timeline.put("processing", order.getProcessingAt().toString());
        }
        if (order.getOutForDeliveryAt() != null) {
            timeline.put("outForDelivery", order.getOutForDeliveryAt().toString());
        }
        if (order.getDeliveredAt() != null) {
            timeline.put("delivered", order.getDeliveredAt().toString());
        }
        if (order.getCancelledAt() != null) {
            timeline.put("cancelled", order.getCancelledAt().toString());
        }
        map.put("timeline", timeline);
        
        return map;
    }
}