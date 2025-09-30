package com.riceflour.shop.backend.repository;

import com.riceflour.shop.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Basic CRUD operations are enough
}
