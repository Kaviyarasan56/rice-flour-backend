package com.riceflour.shop.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders1")
public class Order {

    public enum Status { PENDING, DELIVERED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to user if registered (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    // Device id (always set by client)
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Integer unitPrice = 25; // ₹25 per unit

    @Column(nullable = false)
    private Integer totalPrice = 25; // computed

    @Column(length = 1000, nullable = true)
    private String instructions;

    // Slot/date fields
    @Column(nullable = false)
    private String date; // "today" or "tomorrow"

    @Column(nullable = false)
    private String slot; // "morning" or "evening"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = true)
    private Instant deliveredAt;

    // Helper: recalc total price based on quantity + discount rule
    public void recalcTotal() {
        int total = (unitPrice * (quantity == null ? 0 : quantity));
        if (quantity != null && quantity > 5) total = Math.max(0, total - 10); // flat ₹10 discount
        this.totalPrice = total;
    }

    // Getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; recalcTotal(); }

    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; recalcTotal(); }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
