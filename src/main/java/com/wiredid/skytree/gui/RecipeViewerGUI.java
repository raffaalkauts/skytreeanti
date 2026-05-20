package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.RecipeService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Recipe Viewer GUI
 * Shows crafting recipes in 3x3 grid + sources
 */
public class RecipeViewerGUI {

        private final ItemRegistry itemRegistry;

        // Store all crafting recipes
        private final Map<String, CraftingRecipe> craftingRecipes = new HashMap<>();

        public RecipeViewerGUI(SkytreePlugin plugin, ItemRegistry itemRegistry, RecipeService recipeService) {
                this.itemRegistry = itemRegistry;
                registerAllCraftingRecipes();
        }

        public void open(Player player, String itemId) {
                Inventory gui = Bukkit.createInventory(null, 54,
                                ComponentUtil.parse("§6§lRecipe: " + formatName(itemId)));

                // Get the item
                ItemStack item = itemRegistry.getItem(itemId);
                if (item == null) {
                        player.sendMessage("§c§l[Guide] §cItem not found!");
                        return;
                }

                // Background
                fillBackground(gui);

                // Display result item (slot 25 - center right)
                gui.setItem(25, item);

                // Check if has crafting recipe
                CraftingRecipe recipe = craftingRecipes.get(itemId);
                if (recipe != null) {
                        // Display crafting grid (3x3 centered at slots 10,11,12,19,20,21,28,29,30)
                        displayCraftingGrid(gui, recipe);

                        // Arrow indicator (slot 24)
                        ItemStack arrow = new ItemStack(Material.ARROW);
                        ItemMeta arrowMeta = arrow.getItemMeta();
                        arrowMeta.displayName(ComponentUtil.parse("§e§l→ Crafting Result"));
                        arrow.setItemMeta(arrowMeta);
                        gui.setItem(24, arrow);
                } else {
                        // No crafting recipe, show alternative sources
                        displayAlternativeSources(gui, itemId);
                }

                // Back button
                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.displayName(ComponentUtil.parse("§cBack"));
                back.setItemMeta(backMeta);
                gui.setItem(49, back);

                player.openInventory(gui);
        }

        private void displayCraftingGrid(Inventory gui, CraftingRecipe recipe) {
                // Crafting grid slots (3x3)
                int[] gridSlots = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };

                for (int i = 0; i < recipe.ingredients.length && i < 9; i++) {
                        ItemStack ingredient = recipe.ingredients[i];
                        if (ingredient != null && ingredient.getType() != Material.AIR) {
                                gui.setItem(gridSlots[i], ingredient);
                        }
                }

                // Recipe info
                ItemStack info = new ItemStack(Material.BOOK);
                ItemMeta meta = info.getItemMeta();
                meta.displayName(ComponentUtil.parse("§e§lCrafting Recipe"));
                meta.lore(ComponentUtil.parseList(
                                "§7Place items in crafting table",
                                "§7as shown in the 3x3 grid",
                                "",
                                "§aOutput: §e" + recipe.outputAmount + "x"));
                info.setItemMeta(meta);
                gui.setItem(16, info);
        }

        private void displayAlternativeSources(Inventory gui, String itemId) {
                int slot = 19;

                // Sieve source
                if (itemId.contains("pebble") || itemId.contains("piece") || itemId.contains("dust")) {
                        ItemStack sieveIcon = new ItemStack(Material.OAK_FENCE);
                        ItemMeta meta = sieveIcon.getItemMeta();
                        meta.displayName(ComponentUtil.parse("§aSieve Recipe"));
                        meta.lore(ComponentUtil.parseList("§7Sieve dirt/gravel/sand", "§7with appropriate mesh"));
                        sieveIcon.setItemMeta(meta);
                        gui.setItem(slot++, sieveIcon);
                }

                // Shop source (always available)
                ItemStack shopIcon = new ItemStack(Material.EMERALD);
                ItemMeta shopMeta = shopIcon.getItemMeta();
                shopMeta.displayName(ComponentUtil.parse("§aAvailable in Shop"));
                shopMeta.lore(ComponentUtil.parseList("§7Buy from §e/shop"));
                shopIcon.setItemMeta(shopMeta);
                gui.setItem(slot++, shopIcon);
        }

        private void fillBackground(Inventory gui) {
                ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = bg.getItemMeta();
                meta.displayName(ComponentUtil.parse(" "));
                bg.setItemMeta(meta);

                for (int i = 0; i < 54; i++) {
                        // Skip crafting grid area and result area
                        if (!isCraftingSlot(i) && i != 24 && i != 25 && i != 49 && i != 16) {
                                gui.setItem(i, bg);
                        }
                }
        }

        private boolean isCraftingSlot(int slot) {
                int[] craftingSlots = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
                for (int s : craftingSlots) {
                        if (slot == s)
                                return true;
                }
                return false;
        }

        private String formatName(String itemId) {
                return itemId.replace("_", " ").toUpperCase();
        }

        // ==================== CRAFTING RECIPES ====================

        private void registerAllCraftingRecipes() {
                // CROOKS (5 tiers)
                registerRecipe("crook_wood", 1,
                                new ItemStack(Material.STICK), new ItemStack(Material.STICK), null,
                                null, new ItemStack(Material.STICK), null,
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("crook_stone", 1,
                                new ItemStack(Material.COBBLESTONE), new ItemStack(Material.COBBLESTONE), null,
                                null, new ItemStack(Material.STICK), null,
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("crook_iron", 1,
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT), null,
                                null, new ItemStack(Material.STICK), null,
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("crook_gold", 1,
                                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.GOLD_INGOT), null,
                                null, new ItemStack(Material.STICK), null,
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("crook_diamond", 1,
                                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND), null,
                                null, new ItemStack(Material.STICK), null,
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("crook_emerald", 1,
                                new ItemStack(Material.EMERALD), new ItemStack(Material.EMERALD), null,
                                null, new ItemStack(Material.STICK), null,
                                null, new ItemStack(Material.STICK), null);

                // HAMMERS (6 tiers)
                registerRecipe("hammer_wood", 1,
                                new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.OAK_PLANKS),
                                new ItemStack(Material.OAK_PLANKS),
                                new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.STICK),
                                new ItemStack(Material.OAK_PLANKS),
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("hammer_stone", 1,
                                new ItemStack(Material.COBBLESTONE), new ItemStack(Material.COBBLESTONE),
                                new ItemStack(Material.COBBLESTONE),
                                new ItemStack(Material.COBBLESTONE), new ItemStack(Material.STICK),
                                new ItemStack(Material.COBBLESTONE),
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("hammer_iron", 1,
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.STICK),
                                new ItemStack(Material.IRON_INGOT),
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("hammer_gold", 1,
                                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.GOLD_INGOT),
                                new ItemStack(Material.GOLD_INGOT),
                                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.STICK),
                                new ItemStack(Material.GOLD_INGOT),
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("hammer_diamond", 1,
                                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
                                new ItemStack(Material.DIAMOND),
                                new ItemStack(Material.DIAMOND), new ItemStack(Material.STICK),
                                new ItemStack(Material.DIAMOND),
                                null, new ItemStack(Material.STICK), null);

                registerRecipe("hammer_netherite", 1,
                                new ItemStack(Material.NETHERITE_INGOT), new ItemStack(Material.NETHERITE_INGOT),
                                new ItemStack(Material.NETHERITE_INGOT),
                                new ItemStack(Material.NETHERITE_INGOT), new ItemStack(Material.STICK),
                                new ItemStack(Material.NETHERITE_INGOT),
                                null, new ItemStack(Material.STICK), null);

                // MESHES (6 tiers)
                registerRecipe("mesh_string", 1,
                                new ItemStack(Material.STRING), new ItemStack(Material.STRING),
                                new ItemStack(Material.STRING),
                                new ItemStack(Material.STRING), new ItemStack(Material.STICK),
                                new ItemStack(Material.STRING),
                                new ItemStack(Material.STRING), new ItemStack(Material.STRING),
                                new ItemStack(Material.STRING));

                registerRecipe("mesh_flint", 1,
                                new ItemStack(Material.FLINT), new ItemStack(Material.FLINT),
                                new ItemStack(Material.FLINT),
                                new ItemStack(Material.FLINT), new ItemStack(Material.STICK),
                                new ItemStack(Material.FLINT),
                                new ItemStack(Material.FLINT), new ItemStack(Material.FLINT),
                                new ItemStack(Material.FLINT));

                registerRecipe("mesh_iron", 1,
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.STICK),
                                new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT));

                registerRecipe("mesh_gold", 1,
                                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.GOLD_INGOT),
                                new ItemStack(Material.GOLD_INGOT),
                                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.STICK),
                                new ItemStack(Material.GOLD_INGOT),
                                new ItemStack(Material.GOLD_INGOT), new ItemStack(Material.GOLD_INGOT),
                                new ItemStack(Material.GOLD_INGOT));

                registerRecipe("mesh_diamond", 1,
                                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
                                new ItemStack(Material.DIAMOND),
                                new ItemStack(Material.DIAMOND), new ItemStack(Material.STICK),
                                new ItemStack(Material.DIAMOND),
                                new ItemStack(Material.DIAMOND), new ItemStack(Material.DIAMOND),
                                new ItemStack(Material.DIAMOND));

                registerRecipe("mesh_emerald", 1,
                                new ItemStack(Material.EMERALD), new ItemStack(Material.EMERALD),
                                new ItemStack(Material.EMERALD),
                                new ItemStack(Material.EMERALD), new ItemStack(Material.STICK),
                                new ItemStack(Material.EMERALD),
                                new ItemStack(Material.EMERALD), new ItemStack(Material.EMERALD),
                                new ItemStack(Material.EMERALD));

                // MACHINES
                registerRecipe("sieve", 1,
                                new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.OAK_PLANKS),
                                new ItemStack(Material.OAK_PLANKS),
                                new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.STRING),
                                new ItemStack(Material.OAK_PLANKS),
                                new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.OAK_PLANKS),
                                new ItemStack(Material.OAK_PLANKS));

                registerRecipe("barrel", 1,
                                new ItemStack(Material.OAK_LOG), new ItemStack(Material.OAK_SLAB),
                                new ItemStack(Material.OAK_LOG),
                                new ItemStack(Material.OAK_LOG), null, new ItemStack(Material.OAK_LOG),
                                new ItemStack(Material.OAK_LOG), new ItemStack(Material.OAK_SLAB),
                                new ItemStack(Material.OAK_LOG));

                registerRecipe("crucible", 1,
                                new ItemStack(Material.TERRACOTTA), new ItemStack(Material.TERRACOTTA),
                                new ItemStack(Material.TERRACOTTA),
                                new ItemStack(Material.TERRACOTTA), null, new ItemStack(Material.TERRACOTTA),
                                new ItemStack(Material.TERRACOTTA), new ItemStack(Material.TERRACOTTA),
                                new ItemStack(Material.TERRACOTTA));

                registerRecipe("compressor", 1,
                                new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.PISTON),
                                new ItemStack(Material.IRON_BLOCK),
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_BLOCK),
                                new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK),
                                new ItemStack(Material.IRON_BLOCK));

                registerRecipe("pulverizer", 1,
                                new ItemStack(Material.FLINT), new ItemStack(Material.FLINT),
                                new ItemStack(Material.FLINT),
                                new ItemStack(Material.COBBLESTONE), new ItemStack(Material.PISTON),
                                new ItemStack(Material.COBBLESTONE),
                                new ItemStack(Material.COBBLESTONE), new ItemStack(Material.COBBLESTONE),
                                new ItemStack(Material.COBBLESTONE));

                registerRecipe("furnace_advanced", 1,
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.BLAST_FURNACE),
                                new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_INGOT),
                                new ItemStack(Material.IRON_INGOT));

                // NEW RECIPES
                registerRecipe("cobble_gen", 1,
                                new ItemStack(Material.COBBLESTONE), new ItemStack(Material.COBBLESTONE),
                                new ItemStack(Material.COBBLESTONE),
                                new ItemStack(Material.WATER_BUCKET), new ItemStack(Material.GLASS),
                                new ItemStack(Material.LAVA_BUCKET),
                                new ItemStack(Material.COBBLESTONE), new ItemStack(Material.COBBLESTONE),
                                new ItemStack(Material.COBBLESTONE));

                registerRecipe("spawner_core", 1,
                                null, new ItemStack(Material.GOLD_BLOCK), null,
                                new ItemStack(Material.GOLD_BLOCK), new ItemStack(Material.NETHER_STAR),
                                new ItemStack(Material.GOLD_BLOCK),
                                null, new ItemStack(Material.GOLD_BLOCK), null);

                registerRecipe("powered_spawner", 1,
                                new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK),
                                new ItemStack(Material.IRON_BLOCK),
                                new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.NETHER_STAR),
                                new ItemStack(Material.IRON_BLOCK),
                                new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK),
                                new ItemStack(Material.IRON_BLOCK));

                registerRecipe("item_pipe", 8,
                                null, null, null,
                                new ItemStack(Material.IRON_INGOT), new ItemStack(Material.GLASS),
                                new ItemStack(Material.IRON_INGOT),
                                null, null, null);

                registerRecipe("storage_controller", 1,
                                new ItemStack(Material.REDSTONE), new ItemStack(Material.CHEST),
                                new ItemStack(Material.REDSTONE),
                                new ItemStack(Material.CHEST), new ItemStack(Material.DIAMOND),
                                new ItemStack(Material.CHEST),
                                new ItemStack(Material.REDSTONE), new ItemStack(Material.CHEST),
                                new ItemStack(Material.REDSTONE));
        }

        private void registerRecipe(String itemId, int outputAmount, ItemStack... ingredients) {
                craftingRecipes.put(itemId, new CraftingRecipe(ingredients, outputAmount));
        }

        private static class CraftingRecipe {
                ItemStack[] ingredients;
                int outputAmount;

                CraftingRecipe(ItemStack[] ingredients, int outputAmount) {
                        this.ingredients = ingredients;
                        this.outputAmount = outputAmount;
                }
        }
}
