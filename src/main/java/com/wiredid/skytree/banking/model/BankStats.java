package com.wiredid.skytree.banking.model;

import java.util.UUID;

/**
 * Banking statistics for a player
 */
public class BankStats {

    private final UUID playerId;
    private final long currentBalance;
    private final long totalDeposited;
    private final long totalWithdrawn;
    private final long totalInterestEarned;
    private final long totalTransfers;
    private final long totalFeesPaid;
    private final long accountAge; // In milliseconds

    public BankStats(UUID playerId, long currentBalance, long totalDeposited, long totalWithdrawn,
            long totalInterestEarned, long totalTransfers, long totalFeesPaid, long accountAge) {
        this.playerId = playerId;
        this.currentBalance = currentBalance;
        this.totalDeposited = totalDeposited;
        this.totalWithdrawn = totalWithdrawn;
        this.totalInterestEarned = totalInterestEarned;
        this.totalTransfers = totalTransfers;
        this.totalFeesPaid = totalFeesPaid;
        this.accountAge = accountAge;
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public long getCurrentBalance() {
        return currentBalance;
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

    public long getTotalTransfers() {
        return totalTransfers;
    }

    public long getTotalFeesPaid() {
        return totalFeesPaid;
    }

    public long getAccountAge() {
        return accountAge;
    }

    public long getNetProfit() {
        return (totalDeposited + totalInterestEarned) - (totalWithdrawn + totalFeesPaid);
    }

    @Override
    public String toString() {
        return "BankStats{" +
                "playerId=" + playerId +
                ", currentBalance=" + currentBalance +
                ", netProfit=" + getNetProfit() +
                '}';
    }
}
