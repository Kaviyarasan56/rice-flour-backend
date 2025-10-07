package com.riceflour.shop.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orderstablefinalandfixed1")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    public enum Status { 
        PENDING, 
        PROCESSING, 
        OUT_FOR_DELIVERY, 
        DELIVERED, 
        CANCELLED 
    }
    
    public enum PaymentStatus { 
        PENDING, 
        PAID, 
        COD_PENDING, 
        FAILED 
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Integer unitPrice = 25;

    @Column(nullable = false)
    private Double totalPrice = 25.0;

    @Column(length = 1000, nullable = true)
    private String instructions;

    @Column(nullable = false)
    private String date;

    @Column(nullable = false)
    private String slot;

    @Column(nullable = true, length = 50)
    private String paymentMethod = "COD";

    @Column(nullable = true)
    private String razorpayOrderId;

    @Column(nullable = true)
    private String razorpayPaymentId;

    @Column(nullable = true)
    private String razorpaySignature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = true)
    private Instant processingAt;

    @Column(nullable = true)
    private Instant outForDeliveryAt;

    @Column(nullable = true)
    private Instant deliveredAt;

    @Column(nullable = true)
    private Instant cancelledAt;

    @Column(nullable = true, length = 500)
    private String statusNote;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { 
        this.status = status;
        Instant now = Instant.now();
        if (status == Status.PROCESSING && this.processingAt == null) {
            this.processingAt = now;
        } else if (status == Status.OUT_FOR_DELIVERY && this.outForDeliveryAt == null) {
            this.outForDeliveryAt = now;
        } else if (status == Status.DELIVERED && this.deliveredAt == null) {
            this.deliveredAt = now;
        } else if (status == Status.CANCELLED && this.cancelledAt == null) {
            this.cancelledAt = now;
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getProcessingAt() { return processingAt; }
    public void setProcessingAt(Instant processingAt) { this.processingAt = processingAt; }

    public Instant getOutForDeliveryAt() { return outForDeliveryAt; }
    public void setOutForDeliveryAt(Instant outForDeliveryAt) { this.outForDeliveryAt = outForDeliveryAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getStatusNote() { return statusNote; }
    public void setStatusNote(String statusNote) { this.statusNote = statusNote; }
}
