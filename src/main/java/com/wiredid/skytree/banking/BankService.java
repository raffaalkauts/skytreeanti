package com.wiredid.skytree.banking;

import com.wiredid.skytree.banking.model.*;

import java.util.List;
import java.util.UUID;

/**
 * Banking service interface
 * Handles all banking operations with proper double-entry bookkeeping
 */
public interface BankService {

    // Account Management
    BankAccount getAccount(UUID playerId);

    boolean createAccount(UUID playerId);

    boolean hasAccount(UUID playerId);

    // Transactions
    TransactionResult deposit(UUID playerId, long amount);

    TransactionResult withdraw(UUID playerId, long amount);

    TransactionResult transfer(UUID fromPlayer, UUID toPlayer, long amount);

    // Interest
    void calculateInterest(UUID playerId);

    void calculateAllInterest();

    long getNextInterestAmount(UUID playerId);

    long getTimeUntilNextInterest(UUID playerId);

    // History & Stats
    List<Transaction> getTransactionHistory(UUID playerId, int page, int pageSize);

    BankStats getStats(UUID playerId);

    // Admin
    boolean reconcile();

    List<Transaction> getAuditTrail(UUID playerId, int days);

    void setGlobalInterestRate(double rate);

    double getGlobalInterestRate();

    // Utility
    long calculateFee(long amount);

    String formatAmount(long amount);
}
