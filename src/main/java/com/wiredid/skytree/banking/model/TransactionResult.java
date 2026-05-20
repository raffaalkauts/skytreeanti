package com.wiredid.skytree.banking.model;

/**
 * Result of a banking transaction
 * Contains success status, message, and transaction ID if successful
 */
public class TransactionResult {

    private final boolean success;
    private final String message;
    private final Transaction transaction;

    private TransactionResult(boolean success, String message, Transaction transaction) {
        this.success = success;
        this.message = message;
        this.transaction = transaction;
    }

    public static TransactionResult success(String message, Transaction transaction) {
        return new TransactionResult(true, message, transaction);
    }

    public static TransactionResult failure(String message) {
        return new TransactionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public String toString() {
        return "TransactionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
