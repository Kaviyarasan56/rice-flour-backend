package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.User;
import com.riceflour.shop.backend.repository.UserRepository;
import com.riceflour.shop.backend.service.EncryptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    public UserController(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public static class RegisterInput {
        public String deviceId;
        public String name;
        public String village;
        public String phone;
        public String otherInfo;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterInput input) {
        try {
            if (input == null || input.deviceId == null || input.deviceId.isBlank()) {
                return ResponseEntity.badRequest().body("deviceId is required");
            }
            if (input.phone == null || input.phone.isBlank()) {
                return ResponseEntity.badRequest().body("phone is required");
            }
        
            // Encrypt phone number
            String encryptedPhone = encryptionService.encrypt(input.phone);
            
            // Check if phone already registered (check all encrypted phones)
            Optional<User> existingByPhone = userRepository.findByPhoneEncrypted(encryptedPhone);
            if (existingByPhone.isPresent()) {
                return ResponseEntity.status(409).body("இந்த தொலைபேசி எண் ஏற்கனவே பதிவு செய்யப்பட்டுள்ளது.");
            }
        
            Optional<User> existingByDevice = userRepository.findByDeviceId(input.deviceId);
            User user;
            if (existingByDevice.isPresent()) {
                user = existingByDevice.get();
                user.setName(input.name);
                user.setVillage(input.village);
                user.setPhoneEncrypted(encryptedPhone);
                user.setOtherInfo(input.otherInfo);
            } else {
                user = new User();
                user.setDeviceId(input.deviceId);
                user.setName(input.name);
                user.setVillage(input.village);
                user.setPhoneEncrypted(encryptedPhone);
                user.setOtherInfo(input.otherInfo);
                user.setIsAdmin(false);
            }
            userRepository.save(user);
            
            // Return user data without sensitive info
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("village", user.getVillage());
            response.put("deviceId", user.getDeviceId());
            response.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Registration failed: " + e.getMessage());
        }
    }

    @GetMapping("/by-device/{deviceId}")
    public ResponseEntity<?> byDevice(@PathVariable String deviceId) {
        try {
            Optional<User> userOpt = userRepository.findByDeviceId(deviceId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            
            // Return user without encrypted phone
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("village", user.getVillage());
            response.put("deviceId", user.getDeviceId());
            response.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching user: " + e.getMessage());
        }
    }
}