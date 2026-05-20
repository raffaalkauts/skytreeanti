package com.wiredid.skytree.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ThirstService {
    void startTask();

    void stopTask();

    double getThirst(Player player);

    void setThirst(Player player, double amount);

    void addThirst(Player player, double amount);

    void handleConsumption(Player player, ItemStack item);

    void registerPlayer(Player player);

    void unregisterPlayer(Player player);
}

