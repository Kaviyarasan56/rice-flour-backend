package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class CleanupService {

    private final OrderRepository orderRepository;

    public CleanupService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Run every hour to expire unpaid orders past their slot cutoff
    @Scheduled(fixedRate = 1000 * 60 * 60)
    public void expireUnpaidOrdersPastCutoff() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        
        List<Order> allPending = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == Order.Status.PENDING)
            .filter(o -> o.getPaymentStatus() != Order.PaymentStatus.PAID)
            .toList();
        
        for (Order order : allPending) {
            LocalDate deliveryDate = getDeliveryDate(order);
            
            boolean shouldExpire = false;
            
            // Morning slot expires at 12 AM of delivery date
            if ("morning".equalsIgnoreCase(order.getSlot())) {
                if (today.isAfter(deliveryDate) || 
                    (today.equals(deliveryDate) && currentTime.isAfter(LocalTime.MIDNIGHT))) {
                    shouldExpire = true;
                }
            }
            
            // Evening slot expires at 10 AM of delivery date
            if ("evening".equalsIgnoreCase(order.getSlot())) {
                if (today.isAfter(deliveryDate) || 
                    (today.equals(deliveryDate) && currentTime.isAfter(LocalTime.of(10, 0)))) {
                    shouldExpire = true;
                }
            }
            
            if (shouldExpire) {
                order.setStatus(Order.Status.CANCELLED);
                orderRepository.save(order);
                System.out.println("Expired unpaid order: " + order.getId());
            }
        }
    }
    
    private LocalDate getDeliveryDate(Order order) {
        LocalDateTime orderTime = LocalDateTime.ofInstant(order.getCreatedAt(), ZoneId.of("Asia/Kolkata"));
        LocalDate orderDate = orderTime.toLocalDate();
        
        if ("today".equalsIgnoreCase(order.getDate())) {
            return orderDate;
        } else if ("tomorrow".equalsIgnoreCase(order.getDate())) {
            return orderDate.plusDays(1);
        }
        return orderDate;
    }

    // Delete very old cancelled orders (cleanup after 7 days)
    @Scheduled(fixedRate = 1000 * 60 * 60 * 24) // Daily
    public void deleteOldCancelledOrders() {
        Instant cutoff = Instant.now().minus(java.time.Duration.ofDays(7));
        List<Order> old = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == Order.Status.CANCELLED)
            .filter(o -> o.getCreatedAt().isBefore(cutoff))
            .toList();
        
        if (!old.isEmpty()) {
            System.out.println("Deleting " + old.size() + " old cancelled orders.");
            orderRepository.deleteAll(old);
        }
    }
}