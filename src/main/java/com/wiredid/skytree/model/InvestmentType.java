package com.wiredid.skytree.model;

/**
 * Investment types for the investment system
 */
public enum InvestmentType {
    /**
     * Volatile, fluctuating prices
     */
    STOCK,

    /**
     * Fixed return after duration
     */
    BOND;

    /**
     * Get type from string (case-insensitive)
     */
    public static InvestmentType fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STOCK;
        }
    }
}
