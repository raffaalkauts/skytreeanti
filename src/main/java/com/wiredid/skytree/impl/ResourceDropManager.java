package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ResourceDropManager {

    private final SkytreePlugin plugin;
    private final Map<String, TreeDropTable> treeDrops = new HashMap<>();
    private final Map<String, Double> crookBonuses = new HashMap<>();

    public ResourceDropManager(SkytreePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        treeDrops.clear();
        crookBonuses.clear();
        loadDrops();
    }

    private void loadDrops() {
        File file = new File(plugin.getDataFolder(), "drops.yml");
        if (!file.exists()) {
            plugin.saveResource("drops.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection trees = config.getConfigurationSection("resource_trees");
        if (trees != null) {
            for (String treeKey : trees.getKeys(false)) {
                ConfigurationSection sec = trees.getConfigurationSection(treeKey);
                if (sec == null) continue;

                TreeDropTable table = new TreeDropTable();
                table.growthTime = sec.getInt("growth_time", 30);
                table.trunk = Material.matchMaterial(sec.getString("trunk", "OAK_LOG"));
                table.leaves = Material.matchMaterial(sec.getString("leaves", "OAK_LEAVES"));

                ConfigurationSection dropsSec = sec.getConfigurationSection("drops");
                if (dropsSec != null) {
                    for (String itemKey : dropsSec.getKeys(false)) {
                        ConfigurationSection dropSec = dropsSec.getConfigurationSection(itemKey);
                        if (dropSec == null) continue;
                        DropEntry entry = new DropEntry();
                        entry.itemId = itemKey;
                        entry.chance = dropSec.getDouble("chance", 10);
                        entry.min = dropSec.getInt("min", 1);
                        entry.max = dropSec.getInt("max", 1);
                        table.drops.add(entry);
                    }
                }
                treeDrops.put(treeKey, table);
            }
        }

        ConfigurationSection bonuses = config.getConfigurationSection("crook_bonus");
        if (bonuses != null) {
            for (String crookId : bonuses.getKeys(false)) {
                crookBonuses.put(crookId.toLowerCase(), bonuses.getDouble(crookId, 0.0));
            }
        }

        plugin.getLogger().info("Loaded " + treeDrops.size() + " resource tree drop tables and "
                + crookBonuses.size() + " crook tier bonuses.");
    }

    public TreeDropTable getTreeDrop(String treeKey) {
        return treeDrops.get(treeKey);
    }

    public Map<String, TreeDropTable> getAllTreeDrops() {
        return treeDrops;
    }

    public double getCrookBonus(String crookItemId) {
        return crookBonuses.getOrDefault(crookItemId.toLowerCase(), 0.0);
    }

    public boolean isCrook(String itemId) {
        return itemId != null && crookBonuses.containsKey(itemId.toLowerCase());
    }

    /**
     * Identify tree type by leaf and trunk materials
     */
    public String identifyTreeType(Material leafType, Material trunkType) {
        for (Map.Entry<String, TreeDropTable> entry : treeDrops.entrySet()) {
            TreeDropTable table = entry.getValue();
            if (table.leaves == leafType && table.trunk == trunkType) {
                return entry.getKey();
            }
        }
        // Fallback: match by leaves only
        for (Map.Entry<String, TreeDropTable> entry : treeDrops.entrySet()) {
            TreeDropTable table = entry.getValue();
            if (table.leaves == leafType) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static class TreeDropTable {
        public int growthTime;
        public Material trunk;
        public Material leaves;
        public List<DropEntry> drops = new ArrayList<>();
    }

    public static class DropEntry {
        public String itemId;
        public double chance;
        public int min;
        public int max;
    }
}
