package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.entity.User;
import com.riceflour.shop.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserRepository userRepository;
    public UserController(UserRepository userRepository) { this.userRepository = userRepository; }

    public static class RegisterInput {
        public String deviceId;
        public String name;
        public String village;
        public String phone;
        public String otherInfo;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterInput input) {
        if (input == null || input.deviceId == null || input.deviceId.isBlank()) {
            return ResponseEntity.badRequest().body("deviceId is required");
        }
        if (input.phone == null || input.phone.isBlank()) {
            return ResponseEntity.badRequest().body("phone is required");
        }
    
        // ✅ Check if phone is already registered
        Optional<User> existingByPhone = userRepository.findByPhone(input.phone);
        if (existingByPhone.isPresent()) {
            return ResponseEntity.status(409).body("இந்த தொலைபேசி எண் ஏற்கனவே பதிவு செய்யப்பட்டுள்ளது.");
        }
    
        // ✅ If device already exists, update info
        Optional<User> existingByDevice = userRepository.findByDeviceId(input.deviceId);
        User user;
        if (existingByDevice.isPresent()) {
            user = existingByDevice.get();
            user.setName(input.name);
            user.setVillage(input.village);
            user.setPhone(input.phone);
            user.setOtherInfo(input.otherInfo);
        } else {
            user = new User();
            user.setDeviceId(input.deviceId);
            user.setName(input.name);
            user.setVillage(input.village);
            user.setPhone(input.phone);
            user.setOtherInfo(input.otherInfo);
        }
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }
    

    @GetMapping("/by-device/{deviceId}")
    public ResponseEntity<?> byDevice(@PathVariable String deviceId) {
        return userRepository.findByDeviceId(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
