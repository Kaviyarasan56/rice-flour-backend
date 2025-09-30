package com.riceflour.shop.backend.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional customer details
    @Column(nullable = true)
    private String customerName;

    @Column(nullable = true)
    private String phone;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String address;

    // Required fields
    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 1000, nullable = true)
    private String instructions;

    // Tamil ordering fields
    @Column(nullable = false)
    private String date; // "today" or "tomorrow"

    @Column(nullable = false)
    private String slot; // "morning" or "evening"

    @Column(nullable = false)
    private Timestamp createdAt = Timestamp.from(Instant.now());

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
