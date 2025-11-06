package com.example.demo.entity;

public enum OtpType {
    REGISTRATION("Account Registration"),
    LOGIN("Login Verification"),
    PASSWORD_RESET("Password Reset");

    private final String description;

    OtpType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}