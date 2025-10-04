package com.riceflour.shop.backend.controller;

import com.riceflour.shop.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Input DTO for payment order
    public static class PaymentOrderInput {
        public String deviceId;
        public Integer quantity;
        public String date;
        public String slot;
        public Double totalAmount;
    }

    // Create Razorpay order endpoint
    @PostMapping("/create")
    public ResponseEntity<?> createPaymentOrder(@RequestBody PaymentOrderInput input) {
        try {
            if (input.totalAmount == null || input.totalAmount <= 0) {
                return ResponseEntity.badRequest().body("Invalid amount");
            }

            // Call PaymentService (which now uses hardcoded Razorpay keys)
            Map<String, Object> orderData = paymentService.createRazorpayOrder(
                    input.deviceId,
                    input.totalAmount,
                    input.date,
                    input.slot
            );

            return ResponseEntity.ok(orderData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Payment order creation failed: " + e.getMessage());
        }
    }
}
