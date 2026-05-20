package com.wiredid.skytree.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MinionInventoryHolder implements InventoryHolder {
    private final UUID minionId;

    public MinionInventoryHolder(UUID minionId) {
        this.minionId = minionId;
    }

    public UUID getMinionId() {
        return minionId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
