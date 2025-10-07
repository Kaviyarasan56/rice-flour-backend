package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.User;
import com.riceflour.shop.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin-auth")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    private final UserRepository userRepository;

    // Default admin credentials
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123"; // Change this in production!

    public AdminAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static class LoginRequest {
        public String username;
        public String password;
        public String deviceId;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            if (request.username == null || request.password == null) {
                return ResponseEntity.badRequest().body("Username and password required");
            }

            // Check credentials
            if (!DEFAULT_ADMIN_USERNAME.equals(request.username) || 
                !DEFAULT_ADMIN_PASSWORD.equals(request.password)) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }

            // Check if admin user exists for this device
            Optional<User> existingAdmin = userRepository.findByDeviceId(request.deviceId);
            
            User adminUser;
            if (existingAdmin.isPresent() && existingAdmin.get().getIsAdmin()) {
                adminUser = existingAdmin.get();
            } else {
                // Create new admin user
                adminUser = new User();
                adminUser.setDeviceId(request.deviceId);
                adminUser.setName("Administrator");
                adminUser.setIsAdmin(true);
                adminUser.setUsername(request.username);
                adminUser.setPasswordHash(hashPassword(request.password));
                userRepository.save(adminUser);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("isAdmin", true);
            response.put("userId", adminUser.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Login failed: " + e.getMessage());
        }
    }

    @GetMapping("/verify/{deviceId}")
    public ResponseEntity<?> verifyAdmin(@PathVariable String deviceId) {
        Optional<User> user = userRepository.findByDeviceId(deviceId);
        
        if (user.isPresent() && user.get().getIsAdmin()) {
            Map<String, Object> response = new HashMap<>();
            response.put("isAdmin", true);
            response.put("username", user.get().getUsername());
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.ok(Map.of("isAdmin", false));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        // In a real app, you'd invalidate tokens here
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}