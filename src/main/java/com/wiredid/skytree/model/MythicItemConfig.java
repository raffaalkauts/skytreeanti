package com.wiredid.skytree.model;

import org.bukkit.potion.PotionEffectType;
import java.util.HashMap;
import java.util.Map;

/**
 * POJO for caching Mythic Item logic to avoid constant config lookups.
 * Follows world-class performance standards.
 */
public class MythicItemConfig {
    private final Map<String, Map<PotionEffectType, Integer>> armorSetEffects = new HashMap<>();
    private final Map<String, Double> chances = new HashMap<>();

    public void addArmorSetEffect(String prefix, PotionEffectType type, int amplifier) {
        armorSetEffects.computeIfAbsent(prefix, k -> new HashMap<>()).put(type, amplifier);
    }

    public Map<PotionEffectType, Integer> getArmorSetEffects(String prefix) {
        return armorSetEffects.get(prefix);
    }

    public void setChance(String key, double chance) {
        chances.put(key, chance);
    }

    public double getChance(String key, double defaultValue) {
        return chances.getOrDefault(key, defaultValue);
    }

    public void clear() {
        armorSetEffects.clear();
        chances.clear();
    }
}
