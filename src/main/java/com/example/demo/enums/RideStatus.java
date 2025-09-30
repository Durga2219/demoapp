package com.example.demo.enums;

public enum RideStatus {
    ACTIVE("Active"),
    FULL("Full"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    IN_PROGRESS("In Progress");

    private final String displayName;

    RideStatus(String displayName) {
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
