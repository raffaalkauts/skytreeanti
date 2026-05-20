package com.wiredid.skytree.banking.model;

import java.util.UUID;

/**
 * Represents a player's bank account
 * All amounts stored as LONG (cents) to avoid floating-point errors
 */
public class BankAccount {

    private final UUID playerId;
    private long balance; // Stored in cents (100 = 1 USDT)
    private double interestRate; // Per minute (0.001 = 0.1% = 6% per hour)
    private long createdAt;
    private long lastInterestCalculation;

    // Statistics
    private long totalDeposited;
    private long totalWithdrawn;
    private long totalInterestEarned;

    public BankAccount(UUID playerId) {
        this.playerId = playerId;
        this.balance = 0;
        this.interestRate = 0.001; // Default 0.1% per minute
        this.createdAt = System.currentTimeMillis();
        this.lastInterestCalculation = System.currentTimeMillis();
        this.totalDeposited = 0;
        this.totalWithdrawn = 0;
        this.totalInterestEarned = 0;
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public long getBalance() {
        return balance;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastInterestCalculation() {
        return lastInterestCalculation;
    }

    public long getTotalDeposited() {
        return totalDeposited;
    }

    public long getTotalWithdrawn() {
        return totalWithdrawn;
    }

    public long getTotalInterestEarned() {
        return totalInterestEarned;
    }

    // Setters
    public void setBalance(long balance) {
        this.balance = balance;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public void setLastInterestCalculation(long lastInterestCalculation) {
        this.lastInterestCalculation = lastInterestCalculation;
    }

    public void setTotalDeposited(long totalDeposited) {
        this.totalDeposited = totalDeposited;
    }

    public void setTotalWithdrawn(long totalWithdrawn) {
        this.totalWithdrawn = totalWithdrawn;
    }

    public void setTotalInterestEarned(long totalInterestEarned) {
        this.totalInterestEarned = totalInterestEarned;
    }

    // Helper methods
    public void addBalance(long amount) {
        this.balance += amount;
    }

    public void subtractBalance(long amount) {
        this.balance -= amount;
    }

    public boolean hasBalance(long amount) {
        return this.balance >= amount;
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "playerId=" + playerId +
                ", balance=" + balance +
                ", interestRate=" + interestRate +
                '}';
    }
}
