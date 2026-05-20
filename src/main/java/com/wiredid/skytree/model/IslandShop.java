package com.wiredid.skytree.model;

import org.bukkit.Location;
import org.bukkit.Material;
import java.util.UUID;

public class IslandShop {
    private final UUID ownerUUID;
    private final Location location;
    private final Material material;
    private double buyPrice;
    private double sellPrice;
    private int stock; // Optional, but good for management

    public IslandShop(UUID ownerUUID, Location location, Material material, double buyPrice, double sellPrice) {
        this.ownerUUID = ownerUUID;
        this.location = location;
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.stock = 0;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Location getLocation() {
        return location;
    }

    public Material getMaterial() {
        return material;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
