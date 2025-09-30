package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public static class OrderInput {
        public Integer quantity;
        public String instructions;
        public String address;
        public String date; // "today" or "tomorrow"
        public String slot; // "morning" or "evening"
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderInput input) {
        try {
            // --- Validations ---
            if (input.quantity == null || input.quantity < 1) {
                return ResponseEntity.badRequest().body("Quantity must be at least 1");
            }
            if (input.date == null || !(input.date.equals("today") || input.date.equals("tomorrow"))) {
                return ResponseEntity.badRequest().body("Date must be 'today' or 'tomorrow'");
            }
            if (input.slot == null || !(input.slot.equals("morning") || input.slot.equals("evening"))) {
                return ResponseEntity.badRequest().body("Slot must be 'morning' or 'evening'");
            }

            // --- Time restriction ---
            LocalTime now = LocalTime.now();
            if (input.date.equals("today")) {
                if (input.slot.equals("morning") && now.isAfter(LocalTime.of(10, 0))) {
                    return ResponseEntity.badRequest().body("காலை நேரம் முடிந்தது. நாளை தேர்ந்தெடுக்கவும்.");
                }
                if (input.slot.equals("evening") && now.isAfter(LocalTime.of(17, 0))) {
                    return ResponseEntity.badRequest().body("இன்று மாலை நேரம் முடிந்தது. நாளை தேர்ந்தெடுக்கவும்.");
                }
            }

            // --- Save Order ---
            Order order = new Order();
            order.setQuantity(input.quantity);
            order.setInstructions(input.instructions);
            order.setAddress(input.address);
            order.setDate(input.date);
            order.setSlot(input.slot);

            Order savedOrder = orderRepository.save(order);
            System.out.println("Order saved with ID: " + savedOrder.getId());

            // --- Send Telegram notification safely ---
            notificationService.sendOrderNotification(savedOrder);

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            // Catch any unexpected errors
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
