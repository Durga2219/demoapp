package com.example.demo.entity;

public enum PaymentStatus {
    CREATED,        // Payment order created but not yet paid
    PENDING,        // Payment initiated but not completed
    SUCCESS,        // Payment completed successfully
    FAILED,         // Payment failed
    CANCELLED,      // Payment cancelled by user
    REFUNDED,       // Payment refunded
    PARTIAL_REFUND  // Partial refund processed
}