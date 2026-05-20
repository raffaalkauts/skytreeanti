package com.wiredid.skytree.api;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/**
 * Represents a Buy Order (Request) in the Auction House.
 * A player requests a specific item and quantity for a set price.
 */
public class AuctionOrder {

    private final UUID id;
    private final UUID buyer;
    private final ItemStack requestedItem; // The item type/meta requested
    private final double pricePerItem;
    private final int totalQuantity;
    private int filledQuantity;
    private int claimedQuantity; // Amount claimed by buyer
    private final long createdTime;
    private long expiryTime;
    private final boolean strictMatch; // If true, fulfillment must match ItemMeta (Enchants, etc.)

    // Default Constructor (New Order)
    public AuctionOrder(UUID buyer, ItemStack requestedItem, double pricePerItem, int totalQuantity,
            long durationMillis, boolean strictMatch) {
        this(UUID.randomUUID(), buyer, requestedItem, pricePerItem, totalQuantity, 0, 0, System.currentTimeMillis(),
                System.currentTimeMillis() + durationMillis, strictMatch);
    }

    // Reconstruction Constructor (Load from DB)
    public AuctionOrder(UUID id, UUID buyer, ItemStack requestedItem, double pricePerItem, int totalQuantity,
            int filledQuantity, int claimedQuantity, long createdTime, long expiryTime, boolean strictMatch) {
        this.id = id;
        this.buyer = buyer;
        this.requestedItem = requestedItem.clone();
        this.pricePerItem = pricePerItem;
        this.totalQuantity = totalQuantity;
        this.filledQuantity = filledQuantity;
        this.claimedQuantity = claimedQuantity;
        this.createdTime = createdTime;
        this.expiryTime = expiryTime;
        this.strictMatch = strictMatch;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBuyer() {
        return buyer;
    }

    public ItemStack getRequestedItem() {
        return requestedItem.clone();
    }

    public double getPricePerItem() {
        return pricePerItem;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getFilledQuantity() {
        return filledQuantity;
    }

    public int getClaimedQuantity() {
        return claimedQuantity;
    }

    public int getUnclaimedQuantity() {
        return filledQuantity - claimedQuantity;
    }

    public void setFilledQuantity(int filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    public void setClaimedQuantity(int claimedQuantity) {
        this.claimedQuantity = claimedQuantity;
    }

    public void addFilled(int amount) {
        this.filledQuantity += amount;
    }

    public void addClaimed(int amount) {
        this.claimedQuantity += amount;
    }

    public int getRemainingQuantity() {
        return totalQuantity - filledQuantity;
    }

    public boolean isFilled() {
        return filledQuantity >= totalQuantity;
    }

    public boolean isFullyClaimed() {
        return claimedQuantity >= totalQuantity;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public boolean isStrictMatch() {
        return strictMatch;
    }
}
