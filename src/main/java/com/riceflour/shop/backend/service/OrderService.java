package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.entity.User;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, 
                       NotificationService notificationService, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
    }

    @Transactional
    public Order placeOrMergeOrder(String deviceId, Integer quantity, String instructions, 
                                  String date, String slot, Double totalPrice, String paymentMethod,
                                  String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) throws Exception {
        
        // Validate slot cut-off times
        validateSlotCutoff(date, slot);
        
        // Check user exists
        Optional<User> maybeUser = userRepository.findByDeviceId(deviceId);
        if (maybeUser.isEmpty()) {
            throw new Exception("User not registered. Please register first.");
        }
        User user = maybeUser.get();

        // Handle payment verification for UPI
        Order.PaymentStatus paymentStatus = Order.PaymentStatus.COD_PENDING;
        if ("UPI".equalsIgnoreCase(paymentMethod)) {
            if (razorpayOrderId == null || razorpayPaymentId == null || razorpaySignature == null) {
                throw new Exception("Payment details incomplete for UPI payment");
            }
            
            boolean isValid = paymentService.verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
            if (!isValid) {
                paymentStatus = Order.PaymentStatus.FAILED;
                throw new Exception("Payment verification failed");
            }
            paymentStatus = Order.PaymentStatus.PAID;
        }

        // Try to find existing pending order for same slot
        Optional<Order> pendingSame = orderRepository.findFirstByDeviceIdAndDateAndSlotAndStatusOrderByCreatedAtDesc(
            deviceId, date, slot, Order.Status.PENDING
        );

        Order order;
        if (pendingSame.isPresent()) {
            // Merge orders
            order = pendingSame.get();
            int newQty = (order.getQuantity() == null ? 0 : order.getQuantity()) + (quantity == null ? 0 : quantity);
            order.setQuantity(newQty);
            
            String existingInstr = order.getInstructions() == null ? "" : order.getInstructions();
            String newInstr = (instructions == null ? "" : instructions);
            String combined = (existingInstr + (existingInstr.isEmpty() || newInstr.isEmpty() ? "" : " | ") + newInstr).trim();
            order.setInstructions(combined.isEmpty() ? null : combined);
            
            // Update payment info if this is a UPI payment
            if ("UPI".equalsIgnoreCase(paymentMethod)) {
                order.setPaymentMethod(paymentMethod);
                order.setRazorpayOrderId(razorpayOrderId);
                order.setRazorpayPaymentId(razorpayPaymentId);
                order.setRazorpaySignature(razorpaySignature);
                order.setPaymentStatus(paymentStatus);
            }
            
            order.setTotalPrice(totalPrice);
            orderRepository.save(order);
        } else {
            // Create new order
            order = new Order();
            order.setDeviceId(deviceId);
            order.setUser(user);
            order.setQuantity(quantity == null ? 1 : quantity);
            order.setInstructions(instructions);
            order.setDate(date);
            order.setSlot(slot);
            order.setTotalPrice(totalPrice);
            order.setPaymentMethod(paymentMethod == null ? "COD" : paymentMethod);
            order.setPaymentStatus(paymentStatus);
            
            if ("UPI".equalsIgnoreCase(paymentMethod)) {
                order.setRazorpayOrderId(razorpayOrderId);
                order.setRazorpayPaymentId(razorpayPaymentId);
                order.setRazorpaySignature(razorpaySignature);
            }
            
            orderRepository.save(order);
        }

        // Send Telegram notification
        try { 
            notificationService.sendOrderNotification(order); 
        } catch (Exception ignored) {
            System.err.println("Failed to send notification: " + ignored.getMessage());
        }

        return order;
    }

    private void validateSlotCutoff(String date, String slot) throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        int currentHour = now.getHour();

        if ("today".equalsIgnoreCase(date)) {
            if ("morning".equalsIgnoreCase(slot) && currentHour >= 0) {
                throw new Exception("காலை ஸ்லாட் முடிந்துவிட்டது (12 AM க்கு பிறகு)");
            }
            if ("evening".equalsIgnoreCase(slot) && currentHour >= 10) {
                throw new Exception("மாலை ஸ்லாட் முடிந்துவிட்டது (10 AM க்கு பிறகு)");
            }
        }
    }
}