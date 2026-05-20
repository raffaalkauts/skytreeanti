package com.wiredid.skytree.api;

import org.bukkit.inventory.ItemStack;
import org.bukkit.event.Event;

import java.util.List;

/**
 * Base class for all Custom Enchantments.
 */
public abstract class CustomEnchant {

    private final String id;
    private final String displayName;
    private final Rarity rarity;
    private final int maxLevel;
    private final List<org.bukkit.Material> appliesTo;

    public CustomEnchant(String id, String displayName, Rarity rarity, int maxLevel,
            List<org.bukkit.Material> appliesTo) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.maxLevel = maxLevel;
        this.appliesTo = appliesTo;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public List<org.bukkit.Material> getAppliesTo() {
        return appliesTo;
    }

    /**
     * Logic to execute when the enchantment is triggered.
     * Concrete implementations will override specific trigger methods or use this
     * generic one.
     */
    public abstract void onTrigger(Event event, int level, ItemStack item);

    /**
     * @return A descriptive string for this enchantment.
     */
    public abstract String getDescription();

    public enum Rarity {
        SIMPLE("§fSimple", 100),
        UNIQUE("§aUnique", 75),
        ELITE("§bElite", 50),
        ULTIMATE("§dUltimate", 25),
        LEGENDARY("§6Legendary", 10),
        FABLED("§4Fabled", 5);

        private final String prefix;
        private final int weight;

        Rarity(String prefix, int weight) {
            this.prefix = prefix;
            this.weight = weight;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getWeight() {
            return weight;
        }
    }
}
