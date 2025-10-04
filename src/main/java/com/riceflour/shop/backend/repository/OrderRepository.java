package com.riceflour.shop.backend.repository;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.entity.Order.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findFirstByDeviceIdAndDateAndSlotAndStatusOrderByCreatedAtDesc(
        String deviceId, String date, String slot, Status status
    );

    Optional<Order> findFirstByDeviceIdAndStatusOrderByCreatedAtDesc(String deviceId, Status status);

    List<Order> findByUserIsNullAndStatusAndCreatedAtBefore(Status status, Instant before);
    
    List<Order> findByDateAndSlotAndStatus(String date, String slot, Status status);
}