package com.wiredid.skytree.api;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/**
 * Represents a sell listing in the Auction House.
 */
public class AuctionHouse {

    private final UUID id;
    private final UUID seller;
    private final ItemStack item;
    private final double pricePerItem;
    private final long listedTime;
    private final long expiryTime;
    private int quantityRemaining;
    private boolean isExpired;

    public AuctionHouse(UUID seller, ItemStack item, double pricePerItem, long durationMillis) {
        this(UUID.randomUUID(), seller, item, pricePerItem, System.currentTimeMillis(),
                System.currentTimeMillis() + durationMillis, item.getAmount(), false);
    }

    // Reconstruction Constructor
    public AuctionHouse(UUID id, UUID seller, ItemStack item, double pricePerItem, long listedTime, long expiryTime,
            int quantityRemaining, boolean isExpired) {
        this.id = id;
        this.seller = seller;
        this.item = item.clone();
        this.pricePerItem = pricePerItem;
        this.listedTime = listedTime;
        this.expiryTime = expiryTime;
        this.quantityRemaining = quantityRemaining;
        this.isExpired = isExpired;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item.clone(); // Return clone to prevent mutation
    }

    public double getPricePerItem() {
        return pricePerItem;
    }

    public double getTotalPrice() {
        return pricePerItem * quantityRemaining;
    }

    public long getListedTime() {
        return listedTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public int getQuantityRemaining() {
        return quantityRemaining;
    }

    public void setQuantityRemaining(int quantityRemaining) {
        this.quantityRemaining = quantityRemaining;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime || isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }
}
