package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") // allow all origins for frontend
public class OrderController {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderController(OrderRepository orderRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@Valid @RequestBody Order order) {
        // Save to DB
        Order savedOrder = orderRepository.save(order);

        // Send Telegram notification
        notificationService.sendOrderNotification(savedOrder);

        return ResponseEntity.ok(savedOrder);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
