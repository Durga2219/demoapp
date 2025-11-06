package com.example.demo.entity;

public enum TransactionType {
    CREDIT,         // Money added to wallet (from ride earnings)
    DEBIT,          // Money deducted from wallet (withdrawals, refunds)
    WITHDRAWAL,     // Withdrawal to bank account
    REFUND,         // Refund to passenger (if needed)
    COMMISSION,     // Platform commission deduction
    BONUS,          // Bonus or incentive added
    PENALTY         // Penalty deduction
}