package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;

import java.util.*;

public class SkytreeGuide {

        private final SkytreePlugin plugin;
        private final ItemRegistry itemRegistry;
        private final NamespacedKey categoryKey;

        public SkytreeGuide(SkytreePlugin plugin, ItemRegistry itemRegistry) {
                this.plugin = plugin;
                this.itemRegistry = itemRegistry;
                this.categoryKey = new NamespacedKey(plugin, "guide_category");
        }

        // Open Main Menu (Split: Recipes vs Machines vs Quests)
        public void open(Player player) {
                Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§2§lSkytree Guide §8» §fMenu"));
                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.GREEN_STAINED_GLASS_PANE,
                                Material.LIME_STAINED_GLASS_PANE);

                // 1. Item Recipes
                ItemStack recipes = createItem(Material.KNOWLEDGE_BOOK, "§a§lItem Recipes",
                                "§7Browse custom items and", "§7their crafting recipes.", "", "§e> Click to Browse");
                ItemMeta meta = recipes.getItemMeta();
                meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, "MENU_RECIPES");
                recipes.setItemMeta(meta);
                gui.setItem(11, recipes);

                // 2. Machine Guide
                ItemStack machines = createItem(Material.WRITTEN_BOOK, "§6§lMachine Guide",
                                "§7Learn how to build and use", "§7Skytree machines.", "", "§e> Click to Learn");
                ItemMeta meta2 = machines.getItemMeta();
                meta2.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, "MENU_MACHINES");
                machines.setItemMeta(meta2);
                gui.setItem(13, machines);

                // 3. Quest Tutorial
                ItemStack quests = createItem(Material.COMPASS, "§e§lQuest Tutorial",
                                "§7Step-by-step beginner", "§7objectives and goals.", "", "§e> Click to View");
                ItemMeta meta3 = quests.getItemMeta();
                meta3.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, "MENU_QUESTS");
                quests.setItemMeta(meta3);
                gui.setItem(15, quests);

                player.openInventory(gui);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        }

        // Open Recipe Categories (Old Main Menu)
        public void openRecipeCategories(Player player) {
                Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse("§2§lSkytree Recipes"));
                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.KNOWLEDGE_BOOK, Material.BOOK);

                // --- Categories ---

                // 1. Basic Machines
                gui.setItem(10, createCategoryItem(Material.CRAFTING_TABLE, "Basic Machines", "basic_machines",
                                "§7Fundamental machines for", "§7survival in the void.", "", "§e> Click to Open"));

                // 2. Advanced Machines
                gui.setItem(11, createCategoryItem(Material.PISTON, "Advanced Machines", "advanced_machines",
                                "§7Automation and processing", "§7machinery.", "", "§e> Click to Open"));

                // 3. Tools & Weapons
                gui.setItem(13, createCategoryItem(Material.DIAMOND_PICKAXE, "Tools & Weapons", "tools",
                                "§7Crooks, Hammers, and", "§7special utility tools.", "", "§e> Click to Open"));

                // 4. Resources
                gui.setItem(15, createCategoryItem(Material.IRON_INGOT, "Resources", "resources",
                                "§7Dusts, Ingots, Pebbles,", "§7and Compressed Blocks.", "", "§e> Click to Open"));

                // 5. Magical Items
                gui.setItem(28, createCategoryItem(Material.BLAZE_ROD, "Magical Items", "magic",
                                "§7Mystical Essences and", "§7other arcane materials.", "", "§e> Click to Open"));

                // 6. Farming & Nature
                gui.setItem(30, createCategoryItem(Material.WHEAT, "Farming & Nature", "farming",
                                "§7Seeds, Saplings, and", "§7Biological resources.", "", "§e> Click to Open"));

                // 7. Storage & Transport
                gui.setItem(32, createCategoryItem(Material.CHEST, "Storage & Transport", "storage",
                                "§7Pipes, Storage Systems,", "§7and Barrels.", "", "§e> Click to Open"));

                // Back to Main
                gui.setItem(45, createItem(Material.ARROW, "§cBack to Menu", "§7Click to return"));

                player.openInventory(gui);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
        }

        public void openMachineGuide(Player player) {
                Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse("§2§lMachine Building Guide"));
                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.IRON_INGOT, Material.COAL);

                gui.setItem(45, createItem(Material.ARROW, "§cBack to Menu", "§7Click to return"));

                // Sieve
                gui.setItem(10, createItem(Material.OAK_FENCE, "§eSieve",
                                "§7Filters items through a mesh.",
                                "§6Construction:",
                                "§f[TOP]    §7Oak Fence",
                                "§f[BOT]    §7Chest",
                                "",
                                "§aUsage:",
                                "§7Right-click Fence with Mesh",
                                "§7Put items in Chest."));

                // Barrel
                gui.setItem(11, createItem(Material.BARREL, "§eWooden Barrel",
                                "§7Turns organic items into dirt/clay.",
                                "§6Construction:",
                                "§f[TOP]    §7Wooden Trapdoor",
                                "§f[BOT]    §7Barrel",
                                "",
                                "§aUsage:",
                                "§7Right-click with organic items."));

                // Crucible
                gui.setItem(12, createItem(Material.CAULDRON, "§eCrucible",
                                "§7Melts stones into liquids.",
                                "§6Construction:",
                                "§f[TOP]    §7Cauldron",
                                "§f[BOT]    §7Heat Source (Fire/Lava)",
                                "",
                                "§aUsage:",
                                "§7Right-click with Cobble/Leaves."));

                // Compressor
                gui.setItem(13, createItem(Material.PISTON, "§fCompressor",
                                "§7Compresses 9 items into 1 block.",
                                "§6Construction:",
                                "§f[TOP]    §7Piston",
                                "§f[BOT]    §7Dispenser",
                                "",
                                "§aUsage:",
                                "§7Put items in Dispenser.",
                                "§7Right-click Piston."));

                // Pulverizer
                gui.setItem(14, createItem(Material.IRON_BLOCK, "§7Pulverizer",
                                "§7Crushes ores into dust.",
                                "§6Construction:",
                                "§f[TOP]    §7Iron Block",
                                "§f[BOT]    §7Dispenser",
                                "",
                                "§aUsage:",
                                "§7Put items in Dispenser.",
                                "§7Right-click Iron Block."));

                // Electric Furnace
                gui.setItem(15, createItem(Material.BLAST_FURNACE, "§6Electric Furnace",
                                "§7Smelts dusts instantly.",
                                "§6Construction:",
                                "§f[TOP]    §7Iron Trapdoor",
                                "§f[BOT]    §7Blast Furnace",
                                "",
                                "§aUsage:",
                                "§7Right-click Blast Furnace with dust."));

                // Cobble Gen
                gui.setItem(16, createItem(Material.DISPENSER, "§7Cobblestone Generator",
                                "§7Generates Cobble automatically.",
                                "§6Construction:",
                                "§7Just place the block!",
                                "",
                                "§aUsage:",
                                "§7Generates cobble in inventory",
                                "§7or drops it below."));

                player.openInventory(gui);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
        }

        // Open Specific Category
        public void openCategory(Player player, String categoryId) {
                Inventory gui = Bukkit.createInventory(null, 54,
                                ComponentUtil.parse("§2Guide: " + capitalize(categoryId.replace("_", " "))));
                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.PAPER, Material.MAP);

                // Back Button (To Recipe Categories)
                ItemStack back = createItem(Material.ARROW, "§cBack to Recipes", "§7Click to return");
                ItemMeta meta = back.getItemMeta();
                meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, "MENU_RECIPES");
                back.setItemMeta(meta);
                gui.setItem(45, back);

                // Get items for category
                List<ItemStack> items = getItemsForCategory(categoryId);

                // Populate (Simple implementation: Max 45 items per category for now)
                int slot = 0;
                for (ItemStack item : items) {
                        // Find next empty slot (avoiding borders if we wanted, but let's just use grid)
                        // We skip the back button slot
                        if (slot == 45)
                                slot++;
                        if (slot >= 54)
                                break;

                        // Clone item and add "Click to view recipe" lore
                        ItemStack displayItem = item.clone();
                        ItemMeta displayMeta = displayItem.getItemMeta();
                        List<Component> lore = displayMeta.hasLore() ? displayMeta.lore() : new ArrayList<>();
                        if (lore == null)
                                lore = new ArrayList<>();
                        lore.add(ComponentUtil.parse(""));
                        lore.add(ComponentUtil.parse("§eClick to view Recipe/Usage"));
                        displayMeta.lore(lore);

                        // FIX: Set item_id PDC so listener can find it
                        String itemId = ((com.wiredid.skytree.impl.SkytreeItemRegistry) itemRegistry).getItemId(item);
                        if (itemId != null) {
                                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "item_id");
                                displayMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, itemId);
                        }

                        displayItem.setItemMeta(displayMeta);

                        gui.setItem(slot, displayItem);
                        slot++;
                }

                player.openInventory(gui);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        }

        private List<ItemStack> getItemsForCategory(String category) {
                List<ItemStack> list = new ArrayList<>();
                // Iterate all items and filter by prefix/type
                // In a real optimized system, we'd have a map. Here we filter on the fly.

                for (String id : ((com.wiredid.skytree.impl.SkytreeItemRegistry) itemRegistry).getAllItemIds()) {
                        if (matchesCategory(id, category)) {
                                ItemStack item = itemRegistry.getItem(id);
                                if (item != null)
                                        list.add(item);
                        }
                }

                // Sort vaguely alphabetically or by order?
                // Let's just keep them in insertion order if possible, or simple sort
                list.sort(Comparator.comparing(this::getDisplayName));
                return list;
        }

        private String getDisplayName(ItemStack item) {
                if (item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && meta.hasDisplayName()) {
                                Component displayName = meta.displayName();
                                return displayName != null ? ComponentUtil.toLegacy(displayName) : "";
                        }
                }
                return "";
        }

        private boolean matchesCategory(String id, String category) {
                switch (category) {
                        case "basic_machines":
                                return id.contains("sieve") || id.contains("barrel") || id.contains("crucible")
                                                || id.equals("cobble_gen") || id.startsWith("mesh_")
                                                || id.equals("juicer");
                        case "advanced_machines":
                                return id.equals("compressor") || id.equals("pulverizer")
                                                || id.equals("furnace_advanced");
                        case "tools":
                                return id.startsWith("hammer_") || id.startsWith("crook_") || id.contains("axe")
                                                || id.contains("shovel") || id.contains("drill") || id.contains("hoe")
                                                || id.contains("pickaxe") || id.contains("blade")
                                                || id.contains("sword") || id.contains("dagger")
                                                || id.equals("divine_excavator") || id.equals("divinebreaker")
                                                || id.equals("mjolnir") || id.equals("skyshaper_lance")
                                                // Armor included in tools for now
                                                || id.contains("helmet") || id.contains("chestplate")
                                                || id.contains("leggings") || id.contains("boots")
                                                || id.equals("godwings") || id.contains("armor");
                        case "resources":
                                return id.startsWith("pebble_") || id.startsWith("dust_") || id.startsWith("piece_")
                                                || id.startsWith("compressed_")
                                                || id.startsWith("ingot_") || id.contains("shard")
                                                || id.contains("core") || id.contains("fragment");
                        case "magic":
                                return id.startsWith("essence_") || id.startsWith("bottle")
                                                || id.contains("orb") || id.contains("rune") || id.contains("scroll")
                                                || id.contains("key");
                        case "farming":
                                return id.startsWith("seed_") || id.startsWith("sapling_")
                                                || id.startsWith("silkworm") || id.equals("silk_mesh")
                                                || id.contains("crop") || id.contains("fruit");
                        case "storage":
                                return id.equals("item_pipe") || id.equals("storage_controller")
                                                || id.equals("powered_spawner")
                                                || id.equals("spawner_core") || id.contains("chest");
                        default:
                                return false;
                }
        }

        private ItemStack createItem(Material material, String name, String... lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        meta.displayName(ComponentUtil.parse(name));
                        List<Component> l = ComponentUtil.parseList(lore);
                        meta.lore(l);
                        item.setItemMeta(meta);
                }
                return item;
        }

        private ItemStack createCategoryItem(Material material, String name, String categoryId, String... lore) {
                ItemStack item = createItem(material, "§6" + name, lore);
                ItemMeta meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, categoryId);
                item.setItemMeta(meta);
                return item;
        }

        private String capitalize(String str) {
                if (str == null || str.isEmpty())
                        return str;
                return str.substring(0, 1).toUpperCase() + str.substring(1);
        }

        /**
         * Open Quest Tutorial page with beginner objectives
         */
        public void openQuestTutorial(Player player) {
                Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse("§6§lGuide §8» §7Tutorials"));
                com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.CYAN_STAINED_GLASS_PANE,
                                Material.BLUE_STAINED_GLASS_PANE);

                gui.setItem(45, createItem(Material.ARROW, "§cBack to Menu", "§7Click to return"));

                gui.setItem(10, createItem(Material.OAK_SAPLING, "§a§l1. Early Start (Wood Only)",
                                "§7Awal game: kayu, daun, sapling.",
                                "§7Farm tree dulu sampai stabil."));

                gui.setItem(11, createItem(Material.BARREL, "§a§l2. Dirt Progression",
                                "§7Barrel dulu, jangan lompat ke mesin.",
                                "§74 saplings = 1 level", "§78 levels = 1 dirt"));

                gui.setItem(19, createItem(Material.COBBLESTONE, "§a§l3. Stone Phase",
                                "§7Buka jalur cobble + furnace + iron.",
                                "§7Setelah ini baru realistis ke mesin."));

                gui.setItem(20, createItem(Material.OAK_FENCE, "§a§l4. Sieve Mid-Game",
                                "§7Sieve bukan starter block.",
                                "§7Bangun setelah chest/dropper masuk akal."));

                gui.setItem(28, createItem(Material.PISTON, "§a§l5. Automation Late-Mid",
                                "§7Compressor/pulverizer setelah",
                                "§7resource & redstone sudah stabil."));

                player.openInventory(gui);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
        }
}



