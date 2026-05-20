package com.wiredid.skytree.banking.model;

import java.util.UUID;

/**
 * Represents a bank transaction
 * Immutable for audit trail integrity
 */
public class Transaction {

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER,
        INTEREST,
        FEE
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REVERSED
    }

    private final UUID transactionId;
    private final long timestamp;
    private final String fromAccount; // UUID string, "CASH", or "SYSTEM"
    private final String toAccount; // UUID string, "CASH", or "SYSTEM"
    private final long amount; // In cents
    private final long fee; // In cents
    private final TransactionType type;
    private final TransactionStatus status;
    private final String reference; // Optional description

    public Transaction(UUID transactionId, long timestamp, String fromAccount, String toAccount,
            long amount, long fee, TransactionType type, TransactionStatus status, String reference) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.fee = fee;
        this.type = type;
        this.status = status;
        this.reference = reference;
    }

    // Getters only (immutable)
    public UUID getTransactionId() {
        return transactionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public long getAmount() {
        return amount;
    }

    public long getFee() {
        return fee;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + transactionId +
                ", type=" + type +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}
