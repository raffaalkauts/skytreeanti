package com.wiredid.skytree.model;

import org.bukkit.Material;

/**
 * Shop transaction record for history tracking
 */
public class ShopTransaction {
    private long timestamp;
    private TransactionType type;
    private Material item;
    private String itemId; // For custom items
    private int quantity;
    private double price;
    private double totalCost;

    public enum TransactionType {
        BUY,
        SELL
    }

    public ShopTransaction(TransactionType type, Material item, int quantity, double price) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.item = item;
        this.itemId = null;
        this.quantity = quantity;
        this.price = price;
        this.totalCost = price * quantity;
    }

    public ShopTransaction(TransactionType type, String itemId, int quantity, double price) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.item = null;
        this.itemId = itemId;
        this.quantity = quantity;
        this.price = price;
        this.totalCost = price * quantity;
    }

    // Getters
    public long getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public Material getItem() {
        return item;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public double getTotalCost() {
        return totalCost;
    }

    /**
     * Get display name for item
     */
    public String getDisplayName() {
        if (itemId != null) {
            return itemId;
        }
        return item != null ? item.name().replace("_", " ") : "Unknown";
    }
}
