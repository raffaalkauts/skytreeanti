package com.wiredid.skytree.api;

import java.util.UUID;

/**
 * Service for managing the USDT economy
 */
public interface EconomyService {

    /**
     * Gets a player's balance
     * 
     * @param uuid The player's UUID
     * @return The player's balance
     */
    double getBalance(UUID uuid);

    /**
     * Sets a player's balance
     * 
     * @param uuid   The player's UUID
     * @param amount The new balance
     */
    void setBalance(UUID uuid, double amount);

    /**
     * Adds to a player's balance
     * 
     * @param uuid   The player's UUID
     * @param amount The amount to add
     */
    void addBalance(UUID uuid, double amount);

    /**
     * Removes from a player's balance
     * 
     * @param uuid   The player's UUID
     * @param amount The amount to remove
     * @return true if successful, false if insufficient funds
     */
    boolean removeBalance(UUID uuid, double amount);

    /**
     * Transfers money between players
     * 
     * @param from   The sender's UUID
     * @param to     The receiver's UUID
     * @param amount The amount to transfer
     * @return true if successful
     */
    boolean transfer(UUID from, UUID to, double amount);

    /**
     * Formats a balance amount
     * 
     * @param amount The amount to format
     * @return Formatted string
     */
    String format(double amount);

    /**
     * Logs a transaction
     * 
     * @param uuid   Player UUID
     * @param amount Amount involved
     * @param type   Type (DEPOSIT/WITHDRAW)
     * @param reason Reason/Source
     */
    void logTransaction(UUID uuid, double amount, String type, String reason);

    /**
     * Gets recent transactions for a player
     * 
     * @param uuid Player UUID
     * @return List of transactions
     */
    java.util.List<Transaction> getTransactions(UUID uuid);

    /**
     * Gets all cached balances for leaderboard-style commands.
     *
     * @return Map of player UUID to balance
     */
    java.util.Map<UUID, Double> getAllBalances();
}
