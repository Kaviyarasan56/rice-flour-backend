package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

@Component
public class CleanupService {

    private final OrderRepository orderRepository;

    public CleanupService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Run every 6 hours and delete unregistered pending orders older than 48 hours
    @Scheduled(fixedRate = 1000 * 60 * 60 * 6)
    public void cleanupOldUnregisteredPendingOrders() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(48));
        List<Order> old = orderRepository.findByUserIsNullAndStatusAndCreatedAtBefore(Order.Status.PENDING, cutoff);
        if (!old.isEmpty()) {
            System.out.println("Cleaning up " + old.size() + " unregistered old orders.");
            orderRepository.deleteAll(old);
        }
    }
}
