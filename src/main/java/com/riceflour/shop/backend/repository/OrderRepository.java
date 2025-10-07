package com.riceflour.shop.backend.repository;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.entity.Order.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    // New methods for admin and user tracking
    List<Order> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    
    List<Order> findByStatusOrderByCreatedAtDesc(Status status);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersSince(@Param("startDate") Instant startDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate")
    Long countOrdersSince(@Param("startDate") Instant startDate);
    
    @Query("SELECT o.deviceId, COUNT(o) as orderCount FROM Order o GROUP BY o.deviceId ORDER BY orderCount DESC")
    List<Object[]> findTopCustomers();
}