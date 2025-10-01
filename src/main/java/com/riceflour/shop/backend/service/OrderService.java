package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.entity.User;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Place or merge an order:
     * - If registered user exists for device -> link user
     * - If PENDING order exists for same device+date+slot -> merge (add quantity & append instructions)
     * - Else create new order
     */
    @Transactional
    public Order placeOrMergeOrder(String deviceId, Integer quantity, String instructions, String date, String slot) {
        Optional<User> maybeUser = userRepository.findByDeviceId(deviceId);
        User user = maybeUser.orElse(null);

        // Try find pending exact slot order for device
        Optional<Order> pendingSame = orderRepository.findFirstByDeviceIdAndDateAndSlotAndStatusOrderByCreatedAtDesc(deviceId, date, slot, Order.Status.PENDING);

        Order order;
        if (pendingSame.isPresent()) {
            // Merge: sum quantities and append instructions
            order = pendingSame.get();
            int newQty = (order.getQuantity() == null ? 0 : order.getQuantity()) + (quantity == null ? 0 : quantity);
            order.setQuantity(newQty);
            String existingInstr = order.getInstructions() == null ? "" : order.getInstructions();
            String newInstr = (instructions == null ? "" : instructions);
            String combined = (existingInstr + (existingInstr.isEmpty() || newInstr.isEmpty() ? "" : " | ") + newInstr).trim();
            order.setInstructions(combined.isEmpty() ? null : combined);
            if (user != null) order.setUser(user);
            order.recalcTotal();
            orderRepository.save(order);
        } else {
            // Create new order
            order = new Order();
            order.setDeviceId(deviceId);
            order.setQuantity(quantity == null ? 1 : quantity);
            order.setInstructions(instructions);
            order.setDate(date);
            order.setSlot(slot);
            if (user != null) order.setUser(user);
            order.recalcTotal();
            orderRepository.save(order);
        }

        // Send notification (non-blocking)
        try { notificationService.sendOrderNotification(order); } catch (Exception ignored) {}

        return order;
    }
}
