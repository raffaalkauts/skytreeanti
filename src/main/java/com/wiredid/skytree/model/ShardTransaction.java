package com.wiredid.skytree.model;

/**
 * Shard transaction record for balance history
 */
public class ShardTransaction {
    private long timestamp;
    private TransactionType type;
    private int amount;
    private String source; // "Daily Quest", "Achievement", "Purchase", etc.
    private int balanceAfter;

    public enum TransactionType {
        EARNED,
        SPENT,
        GIFTED_SENT,
        GIFTED_RECEIVED,
        CONVERSION
    }

    public ShardTransaction(TransactionType type, int amount, String source, int balanceAfter) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.amount = amount;
        this.source = source;
        this.balanceAfter = balanceAfter;
    }

    // Getters
    public long getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public String getSource() {
        return source;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    /**
     * Get formatted description
     */
    public String getDescription() {
        return switch (type) {
            case EARNED -> "+" + amount + " from " + source;
            case SPENT -> "-" + amount + " on " + source;
            case GIFTED_SENT -> "-" + amount + " gifted to " + source;
            case GIFTED_RECEIVED -> "+" + amount + " gifted by " + source;
            case CONVERSION -> amount + " converted via " + source;
        };
    }
}
