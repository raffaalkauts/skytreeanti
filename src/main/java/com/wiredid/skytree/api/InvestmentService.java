package com.wiredid.skytree.api;

import com.wiredid.skytree.model.Investment;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing the investment system (stocks and bonds)
 */
public interface InvestmentService {

    /**
     * Buy stock shares
     * 
     * @param playerId Player UUID
     * @param stockId  Stock ticker (e.g., "emerald_corp")
     * @param shares   Number of shares to buy
     * @return true if purchase successful
     */
    boolean buyStock(UUID playerId, String stockId, int shares);

    /**
     * Sell stock shares
     * 
     * @param playerId Player UUID
     * @param stockId  Stock ticker
     * @param shares   Number of shares to sell
     * @return true if sale successful
     */
    boolean sellStock(UUID playerId, String stockId, int shares);

    /**
     * Purchase a bond
     * 
     * @param playerId Player UUID
     * @param bondId   Bond type (e.g., "short_term")
     * @param amount   Amount to invest
     * @return Investment object if successful, null otherwise
     */
    Investment purchaseBond(UUID playerId, String bondId, double amount);

    /**
     * Claim matured bond
     * 
     * @param playerId     Player UUID
     * @param investmentId Investment UUID
     * @return true if claim successful
     */
    boolean claimBond(UUID playerId, UUID investmentId);

    /**
     * Get current stock price
     * 
     * @param stockId Stock ticker
     * @return Current price per share
     */
    double getStockPrice(String stockId);

    /**
     * Get all player investments
     * 
     * @param playerId Player UUID
     * @return List of active investments
     */
    List<Investment> getPlayerInvestments(UUID playerId);

    /**
     * Get player's stock shares for a specific stock
     * 
     * @param playerId Player UUID
     * @param stockId  Stock ticker
     * @return Number of shares owned
     */
    int getPlayerShares(UUID playerId, String stockId);

    /**
     * Update all stock prices (called by scheduled task)
     */
    void updateStockPrices();

    /**
     * Calculate total portfolio value
     * 
     * @param playerId Player UUID
     * @return Total value of all investments
     */
    double calculatePortfolioValue(UUID playerId);

    /**
     * Reload configuration from file
     */
    void reload();
}
