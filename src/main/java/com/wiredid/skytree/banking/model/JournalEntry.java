package com.wiredid.skytree.banking.model;

import java.util.UUID;

/**
 * Represents a journal entry for double-entry bookkeeping
 * Each transaction creates 2+ journal entries
 */
public class JournalEntry {

    public enum AccountType {
        ASSET, // CASH_VAULT
        LIABILITY, // PLAYER_DEPOSITS
        EQUITY, // RETAINED_EARNINGS
        REVENUE, // TRANSACTION_FEES
        EXPENSE // INTEREST_EXPENSE
    }

    private final UUID journalId;
    private final UUID transactionId;
    private final long timestamp;
    private final AccountType accountType;
    private final String accountName;
    private final long debit; // In cents
    private final long credit; // In cents

    public JournalEntry(UUID journalId, UUID transactionId, long timestamp,
            AccountType accountType, String accountName, long debit, long credit) {
        this.journalId = journalId;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.accountType = accountType;
        this.accountName = accountName;
        this.debit = debit;
        this.credit = credit;
    }

    // Getters
    public UUID getJournalId() {
        return journalId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getAccountName() {
        return accountName;
    }

    public long getDebit() {
        return debit;
    }

    public long getCredit() {
        return credit;
    }

    @Override
    public String toString() {
        return "JournalEntry{" +
                "accountType=" + accountType +
                ", accountName='" + accountName + '\'' +
                ", debit=" + debit +
                ", credit=" + credit +
                '}';
    }
}
