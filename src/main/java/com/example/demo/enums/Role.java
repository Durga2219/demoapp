package com.example.demo.enums;

public enum Role {
    PASSENGER("Passenger"),
    DRIVER("Driver"),
    BOTH("Driver & Passenger"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
