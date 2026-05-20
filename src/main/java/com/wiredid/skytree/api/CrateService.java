package com.wiredid.skytree.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;

public interface CrateService {

    /**
     * Get all loaded crate types
     */
    Map<String, CrateData> getCrates();

    /**
     * Get a specific crate by ID
     */
    CrateData getCrate(String id);

    /**
     * Give a crate key to a player
     */
    void giveKey(Player player, String crateId, int amount);

    /**
     * Check if a player has a key for a crate
     */
    boolean hasKey(Player player, String crateId);

    /**
     * Consume a key and return a random reward
     */
    CrateReward openCrate(Player player, String crateId);

    /**
     * Reload crates from config
     */
    void reload();

    class CrateData {
        private final String id;
        private final String name;
        private final List<CrateReward> rewards;
        private final ItemStack keyItem;

        public CrateData(String id, String name, List<CrateReward> rewards, ItemStack keyItem) {
            this.id = id;
            this.name = name;
            this.rewards = rewards;
            this.keyItem = keyItem;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<CrateReward> getRewards() {
            return rewards;
        }

        public ItemStack getKeyItem() {
            return keyItem;
        }
    }

    class CrateReward {
        private final double chance;
        private final String displayName;
        private final ItemStack item;
        private final String command; // Optional command to run
        private final double money; // Optional money reward

        public CrateReward(double chance, String displayName, ItemStack item, String command, double money) {
            this.chance = chance;
            this.displayName = displayName;
            this.item = item;
            this.command = command;
            this.money = money;
        }

        public double getChance() {
            return chance;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ItemStack getItem() {
            return item;
        }

        public String getCommand() {
            return command;
        }

        public double getMoney() {
            return money;
        }
    }
}
