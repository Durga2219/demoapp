package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "balance", nullable = false)
    private Double balance = 0.0;

    @Column(name = "total_earnings", nullable = false)
    private Double totalEarnings = 0.0;

    @Column(name = "total_withdrawn", nullable = false)
    private Double totalWithdrawn = 0.0;

    @Column(name = "pending_amount", nullable = false)
    private Double pendingAmount = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Wallet() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Wallet(User user) {
        this();
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(Double totalEarnings) {
        this.totalEarnings = totalEarnings;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getTotalWithdrawn() {
        return totalWithdrawn;
    }

    public void setTotalWithdrawn(Double totalWithdrawn) {
        this.totalWithdrawn = totalWithdrawn;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(Double pendingAmount) {
        this.pendingAmount = pendingAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public void addEarnings(Double amount) {
        this.balance += amount;
        this.totalEarnings += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void deductAmount(Double amount) {
        this.balance -= amount;
        this.totalWithdrawn += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getAvailableBalance() {
        return this.balance - this.pendingAmount;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", balance=" + balance +
                ", totalEarnings=" + totalEarnings +
                ", totalWithdrawn=" + totalWithdrawn +
                ", pendingAmount=" + pendingAmount +
                '}';
    }
}