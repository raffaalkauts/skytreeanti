package com.wiredid.skytree.api;

import java.time.LocalDateTime;

public class Transaction {
    private final LocalDateTime timestamp;
    private final double amount;
    private final String type; // "DEPOSIT", "WITHDRAW"
    private final String reason;

    public Transaction(LocalDateTime timestamp, double amount, String type, String reason) {
        this.timestamp = timestamp;
        this.amount = amount;
        this.type = type;
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }
}
