package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderController(OrderRepository orderRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    // Input class for order creation
    public static class OrderInput {
        public Integer quantity;      // optional
        public String instructions;   // optional
        public String date;           // optional: "today" or "tomorrow"
        public String slot;           // optional: "morning" or "evening"
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderInput input) {
        try {
            // --- Optional Validations ---
            if (input.quantity != null && input.quantity < 1)
                return ResponseEntity.badRequest().body("Quantity must be at least 1");

            if (input.date != null && !(input.date.equals("today") || input.date.equals("tomorrow")))
                return ResponseEntity.badRequest().body("Date must be 'today' or 'tomorrow'");

            if (input.slot != null && !(input.slot.equals("morning") || input.slot.equals("evening")))
                return ResponseEntity.badRequest().body("Slot must be 'morning' or 'evening'");

            // --- Time restrictions (optional) ---
            LocalTime now = LocalTime.now();
            if ("today".equals(input.date)) {
                if ("morning".equals(input.slot) && now.isAfter(LocalTime.of(10, 0)))
                    return ResponseEntity.badRequest().body("காலை நேரம் முடிந்தது. நாளை தேர்ந்தெடுக்கவும்.");
                if ("evening".equals(input.slot) && now.isAfter(LocalTime.of(17, 0)))
                    return ResponseEntity.badRequest().body("இன்று மாலை நேரம் முடிந்தது. நாளை தேர்ந்தெடுக்கவும்.");
            }

            // --- Save Order ---
            Order order = new Order();
            order.setQuantity(input.quantity);
            order.setInstructions(input.instructions);
            order.setDate(input.date);
            order.setSlot(input.slot);

            // Optional fields can remain null
            order.setCustomerName(null);
            order.setPhone(null);
            order.setEmail(null);
            order.setAddress(null);

            // Set timestamp if not already set
            if (order.getCreatedAt() == null) {
                order.setCreatedAt(Timestamp.from(Instant.now()));
            }

            Order savedOrder = orderRepository.save(order);
            System.out.println("Order saved with ID: " + savedOrder.getId());

            // --- Send Telegram notification ---
            notificationService.sendOrderNotification(savedOrder);

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
