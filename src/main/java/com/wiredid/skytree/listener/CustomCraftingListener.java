package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.Keyed;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public class CustomCraftingListener implements Listener {

    private final SkytreePlugin plugin;
    private final ItemRegistry itemRegistry;

    public CustomCraftingListener(SkytreePlugin plugin, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        boolean hasCustomIngredient = false;
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR && itemRegistry.isCustomItem(item)) {
                hasCustomIngredient = true;
                break;
            }
        }

        if (!hasCustomIngredient) return;

        String recipeKey = extractRecipeKey(recipe);
        if (recipeKey == null) return;

        if (!matchesCustomRecipe(matrix, recipeKey)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        boolean hasCustomIngredient = false;
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR && itemRegistry.isCustomItem(item)) {
                hasCustomIngredient = true;
                break;
            }
        }
        if (!hasCustomIngredient) return;

        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
            if (player.hasMetadata("NPC")) return;
            if (plugin.getJobService() != null) {
                ItemStack result = event.getRecipe().getResult();
                double worth = plugin.getWorthService().getItemSellPrice(result);
                plugin.getJobService().handleJobAction(player, "crafter", Math.max(worth, 3.0));
            }
        }
    }

    private String extractRecipeKey(Recipe recipe) {
        if (recipe instanceof Keyed keyed) {
            return keyed.getKey().getKey();
        }
        return null;
    }

    private boolean matchesCustomRecipe(ItemStack[] matrix, String recipeKey) {
        ItemStack[] ingredients = getRecipeIngredients(recipeKey);
        if (ingredients == null) return true;

        for (int i = 0; i < 9; i++) {
            ItemStack input = matrix[i];
            ItemStack expected = ingredients[i];

            if (expected == null || expected.getType() == Material.AIR) {
                if (input != null && input.getType() != Material.AIR) return false;
                continue;
            }

            if (input == null || input.getType() == Material.AIR) return false;

            if (input.getType() != expected.getType()) return false;

            String inputId = itemRegistry.getItemId(input);
            String expectedId = itemRegistry.getItemId(expected);

            if (expectedId != null && !expectedId.equals(inputId)) return false;
        }
        return true;
    }

    private java.util.Map<String, ItemStack[]> recipeCache = new java.util.HashMap<>();

    private ItemStack[] getRecipeIngredients(String recipeKey) {
        if (recipeCache.containsKey(recipeKey)) return recipeCache.get(recipeKey);

        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(recipeKey);
        if (key == null) return null;

        Recipe recipe = plugin.getServer().getRecipesFor(new ItemStack(Material.AIR)).stream()
                .filter(r -> r instanceof Keyed k && k.getKey().equals(key))
                .findFirst().orElse(null);

        if (recipe == null) return null;

        ItemStack[] matrix = new ItemStack[9];
        if (recipe instanceof ShapedRecipe shaped) {
            String[] shape = shaped.getShape();
            java.util.Map<Character, RecipeChoice> choiceMap = shaped.getChoiceMap();
            java.util.Map<Character, ItemStack> ingredientMap = new java.util.HashMap<>();
            for (var entry : choiceMap.entrySet()) {
                RecipeChoice rc = entry.getValue();
                if (rc != null) {
                    ItemStack rep;
                    if (rc instanceof RecipeChoice.MaterialChoice mc) {
                        rep = new ItemStack(mc.getChoices().get(0));
                    } else if (rc instanceof RecipeChoice.ExactChoice ec) {
                        rep = ec.getChoices().get(0).clone();
                    } else {
                        rep = new ItemStack(Material.AIR);
                    }
                    ingredientMap.put(entry.getKey(), rep);
                }
            }
            int row = 0;
            for (String s : shape) {
                for (int col = 0; col < s.length(); col++) {
                    char c = s.charAt(col);
                    ItemStack ing = ingredientMap.get(c);
                    if (ing != null) matrix[row * 3 + col] = ing.clone();
                }
                row++;
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            int idx = 0;
            for (var choice : shapeless.getChoiceList()) {
                if (idx < 9 && choice != null) {
                    if (choice instanceof RecipeChoice.MaterialChoice mc) {
                        matrix[idx] = new ItemStack(mc.getChoices().get(0));
                    } else if (choice instanceof RecipeChoice.ExactChoice ec) {
                        matrix[idx] = ec.getChoices().get(0).clone();
                    }
                    idx++;
                }
            }
        }

        recipeCache.put(recipeKey, matrix);
        return matrix;
    }
}
