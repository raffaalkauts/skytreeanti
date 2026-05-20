package com.wiredid.skytree.model;

import java.util.UUID;

public class Bounty {
    private final UUID target;
    private final UUID issuer;
    private final double amount;
    private final long timestamp;

    public Bounty(UUID target, UUID issuer, double amount) {
        this.target = target;
        this.issuer = issuer;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for loading from persistence
    public Bounty(UUID target, UUID issuer, double amount, long timestamp) {
        this.target = target;
        this.issuer = issuer;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public UUID getTarget() {
        return target;
    }

    public UUID getIssuer() {
        return issuer;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
