package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.service.EncryptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final OrderRepository orderRepository;
    private final EncryptionService encryptionService;

    public AdminController(OrderRepository orderRepository, EncryptionService encryptionService) {
        this.orderRepository = orderRepository;
        this.encryptionService = encryptionService;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        List<Order> orders;
        
        if (status != null && !status.isEmpty()) {
            try {
                Order.Status orderStatus = Order.Status.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus);
            } catch (IllegalArgumentException e) {
                orders = orderRepository.findAll();
            }
        } else {
            orders = orderRepository.findAll();
        }
        
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("deviceId", order.getDeviceId());
            orderMap.put("quantity", order.getQuantity());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("date", order.getDate());
            orderMap.put("slot", order.getSlot());
            orderMap.put("status", order.getStatus().name());
            orderMap.put("paymentStatus", order.getPaymentStatus().name());
            orderMap.put("paymentMethod", order.getPaymentMethod());
            orderMap.put("instructions", order.getInstructions());
            orderMap.put("createdAt", order.getCreatedAt());
            orderMap.put("processingAt", order.getProcessingAt());
            orderMap.put("outForDeliveryAt", order.getOutForDeliveryAt());
            orderMap.put("deliveredAt", order.getDeliveredAt());
            orderMap.put("cancelledAt", order.getCancelledAt());
            
            if (order.getUser() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("name", order.getUser().getName());
                userMap.put("village", order.getUser().getVillage());
                
                String encryptedPhone = order.getUser().getPhoneEncrypted();
                if (encryptedPhone != null) {
                    try {
                        userMap.put("phone", encryptionService.decrypt(encryptedPhone));
                    } catch (Exception e) {
                        userMap.put("phone", "***");
                    }
                }
                orderMap.put("user", userMap);
            }
            
            return orderMap;
        }).collect(Collectors.toList());
        
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            result = result.stream()
                .filter(order -> {
                    String orderStr = order.toString().toLowerCase();
                    return orderStr.contains(searchLower);
                })
                .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/orders/{orderId}")
    @Transactional
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> update) {
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Order order = orderOpt.get();
        String newStatus = update.get("status");
        
        try {
            Order.Status status = Order.Status.valueOf(newStatus.toUpperCase());
            order.setStatus(status); // This will auto-update timestamps
            
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush(); // Force immediate database sync
            
            // Return simplified response to avoid lazy loading issues
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedOrder.getId());
            response.put("status", savedOrder.getStatus().name());
            response.put("processingAt", savedOrder.getProcessingAt());
            response.put("outForDeliveryAt", savedOrder.getOutForDeliveryAt());
            response.put("deliveredAt", savedOrder.getDeliveredAt());
            response.put("cancelledAt", savedOrder.getCancelledAt());
            response.put("message", "Status updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + newStatus);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Update failed: " + e.getMessage());
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        Instant now = Instant.now();
        Instant todayStart = now.truncatedTo(ChronoUnit.DAYS);
        Instant weekStart = now.minus(7, ChronoUnit.DAYS);
        
        Long ordersToday = orderRepository.countOrdersSince(todayStart);
        analytics.put("ordersToday", ordersToday);
        
        Long ordersWeek = orderRepository.countOrdersSince(weekStart);
        analytics.put("ordersThisWeek", ordersWeek);
        
        Long totalOrders = orderRepository.count();
        analytics.put("totalOrders", totalOrders);
        
        List<Object[]> topCustomersRaw = orderRepository.findTopCustomers();
        List<Map<String, Object>> topCustomers = topCustomersRaw.stream()
            .limit(5)
            .map(arr -> {
                Map<String, Object> customer = new HashMap<>();
                customer.put("deviceId", arr[0]);
                customer.put("orderCount", arr[1]);
                return customer;
            })
            .collect(Collectors.toList());
        analytics.put("topCustomers", topCustomers);
        
        Map<String, Long> statusBreakdown = new HashMap<>();
        for (Order.Status status : Order.Status.values()) {
            long count = orderRepository.findByStatusOrderByCreatedAtDesc(status).size();
            statusBreakdown.put(status.name(), count);
        }
        analytics.put("statusBreakdown", statusBreakdown);
        
        return ResponseEntity.ok(analytics);
    }
}