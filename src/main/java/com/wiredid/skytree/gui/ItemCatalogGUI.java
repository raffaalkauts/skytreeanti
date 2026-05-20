package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.WorthService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemCatalogGUI {

    private final SkytreePlugin plugin;
    private final ItemRegistry itemRegistry;
    private final WorthService worthService;
    private final NamespacedKey catalogKey;

    private static final int ITEMS_PER_PAGE = 36;

    // Category definition: id, display name, icon material, list of item IDs
    private static class Category {
        final String id;
        final String name;
        final Material icon;
        final List<String> itemIds;

        Category(String id, String name, Material icon, List<String> itemIds) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.itemIds = itemIds;
        }
    }

    private final List<Category> categories;

    public ItemCatalogGUI(SkytreePlugin plugin, ItemRegistry itemRegistry, WorthService worthService) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.worthService = worthService;
        this.catalogKey = new NamespacedKey(plugin, "catalog_action");
        this.categories = buildCategories();
    }

    private List<Category> buildCategories() {
        List<Category> list = new ArrayList<>();

        list.add(new Category("sieve", "§e§lSieve", Material.HOPPER, Arrays.asList(
                "pebble_diorite", "pebble_andesite", "pebble_basalt", "pebble_blackstone",
                "piece_copper", "piece_tin", "piece_aluminum", "piece_silver",
                "piece_lead", "piece_nickel", "piece_zinc", "piece_gold",
                "piece_uranium", "piece_iron", "piece_coal",
                "grass_seeds", "ancient_spores",
                "seed_carrot", "seed_potato", "seed_beetroot",
                "seed_melon", "seed_pumpkin",
                "sapling_oak", "sapling_birch", "sapling_spruce", "sapling_jungle", "sapling_acacia"
        )));

        list.add(new Category("crafting", "§6§lCrafting", Material.CRAFTING_TABLE, Arrays.asList(
                "crook_wood", "crook_stone", "crook_bone", "crook_iron", "crook_gold",
                "crook_diamond", "crook_emerald", "crook_netherite",
                "hammer_wood", "hammer_stone", "hammer_iron", "hammer_gold",
                "hammer_diamond", "hammer_netherite",
                "mesh_string", "mesh_flint", "mesh_iron", "mesh_gold",
                "mesh_diamond", "mesh_emerald", "mesh_netherite",
                "water_bottle", "lava_bottle", "witch_water_bottle", "glass_water_bucket",
                "silkworm", "silk_mesh", "silkworm_cooked",
                "spawner_core", "powered_spawner",
                "storage_controller", "juicer",
                "item_pipe", "linker_tool",
                "auto_crafter",
                "dirt_essence", "sell_wand"
        )));

        list.add(new Category("furnace", "§c§lFurnace", Material.FURNACE, Arrays.asList(
                "dust_iron", "dust_gold", "dust_copper", "dust_tin",
                "dust_aluminum", "dust_silver", "dust_lead",
                "dust_nickel", "dust_zinc", "dust_uranium",
                "ingot_tin", "ingot_aluminum", "ingot_silver", "ingot_lead",
                "ingot_nickel", "ingot_zinc", "ingot_brass",
                "ingot_bronze", "ingot_steel", "ingot_uranium",
                "purified_water"
        )));

        list.add(new Category("alloy", "§5§lAlloy", Material.BREWING_STAND, Arrays.asList(
                "dust_brass", "dust_bronze", "dust_steel"
        )));

        list.add(new Category("compressor", "§b§lCompressor", Material.PISTON, Arrays.asList(
                "compressed_cobble_1x", "compressed_cobble_2x",
                "compressed_dirt_1x", "compressed_dirt_2x",
                "compressed_gravel_1x", "compressed_gravel_2x",
                "compressed_sand_1x", "compressed_sand_2x",
                "compressed_dust_1x"
        )));

        list.add(new Category("shardshop", "§d§lShard Shop", Material.END_CRYSTAL, Arrays.asList(
                "prime_drill", "timber_axe", "trench_pickaxe", "trench_shovel",
                "harvester_hoe", "slayer_sword", "god_wings",
                "divine_excavator", "divinebreaker", "mjolnir", "skyshaper_lance",
                "oracle_chest_basic", "oracle_chest_premium", "oracle_chest_divine"
        )));

        list.add(new Category("minions", "§3§lMinions", Material.ARMOR_STAND, Arrays.asList(
                "minion_farmer", "minion_miner", "minion_lumberjack", "minion_fisher"
        )));

        list.add(new Category("potions", "§a§lPotions", Material.POTION, Arrays.asList(
                "potion_infestation", "potion_oozing", "potion_weaving", "potion_wind_charging"
        )));

        // Armor sets (grouped by set in lore)
        List<String> armorIds = new ArrayList<>();
        String[] metals = {"tin", "aluminum", "silver", "lead", "nickel", "zinc", "brass", "bronze", "steel", "uranium"};
        for (String m : metals) {
            armorIds.add(m + "_helmet");
            armorIds.add(m + "_chestplate");
            armorIds.add(m + "_leggings");
            armorIds.add(m + "_boots");
            armorIds.add(m + "_sword");
            armorIds.add(m + "_pickaxe");
            armorIds.add(m + "_axe");
            armorIds.add(m + "_shovel");
            armorIds.add(m + "_hoe");
        }
        list.add(new Category("armor", "§b§lArmor & Tools", Material.DIAMOND_CHESTPLATE, armorIds));

        return list;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lItem Catalog"));
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);

        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < categories.size() && i < slots.length; i++) {
            Category cat = categories.get(i);
            int count = cat.itemIds.size();
            ItemStack icon = createItem(cat.icon, cat.name,
                    "§7" + count + " items",
                    "",
                    "§e> Click to Browse");
            ItemMeta meta = icon.getItemMeta();
            meta.getPersistentDataContainer().set(catalogKey, PersistentDataType.STRING, "cat_" + cat.id);
            icon.setItemMeta(meta);
            gui.setItem(slots[i], icon);
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    public void openCategory(Player player, String categoryId, int page) {
        Category cat = null;
        for (Category c : categories) {
            if (c.id.equals(categoryId)) {
                cat = c;
                break;
            }
        }
        if (cat == null) return;

        int totalPages = Math.max(1, (int) Math.ceil((double) cat.itemIds.size() / ITEMS_PER_PAGE));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        Inventory gui = Bukkit.createInventory(null, 54,
                ComponentUtil.parse("§6" + cat.name + " §8(Page " + page + "/" + totalPages + ")"));

        // Background
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, cat.itemIds.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            String itemId = cat.itemIds.get(i);
            ItemStack item = itemRegistry.getItem(itemId);
            if (item == null) continue;

            ItemStack display = item.clone();
            ItemMeta meta = display.getItemMeta();

            List<String> loreList = new ArrayList<>();
            if (meta.hasLore() && meta.lore() != null) {
                for (net.kyori.adventure.text.Component c : meta.lore()) {
                    loreList.add(ComponentUtil.toLegacy(c));
                }
            }

            // Add worth info
            double worth = worthService != null ? worthService.getItemSellPrice(item) : 0;
            loreList.add("");
            if (worth > 0) {
                loreList.add("§aWorth: §e$" + String.format("%,.2f", worth));
            } else {
                loreList.add("§7No shop value");
            }

            loreList.add("");
            loreList.add("§eClick to view Recipe");

            meta.lore(ComponentUtil.parseList(loreList));

            NamespacedKey key = new NamespacedKey(plugin, "item_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, itemId);
            display.setItemMeta(meta);

            gui.setItem(slot, display);
            slot++;
            // Skip slot 45+ for navigation
            if (slot == 45) slot = 45 + 9;
        }

        // Navigation row (slots 45-53)
        if (page > 1) {
            ItemStack prev = createItem(Material.ARROW, "§e« Previous Page", "§7Page " + (page - 1));
            ItemMeta pMeta = prev.getItemMeta();
            pMeta.getPersistentDataContainer().set(catalogKey, PersistentDataType.STRING, "prev_" + categoryId + "_" + page);
            prev.setItemMeta(pMeta);
            gui.setItem(45, prev);
        }

        if (page < totalPages) {
            ItemStack next = createItem(Material.ARROW, "§eNext Page »", "§7Page " + (page + 1));
            ItemMeta nMeta = next.getItemMeta();
            nMeta.getPersistentDataContainer().set(catalogKey, PersistentDataType.STRING, "next_" + categoryId + "_" + page);
            next.setItemMeta(nMeta);
            gui.setItem(53, next);
        }

        // Back to main catalog
        ItemStack back = createItem(Material.BARRIER, "§cBack to Catalog");
        ItemMeta bMeta = back.getItemMeta();
        bMeta.getPersistentDataContainer().set(catalogKey, PersistentDataType.STRING, "back");
        back.setItemMeta(bMeta);
        gui.setItem(49, back);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    public NamespacedKey getCatalogKey() {
        return catalogKey;
    }

    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ComponentUtil.parse(name));
            List<net.kyori.adventure.text.Component> l = new ArrayList<>();
            for (String line : lore) {
                l.add(ComponentUtil.parse(line));
            }
            meta.lore(l);
            item.setItemMeta(meta);
        }
        return item;
    }
}
