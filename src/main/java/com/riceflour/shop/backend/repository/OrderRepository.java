package com.riceflour.shop.backend.repository;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.entity.Order.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find latest PENDING order for device + date + slot (for merge logic)
    Optional<Order> findFirstByDeviceIdAndDateAndSlotAndStatusOrderByCreatedAtDesc(String deviceId, String date, String slot, Status status);

    // Find pending order by device
    Optional<Order> findFirstByDeviceIdAndStatusOrderByCreatedAtDesc(String deviceId, Status status);

    // Find orders older than time (for cleanup), unregistered (user is null) and pending
    List<Order> findByUserIsNullAndStatusAndCreatedAtBefore(Status status, Instant before);
}
