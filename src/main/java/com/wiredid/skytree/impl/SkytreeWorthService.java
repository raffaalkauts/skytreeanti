package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.WorthService;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SkytreeWorthService implements WorthService {

    private final SkytreePlugin plugin;
    private final Map<String, Double> sellPrices = new HashMap<>();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private FileConfiguration worthConfig;

    public SkytreeWorthService(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        sellPrices.clear();

        File worthFile = new File(plugin.getDataFolder(), "worth.yml");
        if (!worthFile.exists()) {
            plugin.saveResource("worth.yml", false);
        }
        this.worthConfig = YamlConfiguration.loadConfiguration(worthFile);

        ConfigurationSection typeSec = worthConfig.getConfigurationSection("TYPE");
        if (typeSec != null) {
            for (String category : typeSec.getKeys(false)) {
                ConfigurationSection catSec = typeSec.getConfigurationSection(category);
                if (catSec != null) {
                    for (String key : catSec.getKeys(false)) {
                        sellPrices.put(key.toLowerCase(Locale.ROOT), catSec.getDouble(key));
                    }
                }
            }
        }

        loadShopOverrides();
        loadCustomItemDefaults();
    }

    private void loadCustomItemDefaults() {
        for (String id : plugin.getItemRegistry().getAllItemIds()) {
            if (sellPrices.containsKey(id)) continue;
            sellPrices.put(id, defaultWorthForItem(id));
        }
    }

    private double defaultWorthForItem(String id) {
        if (id.startsWith("dust_")) return 2.0;
        if (id.startsWith("piece_")) return 5.0;
        if (id.startsWith("ingot_")) return 20.0;
        if (id.startsWith("pebble_")) return 1.0;
        if (id.startsWith("seed_")) return 10.0;
        if (id.startsWith("sapling_")) return 15.0;
        if (id.startsWith("compressed_cobble_2x")) return 162.0;
        if (id.startsWith("compressed_cobble_1x")) return 18.0;
        if (id.startsWith("compressed_dirt_2x")) return 162.0;
        if (id.startsWith("compressed_dirt_1x")) return 18.0;
        if (id.startsWith("compressed_")) return 50.0;
        if (id.startsWith("crook_wood")) return 100.0;
        if (id.startsWith("crook_stone")) return 150.0;
        if (id.startsWith("crook_iron")) return 250.0;
        if (id.startsWith("crook_gold")) return 200.0;
        if (id.startsWith("crook_diamond")) return 400.0;
        if (id.startsWith("crook_netherite")) return 500.0;
        if (id.startsWith("hammer_wood")) return 150.0;
        if (id.startsWith("hammer_stone")) return 200.0;
        if (id.startsWith("hammer_iron")) return 350.0;
        if (id.startsWith("hammer_gold")) return 300.0;
        if (id.startsWith("hammer_diamond")) return 600.0;
        if (id.startsWith("hammer_netherite")) return 750.0;
        if (id.startsWith("mesh_string")) return 50.0;
        if (id.startsWith("mesh_flint")) return 80.0;
        if (id.startsWith("mesh_iron")) return 120.0;
        if (id.startsWith("mesh_gold")) return 100.0;
        if (id.startsWith("mesh_diamond")) return 200.0;
        if (id.startsWith("mesh_netherite")) return 300.0;
        if (id.startsWith("minion_")) return 1000.0;
        if (id.contains("helmet")) return 100.0;
        if (id.contains("chestplate")) return 150.0;
        if (id.contains("leggings")) return 120.0;
        if (id.contains("boots")) return 80.0;
        if (id.contains("sword")) return 100.0;
        if (id.contains("pickaxe")) return 100.0;
        if (id.contains("axe")) return 100.0;
        if (id.contains("shovel")) return 80.0;
        if (id.contains("hoe")) return 60.0;
        if (id.equals("sieve")) return 200.0;
        if (id.equals("barrel")) return 150.0;
        if (id.equals("crucible")) return 300.0;
        if (id.equals("compressor")) return 400.0;
        if (id.equals("pulverizer")) return 350.0;
        if (id.equals("furnace_advanced")) return 250.0;
        if (id.equals("auto_crafter")) return 500.0;
        if (id.equals("cobble_gen")) return 100.0;
        if (id.equals("sell_wand")) return 500.0;
        if (id.equals("prime_drill") || id.equals("timber_axe")) return 5000.0;
        if (id.equals("trench_pickaxe") || id.equals("trench_shovel")) return 4000.0;
        if (id.equals("harvester_hoe")) return 4000.0;
        if (id.equals("slayer_sword")) return 7500.0;
        if (id.equals("god_wings")) return 10000.0;
        if (id.equals("divine_excavator") || id.equals("divinebreaker")) return 10000.0;
        if (id.equals("mjolnir")) return 15000.0;
        if (id.equals("skyshaper_lance")) return 20000.0;
        if (id.startsWith("potion_")) return 100.0;
        if (id.equals("silkworm") || id.equals("silkworm_cooked")) return 5.0;
        if (id.equals("silk_mesh")) return 30.0;
        if (id.equals("spawner_core")) return 300.0;
        if (id.equals("powered_spawner")) return 1000.0;
        if (id.equals("item_pipe")) return 50.0;
        if (id.equals("linker_tool")) return 100.0;
        if (id.equals("storage_controller")) return 300.0;
        if (id.equals("juicer")) return 200.0;
        return 10.0;
    }

    private void loadShopOverrides() {
        // This method is now deprecated and kept as a stub to avoid breaking references.
        // Worth is now strictly driven by worth.yml and legacy config.yml.
    }

    private double calculateDefaultWorth(Material mat) {
        String name = mat.name();

        if (name.contains("NETHERITE"))
            return 1000.0;
        if (name.contains("DIAMOND"))
            return 100.0;
        if (name.contains("EMERALD"))
            return 50.0;
        if (name.contains("GOLD_"))
            return 30.0;
        if (name.contains("IRON_"))
            return 20.0;
        if (name.contains("COPPER_"))
            return 15.0;
        if (name.contains("ORE"))
            return 25.0;
        if (name.contains("RAW_"))
            return 15.0;

        if (name.contains("LOG") || name.contains("STEM"))
            return 10.0;
        if (name.contains("PLANKS"))
            return 2.5;
        if (name.contains("LEAVES"))
            return 1.0;
        if (name.contains("SAPLING"))
            return 5.0;

        if (mat.isEdible())
            return 5.0;
        if (name.contains("SPAWN_EGG"))
            return 0;
        if (name.contains("MUSIC_DISC"))
            return 1000.0;
        if (name.contains("POTTERY_SHERD") || name.contains("ARMOR_TRIM"))
            return 200.0;

        if (mat.isBlock())
            return 2.0;

        return 2.0;
    }

    @Override
    public double getItemSellPrice(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return 0;

        if (com.wiredid.skytree.fishing.NbtUtils.isCustomFish(item)) {
            double fishPrice = com.wiredid.skytree.fishing.NbtUtils.getDouble(item,
                    com.wiredid.skytree.fishing.NbtUtils.KEY_FISH_PRICE);
            if (fishPrice > 0)
                return fishPrice;
        }

        String customId = plugin.getItemRegistry().getItemId(item);
        if (customId != null && sellPrices.containsKey(customId.toLowerCase(Locale.ROOT))) {
            return sellPrices.get(customId.toLowerCase(Locale.ROOT));
        }

        if (item.getType() == Material.SPAWNER) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof org.bukkit.block.CreatureSpawner spawner) {
                    org.bukkit.entity.EntityType spawnedType = spawner.getSpawnedType();
                    if (spawnedType != null) {
                        String mobName = spawnedType.name().toLowerCase(Locale.ROOT);
                        String key = "spawner_" + mobName;
                        if (sellPrices.containsKey(key))
                            return sellPrices.get(key);
                    }
                }
            }
        }

        String matKey = item.getType().name().toLowerCase(Locale.ROOT);
        return sellPrices.getOrDefault(matKey, calculateDefaultWorth(item.getType()));
    }

    @Override
    public void updateItemLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return;

        double unitPrice = getItemSellPrice(item);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        List<Component> lore = meta.lore();
        boolean hasLore = lore != null && !lore.isEmpty();

        if (unitPrice <= 0 && !hasLore)
            return;

        List<Component> newLore;
        if (hasLore && lore != null) {
            newLore = lore.stream()
                    .filter(line -> {
                        String plain = PLAIN_SERIALIZER.serialize(line);
                        return !isWorthLoreLine(plain);
                    })
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            newLore = new ArrayList<>();
        }

        if (unitPrice > 0) {
            if (!newLore.isEmpty() && !PLAIN_SERIALIZER.serialize(newLore.get(newLore.size() - 1)).isBlank()) {
                newLore.add(Component.empty());
            }

            newLore.add(ComponentUtil.parse("&8[&6Worth&8]"));
            newLore.add(ComponentUtil.parse("&7Unit: &e" + NumberUtil.formatCurrency(unitPrice)));
            // Removed Total line to fix stackability issues in lore-fallback mode
        }

        if (lore != null && newLore.equals(lore)) {
            return;
        }

        meta.lore(newLore);
        item.setItemMeta(meta);
    }

    @Override
    public void updateInventoryLore(Player player) {
        if (plugin.getPersistenceService() == null) return;

        com.wiredid.skytree.model.PlayerData pData = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        if (pData == null || pData.getSettings() == null) return;

        boolean enabled = pData.getSettings().getOrDefault("worth_display", true);
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        if (contents == null) return;
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            
            String sigBefore = getLoreSig(item);
            
            if (enabled) {
                updateItemLore(item);
            } else {
                stripWorthLore(item);
            }

            if (!sigBefore.equals(getLoreSig(item))) {
                inv.setItem(i, item); 
                changed = true;
            }
        }
        if (changed) {
            player.updateInventory();
        }
    }

    private String getLoreSig(ItemStack item) {
        if (!item.hasItemMeta()) return "";
        org.bukkit.inventory.meta.ItemMeta m = item.getItemMeta();
        if (!m.hasLore()) return "";
        List<Component> lore = m.lore();
        if (lore == null) return "";
        return lore.stream()
                .map(PLAIN_SERIALIZER::serialize)
                .collect(java.util.stream.Collectors.joining("|"));
    }

    @Override
    public void removeInventoryLore(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        boolean changed = false;
        ItemStack[] contents = inv.getContents();
        if (contents == null) return;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                String sigBefore = getLoreSig(item);
                stripWorthLore(item);
                if (!sigBefore.equals(getLoreSig(item))) {
                    inv.setItem(i, item);
                    changed = true;
                }
            }
        }
        if (changed) player.updateInventory();
    }

    private void stripWorthLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return;
        }

        List<Component> filtered = lore.stream()
                .filter(line -> {
                    String plain = PLAIN_SERIALIZER.serialize(line);
                    return !isWorthLoreLine(plain);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        if (filtered.equals(lore)) {
            return;
        }

        meta.lore(filtered);
        item.setItemMeta(meta);
    }

    @Override
    public void stripSingleItemLore(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            stripWorthLore(item);
        }
    }


    @Override
    public boolean isSimilarIgnoringWorth(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null)
            return false;
        if (item1.getType() != item2.getType())
            return false;
        if (item1.hasItemMeta() != item2.hasItemMeta())
            return false;
        if (!item1.hasItemMeta())
            return true;

        ItemStack clone1 = item1.clone();
        ItemStack clone2 = item2.clone();
        
        stripWorthLore(clone1);
        stripWorthLore(clone2);
        
        return clone1.isSimilar(clone2);
    }

    private boolean isWorthLoreLine(String plain) {
        String clean = plain == null ? "" : plain;
        return clean.contains("[Worth]")
                || clean.contains("Worth:")
                || clean.contains("Unit:")
                || clean.contains("Total:");
    }
}
