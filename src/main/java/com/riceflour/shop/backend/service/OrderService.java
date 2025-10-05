package com.riceflour.shop.backend.service;

import com.riceflour.shop.backend.entity.Order;
import com.riceflour.shop.backend.entity.User;
import com.riceflour.shop.backend.repository.OrderRepository;
import com.riceflour.shop.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        boolean isPaidOrder = false;
        
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
            isPaidOrder = true;
        }

        Order order;
        
        // Only merge COD orders with other COD pending orders - never merge with paid orders
        if ("COD".equalsIgnoreCase(paymentMethod)) {
            Optional<Order> pendingCOD = orderRepository.findFirstByDeviceIdAndDateAndSlotAndStatusOrderByCreatedAtDesc(
                deviceId, date, slot, Order.Status.PENDING
            );
            
            if (pendingCOD.isPresent() && pendingCOD.get().getPaymentStatus() == Order.PaymentStatus.COD_PENDING) {
                // Merge with existing COD order
                order = pendingCOD.get();
                int newQty = (order.getQuantity() == null ? 0 : order.getQuantity()) + (quantity == null ? 0 : quantity);
                order.setQuantity(newQty);
                
                String existingInstr = order.getInstructions() == null ? "" : order.getInstructions();
                String newInstr = (instructions == null ? "" : instructions);
                String combined = (existingInstr + (existingInstr.isEmpty() || newInstr.isEmpty() ? "" : " | ") + newInstr).trim();
                order.setInstructions(combined.isEmpty() ? null : combined);
                
                order.setTotalPrice(totalPrice);
                orderRepository.save(order);
            } else {
                // Create new COD order
                order = createNewOrder(deviceId, user, quantity, instructions, date, slot, totalPrice, paymentMethod, paymentStatus, null, null, null);
            }
        } else {
            // Always create new order for UPI payments - never merge
            order = createNewOrder(deviceId, user, quantity, instructions, date, slot, totalPrice, paymentMethod, paymentStatus, razorpayOrderId, razorpayPaymentId, razorpaySignature);
        }

        // Send Telegram notification
        try { 
            notificationService.sendOrderNotification(order); 
        } catch (Exception ignored) {
            System.err.println("Failed to send notification: " + ignored.getMessage());
        }

        return order;
    }

    private Order createNewOrder(String deviceId, User user, Integer quantity, String instructions,
                                String date, String slot, Double totalPrice, String paymentMethod,
                                Order.PaymentStatus paymentStatus, String razorpayOrderId, 
                                String razorpayPaymentId, String razorpaySignature) {
        Order order = new Order();
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
        
        return orderRepository.save(order);
    }

    private void validateSlotCutoff(String date, String slot) throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        
        // Calculate actual delivery date
        LocalDate deliveryDate = "today".equalsIgnoreCase(date) ? today : today.plusDays(1);
        
        // Check if delivery date is in the past
        if (deliveryDate.isBefore(today)) {
            throw new Exception("Cannot order for past dates");
        }
        
        // Morning slot cutoff: 12:00 AM (midnight) of delivery date
        if ("morning".equalsIgnoreCase(slot)) {
            if (deliveryDate.equals(today) && currentTime.isAfter(LocalTime.MIDNIGHT)) {
                throw new Exception("காலை ஸ்லாட் முடிந்துவிட்டது (12 AM க்கு பிறகு)");
            }
        }
        
        // Evening slot cutoff: 10:00 AM of delivery date
        if ("evening".equalsIgnoreCase(slot)) {
            if (deliveryDate.equals(today) && currentTime.isAfter(LocalTime.of(10, 0))) {
                throw new Exception("மாலை ஸ்லாட் முடிந்துவிட்டது (10 AM க்கு பிறகு)");
            }
        }
    }
}