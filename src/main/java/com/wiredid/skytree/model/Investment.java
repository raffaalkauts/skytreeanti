package com.wiredid.skytree.model;

import java.util.UUID;

/**
 * Investment data for stocks and bonds
 */
public class Investment {
    private UUID investmentId;
    private UUID playerId;
    private InvestmentType type;
    private String assetId; // Stock ticker or bond ID
    private double amount; // Money invested
    private int shares; // For stocks
    private long purchaseTime;
    private double purchasePrice; // Price per share/bond at purchase
    private long maturityTime; // For bonds only
    private boolean active;

    /**
     * Create stock investment
     */
    public static Investment createStock(UUID playerId, String stockTicker, int shares, double pricePerShare) {
        Investment inv = new Investment();
        inv.investmentId = UUID.randomUUID();
        inv.playerId = playerId;
        inv.type = InvestmentType.STOCK;
        inv.assetId = stockTicker;
        inv.shares = shares;
        inv.purchasePrice = pricePerShare;
        inv.amount = shares * pricePerShare;
        inv.purchaseTime = System.currentTimeMillis();
        inv.maturityTime = 0;
        inv.active = true;
        return inv;
    }

    /**
     * Create bond investment
     */
    public static Investment createBond(UUID playerId, String bondId, double amount, long duration) {
        Investment inv = new Investment();
        inv.investmentId = UUID.randomUUID();
        inv.playerId = playerId;
        inv.type = InvestmentType.BOND;
        inv.assetId = bondId;
        inv.amount = amount;
        inv.shares = 0;
        inv.purchasePrice = amount;
        inv.purchaseTime = System.currentTimeMillis();
        inv.maturityTime = System.currentTimeMillis() + duration;
        inv.active = true;
        return inv;
    }

    public Investment(UUID investmentId, UUID playerId, InvestmentType type, String assetId, double amount, int shares,
            long purchaseTime, double purchasePrice, long maturityTime, boolean active) {
        this.investmentId = investmentId;
        this.playerId = playerId;
        this.type = type;
        this.assetId = assetId;
        this.amount = amount;
        this.shares = shares;
        this.purchaseTime = purchaseTime;
        this.purchasePrice = purchasePrice;
        this.maturityTime = maturityTime;
        this.active = active;
    }

    private Investment() {
    }

    /**
     * Calculate current value for stocks
     */
    public double calculateValue(double currentPrice) {
        if (type == InvestmentType.STOCK) {
            return shares * currentPrice;
        }
        return amount; // Bonds don't fluctuate
    }

    /**
     * Calculate profit/loss
     */
    public double calculateProfit(double currentPrice) {
        return calculateValue(currentPrice) - amount;
    }

    /**
     * Check if bond has matured
     */
    public boolean hasMatured() {
        if (type == InvestmentType.BOND) {
            return System.currentTimeMillis() >= maturityTime;
        }
        return false;
    }

    // Getters and setters
    public UUID getInvestmentId() {
        return investmentId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public InvestmentType getType() {
        return type;
    }

    public String getAssetId() {
        return assetId;
    }

    public double getAmount() {
        return amount;
    }

    public int getShares() {
        return shares;
    }

    public long getPurchaseTime() {
        return purchaseTime;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public long getMaturityTime() {
        return maturityTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
