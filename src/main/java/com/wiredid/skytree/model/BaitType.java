package com.wiredid.skytree.model;

/**
 * Bait types for fishing system
 */
public enum BaitType {
    /**
     * +10% Common fish chance
     */
    WORM(0.10, 0, 0, 0),

    /**
     * +20% Uncommon fish chance
     */
    MINNOW(0, 0.20, 0, 0),

    /**
     * +30% Rare fish chance
     */
    SQUID(0, 0, 0.30, 0),

    /**
     * +50% Legendary fish chance
     */
    ENCHANTED(0, 0, 0, 0.50);

    private final double commonBonus;
    private final double uncommonBonus;
    private final double rareBonus;
    private final double legendaryBonus;

    BaitType(double commonBonus, double uncommonBonus, double rareBonus, double legendaryBonus) {
        this.commonBonus = commonBonus;
        this.uncommonBonus = uncommonBonus;
        this.rareBonus = rareBonus;
        this.legendaryBonus = legendaryBonus;
    }

    public double getCommonBonus() {
        return commonBonus;
    }

    public double getUncommonBonus() {
        return uncommonBonus;
    }

    public double getRareBonus() {
        return rareBonus;
    }

    public double getLegendaryBonus() {
        return legendaryBonus;
    }

    /**
     * Get bait from string (case-insensitive)
     */
    public static BaitType fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
