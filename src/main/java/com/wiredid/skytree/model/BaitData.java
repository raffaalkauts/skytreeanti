package com.wiredid.skytree.model;

/**
 * Bait data for active bait tracking
 */
public class BaitData {
    private BaitType type;
    private int quantity;
    private long equipTime;

    public BaitData(BaitType type, int quantity) {
        this.type = type;
        this.quantity = quantity;
        this.equipTime = System.currentTimeMillis();
    }

    /**
     * Consume one bait
     */
    public boolean consume() {
        if (quantity > 0) {
            quantity--;
            return true;
        }
        return false;
    }

    /**
     * Check if bait is depleted
     */
    public boolean isDepleted() {
        return quantity <= 0;
    }

    // Getters and setters
    public BaitType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getEquipTime() {
        return equipTime;
    }
}
