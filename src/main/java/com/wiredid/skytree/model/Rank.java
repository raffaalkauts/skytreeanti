package com.wiredid.skytree.model;

/**
 * Player rank system with 6 tiers
 */
public enum Rank {
    IOLITE("§f§lIOLITE", 1.0, 0, 0),
    BERYL("§b§lBERYL", 1.1, 2, 8),
    GARNET("§c§lGARNET", 1.25, 4, 16),
    AMETHYST("§5§lAMETHYST", 1.5, 8, 32),
    EMERALD("§a§lEMERALD", 1.75, 12, 48),
    DIVINE("§6§lDIVINE", 2.0, 20, 112),
    ADMIN("§c§lADMIN", 3.0, 50, 500),
    CO_OWNER("§b§lCO-OWNER", 4.0, 100, 1000),
    DEVELOPER("§d§lDEVELOPER", 4.5, 150, 5000),
    OWNER("§4§lOWNER", 5.0, 256, 10000);

    private final String prefix;
    private final double multiplier;
    private final int memberBonus;
    private final int spawnerBonus;

    Rank(String prefix, double multiplier, int memberBonus, int spawnerBonus) {
        this.prefix = prefix;
        this.multiplier = multiplier;
        this.memberBonus = memberBonus;
        this.spawnerBonus = spawnerBonus;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDisplayName() {
        return prefix;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public int getMemberBonus() {
        return memberBonus;
    }

    public int getSpawnerBonus() {
        return spawnerBonus;
    }

    public boolean isAtLeast(Rank other) {
        return this.ordinal() >= other.ordinal();
    }
}
