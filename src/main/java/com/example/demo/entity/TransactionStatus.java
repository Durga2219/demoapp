package com.example.demo.entity;

public enum TransactionStatus {
    PENDING,        // Transaction initiated but not processed
    PROCESSING,     // Transaction being processed
    COMPLETED,      // Transaction completed successfully
    FAILED,         // Transaction failed
    CANCELLED,      // Transaction cancelled
    REVERSED        // Transaction reversed/refunded
}