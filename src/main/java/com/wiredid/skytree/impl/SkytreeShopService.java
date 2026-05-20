package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.ShopService;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Shop implementation loading from shop.yml and merging with worth.yml
 */
public class SkytreeShopService implements ShopService {

        private final SkytreePlugin plugin;
        private FileConfiguration shopConfig;
        private final Map<String, Inventory> categoryInventories = new HashMap<>();
        private final Map<String, String> categoryNames = new HashMap<>();
        private final Map<String, Material> categoryIcons = new HashMap<>();

        public SkytreeShopService(SkytreePlugin plugin, EconomyService economy, ItemRegistry itemRegistry) {
                this.plugin = plugin;
        }

        @Override
        public void reload() {
                File shopFile = new File(plugin.getDataFolder(), "shop.yml");
                if (!shopFile.exists()) {
                        plugin.saveResource("shop.yml", false);
                }
                this.shopConfig = YamlConfiguration.loadConfiguration(shopFile);
                categoryInventories.clear();
                loadCategories();
                if (plugin.getWorthService() != null) {
                        plugin.getWorthService().reload();
                }
        }

        @Override
        public org.bukkit.configuration.file.FileConfiguration getShopConfig() {
                return shopConfig;
        }

        private void loadCategories() {
                // Default Icon Mappings
                categoryIcons.put("blocks", Material.GRASS_BLOCK);
                categoryNames.put("blocks", "§aBlocks");

                categoryIcons.put("blocks_natural", Material.GRASS_BLOCK);
                categoryNames.put("blocks_natural", "§aNatural Blocks");

                categoryIcons.put("blocks_stone", Material.STONE);
                categoryNames.put("blocks_stone", "§7Stone & Ores");

                categoryIcons.put("blocks_wood", Material.OAK_LOG);
                categoryNames.put("blocks_wood", "§6Woods");

                categoryIcons.put("blocks_copper", Material.COPPER_BLOCK);
                categoryNames.put("blocks_copper", "§6Copper & Tuff");

                categoryIcons.put("decoration_wool", Material.WHITE_WOOL);
                categoryNames.put("decoration_wool", "§fWool & Carpet");

                categoryIcons.put("decoration_colored", Material.GLASS);
                categoryNames.put("decoration_colored", "§dColored Blocks");

                categoryIcons.put("decoration_misc", Material.FLOWER_POT);
                categoryNames.put("decoration_misc", "§dDecoration Misc");

                categoryIcons.put("farming", Material.WHEAT);
                categoryNames.put("farming", "§eFarming");

                categoryIcons.put("spawners", Material.SPAWNER);
                categoryNames.put("spawners", "§5Spawners");

                categoryIcons.put("machines", Material.FURNACE);
                categoryNames.put("machines", "§6Machines");

                categoryIcons.put("minerals", Material.DIAMOND);
                categoryNames.put("minerals", "§bMinerals");

                categoryIcons.put("mob_drops", Material.ROTTEN_FLESH);
                categoryNames.put("mob_drops", "§cMob Drops");

                categoryIcons.put("decoration", Material.OAK_SAPLING);
                categoryNames.put("decoration", "§dDecoration");

                categoryIcons.put("custom_items", Material.NETHER_STAR);
                categoryNames.put("custom_items", "§6Custom Items");

                categoryIcons.put("food", Material.COOKED_BEEF);
                categoryNames.put("food", "§6Food");

                categoryIcons.put("redstone", Material.REDSTONE);
                categoryNames.put("redstone", "§cRedstone");

                categoryIcons.put("transport", Material.MINECART);
                categoryNames.put("transport", "§9Transport");

                categoryIcons.put("utility", Material.NAME_TAG);
                categoryNames.put("utility", "§bUtility");

                categoryIcons.put("brewing", Material.BREWING_STAND);
                categoryNames.put("brewing", "§5Brewing");

                categoryIcons.put("dyes", Material.MAGENTA_DYE);
                categoryNames.put("dyes", "§dDyes");

                categoryIcons.put("special", Material.MUSIC_DISC_CAT);
                categoryNames.put("special", "§bSpecial");

                categoryIcons.put("misc", Material.TORCH);
                categoryNames.put("misc", "§7Misc");

                categoryIcons.put("resources", Material.IRON_INGOT);
                categoryNames.put("resources", "§7Resources");

                categoryIcons.put("blocks_compressed", Material.COBBLESTONE);
                categoryNames.put("blocks_compressed", "§8Compressed Blocks");

                categoryIcons.put("machines_custom", Material.PISTON);
                categoryNames.put("machines_custom", "§6Custom Machines");

                categoryIcons.put("armor_sets", Material.NETHERITE_CHESTPLATE);
                categoryNames.put("armor_sets", "§bArmor & Tools");

                categoryIcons.put("minions", Material.VILLAGER_SPAWN_EGG);
                categoryNames.put("minions", "§dMinions");

                categoryIcons.put("gadgets", Material.HOPPER);
                categoryNames.put("gadgets", "§eGadgets");

                categoryIcons.put("potions_custom", Material.POTION);
                categoryNames.put("potions_custom", "§5Custom Potions");

                // Dynamic Fallback for any unmapped categories in config
                ConfigurationSection shopSec = shopConfig.getConfigurationSection("categories");
                if (shopSec == null) {
                        shopSec = shopConfig.getConfigurationSection("shop_categories");
                }

                if (shopSec != null) {
                        for (String key : shopSec.getKeys(false)) {
                                if (!categoryNames.containsKey(key)) {
                                        ConfigurationSection catSec = shopSec.getConfigurationSection(key);
                                        if (catSec != null) {
                                                String iconStr = catSec.getString("icon");
                                                if (iconStr != null) {
                                                        try {
                                                                categoryIcons.put(key, Material
                                                                                .valueOf(iconStr.toUpperCase()));
                                                        } catch (Exception ignored) {
                                                        }
                                                }
                                                String name = catSec.getString("name");
                                                if (name != null) {
                                                        categoryNames.put(key, ComponentUtil
                                                                        .toLegacy(ComponentUtil.parse(name)));
                                                }
                                        }

                                        if (!categoryNames.containsKey(key)) {
                                                categoryIcons.put(key, Material.CHEST);
                                                String name = key.replace("_", " ");
                                                if (name.length() > 0) {
                                                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                                                }
                                                categoryNames.put(key, "§e" + name);
                                        }
                                }
                        }
                }

                // ==========================================
                // AUTO-POPULATION FROM WORTH SYSTEM
                // ==========================================
                for (Material mat : Material.values()) {
                        if (mat.isAir() || !mat.isItem())
                                continue;
                        // Skip forbidden items (spawn eggs, admin blocks, etc.)
                        String matName = mat.name();
                        if (matName.contains("SPAWN_EGG")
                                        || mat == Material.BARRIER || mat == Material.BEDROCK
                                        || mat == Material.COMMAND_BLOCK || mat == Material.CHAIN_COMMAND_BLOCK
                                        || mat == Material.REPEATING_COMMAND_BLOCK
                                        || mat == Material.STRUCTURE_BLOCK || mat == Material.JIGSAW
                                        || mat == Material.DRAGON_EGG)
                                continue;
                        String itemId = mat.name().toLowerCase();
                        String category = determineCategory(mat);
                        if (!shopConfig.contains("categories." + category + "." + itemId)) {
                                double sell = plugin.getWorthService().getItemSellPrice(new ItemStack(mat));
                                if (sell > 0) {
                                        shopConfig.set("categories." + category + "." + itemId + ".sell",
                                                        sell);
                                        shopConfig.set("categories." + category + "." + itemId + ".price",
                                                        sell * 10.0);
                                }
                        }
                }
                // ==========================================

                categoryIcons.put("enchanted_books", Material.ENCHANTED_BOOK);
                categoryNames.put("enchanted_books", "§bEnchanted Books");

                categoryIcons.put("custom_gear", Material.NETHERITE_CHESTPLATE);
                categoryNames.put("custom_gear", "§6Custom Gear");
        }

        private String determineCategory(Material mat) {
                String name = mat.name();
                if (name.contains("LOG") || name.contains("WOOD") || name.contains("STEM"))
                        return "blocks_wood";
                if (name.contains("SAPLING") || name.contains("LEAVES") || name.contains("GRASS"))
                        return "blocks_natural";
                if (name.contains("ORE") || name.contains("RAW_"))
                        return "blocks_stone";
                if (name.contains("COPPER") || name.contains("TUFF"))
                        return "blocks_copper";
                if (name.contains("WOOL") || name.contains("CARPET"))
                        return "decoration_wool";
                if (name.contains("GLASS") || name.contains("STAINED_"))
                        return "decoration_colored";
                if (name.contains("TERRACOTTA") || name.contains("CONCRETE"))
                        return "decoration_colored";
                if (mat.isEdible())
                        return "food";
                if (name.contains("WHEAT") || name.contains("SEED") || name.contains("POTATO")
                                || name.contains("CARROT"))
                        return "farming";
                if (name.contains("DYE"))
                        return "dyes";
                if (name.contains("REDSTONE"))
                        return "redstone";
                if (name.contains("MINECART") || name.contains("RAIL"))
                        return "transport";
                if (name.contains("POTION") || name.contains("BREWING"))
                        return "brewing";
                if (name.contains("_SWORD") || name.contains("_PICKAXE") || name.contains("_AXE")
                                || name.contains("_SHOVEL") || name.contains("_HOE"))
                        return "equipment";
                if (name.contains("_HELMET") || name.contains("_CHESTPLATE") || name.contains("_LEGGINGS")
                                || name.contains("_BOOTS"))
                        return "equipment";
                if (name.contains("SPAWN_EGG"))
                        return "special";
                if (name.contains("MUSIC_DISC"))
                        return "special";
                return "misc";
        }

        @Override
        public void openShop(Player player) {
                Inventory shop = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§6§lShop Categories"));
                int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37,
                                38, 39, 40, 41, 42, 43 };
                int index = 0;
                String[] priority = { "blocks_natural", "blocks_stone", "blocks_wood", "blocks_copper", "farming",
                                "food", "minerals", "mob_drops",
                                "decoration_wool", "decoration_colored", "decoration_misc", "dyes", "redstone",
                                "transport", "brewing", "equipment",
                                "spawners", "machines", "custom_items", "special", "enchanted_books", "custom_gear",
                                "misc", "utility" };

                for (String key : priority) {
                        if (index >= slots.length)
                                break;
                        if (shopConfig.contains("categories." + key)) {
                                addCategoryItem(shop, slots[index++], key, "§7Click to browse items");
                        }
                }

                ItemStack search = new ItemStack(Material.OAK_SIGN);
                ItemMeta sMeta = search.getItemMeta();
                sMeta.displayName(ComponentUtil.smartParse("§a§lSearch Item"));
                sMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_action"),
                                PersistentDataType.STRING, "SEARCH");
                search.setItemMeta(sMeta);
                shop.setItem(48, search);

                ItemStack close = new ItemStack(Material.BARRIER);
                ItemMeta meta = close.getItemMeta();
                meta.displayName(ComponentUtil.smartParse("§cClose"));
                close.setItemMeta(meta);
                shop.setItem(49, close);

                player.openInventory(shop);
        }

        private void addCategoryItem(Inventory inv, int slot, String key, String desc) {
                ItemStack item = new ItemStack(categoryIcons.getOrDefault(key, Material.CHEST));
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ComponentUtil.smartParse(categoryNames.getOrDefault(key, key)));
                List<Component> lore = new ArrayList<>();
                lore.add(ComponentUtil.smartParse(desc));
                lore.add(Component.empty());
                lore.add(ComponentUtil.smartParse("§eClick to browse"));
                meta.lore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_category"),
                                PersistentDataType.STRING, key);
                item.setItemMeta(meta);
                inv.setItem(slot, item);
        }

        public void openSearch(Player player, String query, String mode) {
                String guiTitle = "§6§lShop Search: " + query + " (" + mode + ")";
                if (guiTitle.length() > 32)
                        guiTitle = "Search: " + query;

                Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse(guiTitle));
                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.ORANGE_STAINED_GLASS_PANE,
                                Material.YELLOW_STAINED_GLASS_PANE);

                List<ItemStack> matches = new ArrayList<>();
                if (shopConfig.contains("categories")) {
                        ConfigurationSection categoriesSec = shopConfig.getConfigurationSection("categories");
                        if (categoriesSec != null) {
                                for (String catKey : categoriesSec.getKeys(false)) {
                                        ConfigurationSection catSection = categoriesSec.getConfigurationSection(catKey);
                                        if (catSection == null) continue;

                                        for (String itemId : catSection.getKeys(false)) {
                                                if (itemId.equals("icon") || itemId.equals("name") || itemId.equals("slot")) continue;
                                                
                                                if (itemId.equalsIgnoreCase(query) || itemId.contains(query.toLowerCase())) {
                                                        ItemStack item = createGuiShopItem(catSection, itemId, mode);
                                                        if (item != null) {
                                                                matches.add(item);
                                                        }
                                                }
                                                if (matches.size() >= 45) break;
                                        }
                                        if (matches.size() >= 45) break;
                                }
                        } else {
                                plugin.getLogger().warning("Shop configuration section 'categories' is missing!");
                        }
                }

                int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37,
                                38, 39, 40, 41, 42, 43 };
                for (int i = 0; i < matches.size() && i < slots.length; i++)
                        inv.setItem(slots[i], matches.get(i));

                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta bMeta = back.getItemMeta();
                bMeta.displayName(ComponentUtil.smartParse("§cBack to Categories"));
                back.setItemMeta(bMeta);
                inv.setItem(49, back);

                ItemStack toggle = new ItemStack(mode.equals("BUY") ? Material.LIME_DYE : Material.RED_DYE);
                ItemMeta tMeta = toggle.getItemMeta();
                tMeta.displayName(ComponentUtil
                                .smartParse(mode.equals("BUY") ? "§a§lMode: BUYING" : "§c§lMode: SELLING"));
                List<Component> tLore = new ArrayList<>();
                tLore.add(ComponentUtil.smartParse("§7Click to switch mode."));
                tLore.add(Component.empty());
                tLore.add(ComponentUtil.smartParse(
                                mode.equals("BUY") ? "§7Current: §aTap items to BUY\n§eClick to switch to SELL mode"
                                                : "§7Current: §cTap items to SELL\n§eClick to switch to BUY mode"));
                tMeta.lore(tLore);
                tMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_action"),
                                PersistentDataType.STRING, "SEARCH_TOGGLE");
                tMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "search_query"),
                                PersistentDataType.STRING, query);
                tMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "search_mode"),
                                PersistentDataType.STRING, mode);
                toggle.setItemMeta(tMeta);
                inv.setItem(53, toggle);

                player.openInventory(inv);
        }

        public void openSearch(Player player, String query) {
                openSearch(player, query, "BUY");
        }

        private ItemStack createGuiShopItem(ConfigurationSection section, String itemId, String mode) {
                double basePrice = section.getDouble(itemId + ".price", -1);
                double baseSell = section.getDouble(itemId + ".sell", -1);

                double price = basePrice;
                double sell = baseSell;
                if (plugin.getEconomyManager() != null) {
                        price = plugin.getEconomyManager().getAdjustedPrice(itemId, basePrice);
                        sell = plugin.getEconomyManager().getAdjustedSellPrice(itemId, baseSell);
                }

                ItemStack item = null;

                if (plugin.getItemRegistry() != null && plugin.getItemRegistry().getItem(itemId) != null) {
                        item = plugin.getItemRegistry().getItem(itemId);
                } else if (itemId.startsWith("spawner_")) {
                        String entityName = itemId.replace("spawner_", "").toUpperCase();
                        // Map common names to EntityType
                        entityName = switch (entityName) {
                                case "MOOSHROOM" -> "MUSHROOM_COW";
                                case "IRON_GOLEM" -> "IRON_GOLEM";
                                case "SNOW_GOLEM" -> "SNOW_GOLEM";
                                case "OCELOT" -> "OCELOT";
                                case "CAT" -> "CAT";
                                case "WOLF" -> "WOLF";
                                default -> entityName;
                        };

                        try {
                                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(entityName);
                                item = new ItemStack(Material.SPAWNER);
                                ItemMeta meta = item.getItemMeta();
                                if (meta instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                                        org.bukkit.block.CreatureSpawner spawner = (org.bukkit.block.CreatureSpawner) bsm.getBlockState();
                                        spawner.setSpawnedType(type);
                                        bsm.setBlockState(spawner);
                                        // Proper display name from EntityType
                                        String[] words = type.name().replace("_", " ").toLowerCase().split(" ");
                                        StringBuilder sb = new StringBuilder();
                                        for (String w : words) {
                                                if (!w.isEmpty()) {
                                                        sb.append(Character.toUpperCase(w.charAt(0)))
                                                                        .append(w.substring(1)).append(" ");
                                                }
                                        }
                                        String friendlyName = sb.toString().trim();
                                        bsm.displayName(ComponentUtil.smartParse("§f" + friendlyName + " Spawner"));
                                        item.setItemMeta(bsm);
                                }
                        } catch (Exception e) {
                                final String errorEntityName = entityName;
                                plugin.getLogger().warning(() -> "Failed to create spawner for: " + errorEntityName + " - " + e.getMessage());
                        }
                } else if (itemId.startsWith("enchanted_book_")) {
                        try {
                                String raw = itemId.replace("enchanted_book_", "");
                                int lastUnderscore = raw.lastIndexOf('_');
                                if (lastUnderscore != -1) {
                                        String enchantName = raw.substring(0, lastUnderscore).toLowerCase();
                                        int level = 1;
                                        try {
                                                level = Integer.parseInt(raw.substring(lastUnderscore + 1));
                                        } catch (NumberFormatException e) {
                                                plugin.getLogger().warning(() -> "Invalid enchantment level for: " + itemId);
                                        }

                                        @SuppressWarnings("deprecation")
                                        org.bukkit.enchantments.Enchantment enchant = org.bukkit.Registry.ENCHANTMENT
                                                        .get(org.bukkit.NamespacedKey.minecraft(enchantName));
                                        if (enchant != null) {
                                                item = new ItemStack(Material.ENCHANTED_BOOK);
                                                org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) item
                                                                .getItemMeta();
                                                if (meta != null) {
                                                        meta.addStoredEnchant(enchant, level, true);
                                                        item.setItemMeta(meta);
                                                }
                                        }
                                }
                        } catch (Exception ignored) {
                        }
                } else {
                        Material mat = Material.matchMaterial(itemId);
                        if (mat != null)
                                item = new ItemStack(mat);
                }

                if (item != null) {
                        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                                List<net.kyori.adventure.text.Component> lore = meta.lore();
                                if (lore == null) lore = new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(ComponentUtil.smartParse(price > 0 ? "§7Buy: §a\u20AE " + String.format("%,.0f", price)
                                        : "§7Buy: §cNot for sale"));
                        lore.add(ComponentUtil.smartParse(sell > 0 ? "§7Sell: §e\u20AE " + String.format("%,.0f", sell)
                                        : "§7Sell: §cUnsellable"));
                        lore.add(Component.empty());
                        if (mode.equals("BUY")) {
                                lore.add(ComponentUtil.smartParse("§a▶ §eClick to BUY"));
                        } else {
                                lore.add(ComponentUtil
                                                .smartParse(sell > 0 ? "§c▶ §eClick to SELL" : "§7▶ §cCannot be sold"));
                        }
                        meta.lore(lore);
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_buy_price"),
                                        PersistentDataType.DOUBLE, price);
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_sell_price"),
                                        PersistentDataType.DOUBLE, sell);
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_item_id"),
                                        PersistentDataType.STRING, itemId);
                        item.setItemMeta(meta);
                    }
                }
                return item;
        }

        @Override
        public void openCategory(Player player, String categoryId) {
                openCategory(player, categoryId, 0, "BUY");
        }

        @Override
        public void openCategory(Player player, String categoryId, int page, String mode) {
                ConfigurationSection categorySection = shopConfig.getConfigurationSection("categories." + categoryId);
                if (categorySection == null) {
                        player.sendMessage("§cThis category is empty!");
                        return;
                }

                String title = categoryNames.getOrDefault(categoryId, "Category: " + categoryId);
                String guiTitle = "§6§lShop - " + title.replaceAll("§[a-f0-9]", "") + " (" + mode + ")";
                if (guiTitle.length() > 32)
                        guiTitle = "Shop " + mode;

                Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.smartParse(guiTitle));
                List<String> keys = new ArrayList<>(categorySection.getKeys(false));
                int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37,
                                38, 39, 40, 41, 42, 43 };
                int pageSize = slots.length;
                int startIndex = page * pageSize;
                int endIndex = Math.min(startIndex + pageSize, keys.size());

                for (int i = startIndex, slot = 0; i < endIndex; i++) {
                        ItemStack item = createGuiShopItem(categorySection, keys.get(i), mode);
                        if (item != null)
                                gui.setItem(slots[slot++], item);
                }

                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.CYAN_STAINED_GLASS_PANE,
                                Material.BLUE_STAINED_GLASS_PANE);

                // Sell All Button (Slot 52)
                ItemStack sellAll = new ItemStack(Material.HOPPER);
                ItemMeta sMeta = sellAll.getItemMeta();
                sMeta.displayName(ComponentUtil.smartParse("§e§lSell All Items"));
                sMeta.lore(List.of(ComponentUtil.smartParse("§7Sell all matching items in inventory"),
                                Component.empty(), ComponentUtil.smartParse("§eClick to Sell")));
                sMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_action"),
                                PersistentDataType.STRING, "SELL_ALL_CATEGORY");
                sMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_cat_id"),
                                PersistentDataType.STRING, categoryId);
                sellAll.setItemMeta(sMeta);
                gui.setItem(52, sellAll);

                if (page > 0) {
                        ItemStack prev = new ItemStack(Material.ARROW);
                        ItemMeta pMeta = prev.getItemMeta();
                        pMeta.displayName(ComponentUtil.smartParse("§ePrevious Page"));
                        pMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_page"),
                                        PersistentDataType.INTEGER, page - 1);
                        pMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_cat_id"),
                                        PersistentDataType.STRING, categoryId);
                        pMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_mode"),
                                        PersistentDataType.STRING, mode);
                        prev.setItemMeta(pMeta);
                        gui.setItem(48, prev);
                }

                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta bMeta = back.getItemMeta();
                bMeta.displayName(ComponentUtil.smartParse("§cBack to Categories"));
                back.setItemMeta(bMeta);
                gui.setItem(49, back);

                if (endIndex < keys.size()) {
                        ItemStack next = new ItemStack(Material.ARROW);
                        ItemMeta nMeta = next.getItemMeta();
                        nMeta.displayName(ComponentUtil.smartParse("§eNext Page"));
                        nMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_page"),
                                        PersistentDataType.INTEGER, page + 1);
                        nMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_cat_id"),
                                        PersistentDataType.STRING, categoryId);
                        nMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_mode"),
                                        PersistentDataType.STRING, mode);
                        next.setItemMeta(nMeta);
                        gui.setItem(50, next);
                }

                ItemStack toggle = new ItemStack(mode.equals("BUY") ? Material.LIME_DYE : Material.RED_DYE);
                ItemMeta tMeta = toggle.getItemMeta();
                tMeta.displayName(ComponentUtil
                                .smartParse(mode.equals("BUY") ? "§a§lMode: BUYING" : "§c§lMode: SELLING"));
                tMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_toggle_mode"),
                                PersistentDataType.STRING, mode.equals("BUY") ? "SELL" : "BUY");
                tMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_cat_id"),
                                PersistentDataType.STRING, categoryId);
                tMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_page"),
                                PersistentDataType.INTEGER, page);
                toggle.setItemMeta(tMeta);
                gui.setItem(53, toggle);

                player.openInventory(gui);
        }
}
