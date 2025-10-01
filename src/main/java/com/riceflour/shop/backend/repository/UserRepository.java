package com.riceflour.shop.backend.repository;

import com.riceflour.shop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDeviceId(String deviceId);
    Optional<User> findByPhone(String phone); // ✅ add this
}
