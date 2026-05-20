package com.wiredid.skytree.model;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Minion data including level, type, location, and storage
 */
public class MinionData {
    private UUID minionId;
    private UUID ownerId;
    private UUID islandId;
    private MinionType type;
    private int level;
    private MinionSkin skin;
    private Location location;
    private List<ItemStack> storage; // Built-in storage (27 slots)
    private boolean storageUnlocked; // Unlocked at level 5
    private long lastAction;
    private boolean active;

    public MinionData(UUID ownerId, UUID islandId, MinionType type, Location location) {
        this.minionId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.islandId = islandId;
        this.type = type;
        this.level = 1;
        this.skin = MinionSkin.STEVE;
        this.location = location;
        this.storage = new ArrayList<>(27);
        this.storageUnlocked = false;
        this.lastAction = System.currentTimeMillis();
        this.active = true;
    }

    public MinionData(UUID minionId, UUID ownerId, UUID islandId, MinionType type, int level, MinionSkin skin,
            Location location, boolean active, boolean storageUnlocked) {
        this.minionId = minionId;
        this.ownerId = ownerId;
        this.islandId = islandId;
        this.type = type;
        this.level = level;
        this.skin = skin;
        this.location = location;
        this.active = active;
        this.storageUnlocked = storageUnlocked;
        this.storage = new ArrayList<>(27);
        this.lastAction = System.currentTimeMillis();
    }

    /**
     * Get effective speed based on level
     */
    public double getEffectiveSpeed() {
        return type.getBaseSpeed() * (1.0 + (level * 0.1));
    }

    /**
     * Get effective range based on level
     */
    public int getEffectiveRange() {
        return type.getBaseRange() + (int) Math.ceil(level * 0.5);
    }

    /**
     * Upgrade minion to next level
     */
    public boolean upgrade() {
        if (level >= 10) {
            return false;
        }
        level++;
        if (level >= 5 && !storageUnlocked) {
            storageUnlocked = true;
        }
        return true;
    }

    /**
     * Add item to minion storage with stacking support
     */
    public boolean addToStorage(ItemStack item) {
        if (!storageUnlocked || item == null) {
            return false;
        }

        // 1. Try to find an existing partial stack
        for (ItemStack stored : storage) {
            if (stored != null && stored.isSimilar(item)) {
                int canAdd = stored.getMaxStackSize() - stored.getAmount();
                if (canAdd > 0) {
                    int toAdd = Math.min(item.getAmount(), canAdd);
                    stored.setAmount(stored.getAmount() + toAdd);
                    item.setAmount(item.getAmount() - toAdd);
                    if (item.getAmount() <= 0)
                        return true;
                }
            }
        }

        // 2. If item remains, try to add to a new slot
        if (item.getAmount() > 0 && storage.size() < 27) {
            storage.add(item.clone());
            item.setAmount(0);
            return true;
        }

        return false;
    }

    // Getters and setters
    public UUID getMinionId() {
        return minionId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getIslandId() {
        return islandId;
    }

    public MinionType getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public MinionSkin getSkin() {
        return skin;
    }

    public void setSkin(MinionSkin skin) {
        this.skin = skin;
    }

    public Location getLocation() {
        return location;
    }

    public List<ItemStack> getStorage() {
        return new ArrayList<>(storage);
    }

    public void setStorage(List<ItemStack> storage) {
        this.storage = new ArrayList<>(storage);
    }

    public boolean isStorageUnlocked() {
        return storageUnlocked;
    }

    public long getLastAction() {
        return lastAction;
    }

    public void updateLastAction() {
        this.lastAction = System.currentTimeMillis();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
