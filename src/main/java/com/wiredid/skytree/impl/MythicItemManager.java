package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;

import com.wiredid.skytree.util.ComponentUtil;
import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.stream.Collectors;

public class MythicItemManager {

    private final SkytreePlugin plugin;
    private final SkytreeItemRegistry registry;
    private final SkytreeRecipeService recipeService;
    private final Gson gson;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private MythicConfig config; // Store loaded config

    public MythicItemManager(SkytreePlugin plugin, SkytreeItemRegistry registry, SkytreeRecipeService recipeService) {
        this.plugin = plugin;
        this.registry = registry;
        this.recipeService = recipeService;
        this.gson = new Gson();
    }

    public void load() {
        plugin.getLogger().info("Loading Mythic Items...");

        java.io.File file = new java.io.File(plugin.getDataFolder(), "mythic_items.json");
        if (!file.exists()) {
            plugin.saveResource("mythic_items.json", false);
        }

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                Reader reader = new InputStreamReader(fis)) {

            MythicConfig config = gson.fromJson(reader, MythicConfig.class);
            if (config == null) {
                plugin.getLogger().severe("Failed to parse mythic_items.json (result was null)");
                return;
            }

            this.config = config;

            // Pass 1: Register all items first
            registerItemDefinitions(config.items);

            // Pass 2: Register recipes
            registerEmbeddedRecipes(config.items);
            registerRecipes(config.crafting_recipes);
            registerGlobalFurnaceRecipes(config.furnace_recipes);

            plugin.getLogger().info("Loaded " + (config.items != null ? config.items.size() : 0) + " mythic items.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading mythic_items.json", e);
        }
    }

    public void reload() {
        // Clear existing recipes
        for (NamespacedKey key : recipeKeys) {
            plugin.getServer().removeRecipe(key);
        }
        recipeKeys.clear();

        // Re-load
        load();
    }

    public void unlockRecipes(org.bukkit.entity.Player player) {
        if (player == null)
            return;
        for (NamespacedKey key : recipeKeys) {
            player.discoverRecipe(key);
        }

        // Also unlock all recipes registered via SkytreeRecipeService (which use item
        // ID as key)
        for (String id : registry.getAllItemIds()) {
            try {
                NamespacedKey key = new NamespacedKey(plugin, id);
                // We can just try to discover it. If it doesn't exist, it might do nothing or
                // ignored.
                // Bukkit's discoverRecipe doesn't throw if recipe doesn't exist usually, but
                // let's be safe.
                player.discoverRecipe(key);
            } catch (Exception e) {
                // Ignore invalid keys
            }
        }
    }

    private void registerItemDefinitions(List<MythicItemDef> items) {
        if (items == null)
            return;

        for (MythicItemDef def : items) {
            try {
                if (def.id == null || def.baseItem == null || def.id.startsWith("__ITEMS_PLACEHOLDER"))
                    continue;

                Material mat = Material.matchMaterial(def.baseItem);
                if (mat == null) {
                    plugin.getLogger().warning("Invalid material for item " + def.id + ": " + def.baseItem);
                    mat = Material.STONE; // Fallback
                }

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(ComponentUtil.parse(def.name != null ? def.name : def.id));

                    List<String> lore = new ArrayList<>();
                    if (def.description != null)
                        lore.add("§7" + def.description);
                    if (def.usage != null)
                        lore.add("§eUsage: §f" + def.usage);
                    if (def.effects != null) {
                        if (def.effects.positive != null && !def.effects.positive.isEmpty()
                                && !"None".equals(def.effects.positive))
                            lore.add("§aEffect: " + def.effects.positive);
                        if (def.effects.negative != null && !def.effects.negative.isEmpty()
                                && !"None".equals(def.effects.negative))
                            lore.add("§cDrawback: " + def.effects.negative);
                    }
                    if (def.rarity != null)
                        lore.add(getRarityColor(def.rarity) + "§l" + def.rarity.toUpperCase());

                    List<Component> loreComponents = lore.stream().map(ComponentUtil::parse)
                            .collect(Collectors.toList());
                    meta.lore(loreComponents);

                    // Custom Data
                    PersistentDataContainer data = meta.getPersistentDataContainer();
                    data.set(new NamespacedKey(plugin, "mythic_id"), PersistentDataType.STRING, def.id);
                    if (def.category != null) {
                        data.set(new NamespacedKey(plugin, "mythic_category"), PersistentDataType.STRING, def.category);
                    }

                    // Handle Custom NBT/ModelData from JSON
                    if (def.nbt != null) {
                        for (Map.Entry<String, Object> entry : def.nbt.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();

                            if (key.equals("customModelData") && value instanceof Number) {
                                meta.setCustomModelData(((Number) value).intValue());
                                continue;
                            }

                            // Store other properties in PDC
                            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
                            if (value instanceof Boolean) {
                                // Store boolean as byte (0 or 1)
                                data.set(namespacedKey, PersistentDataType.BYTE, (byte) ((Boolean) value ? 1 : 0));
                            } else if (value instanceof Number) {
                                double dVal = ((Number) value).doubleValue();
                                if (dVal == Math.rint(dVal) && !Double.isInfinite(dVal)) {
                                    data.set(namespacedKey, PersistentDataType.INTEGER, ((Number) value).intValue());
                                } else {
                                    data.set(namespacedKey, PersistentDataType.DOUBLE, dVal);
                                }
                            } else if (value instanceof String) {
                                data.set(namespacedKey, PersistentDataType.STRING, (String) value);
                            } else if (value instanceof List) {
                                List<?> list = (List<?>) value;
                                if (!list.isEmpty() && list.get(0) instanceof Number) {
                                    int[] ints = list.stream().mapToInt(o -> ((Number) o).intValue()).toArray();
                                    data.set(namespacedKey, PersistentDataType.INTEGER_ARRAY, ints);
                                }
                            }

                            // Attribute Handling
                            applyAttribute(meta, key, value);
                        }
                    }

                    item.setItemMeta(meta);
                }

                registry.registerCustomItem(def.id.toLowerCase(), item);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to register item: " + def.id, e);
            }
        }
    }

    private void applyAttribute(ItemMeta meta, String key, Object value) {
        if (!(value instanceof Number))
            return;
        double val = ((Number) value).doubleValue();

        Attribute attribute = null;
        AttributeModifier.Operation op = AttributeModifier.Operation.ADD_NUMBER;
        EquipmentSlot slot = null;

        switch (key) {
            case "attackDamage":
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                slot = EquipmentSlot.HAND;
                break;
            case "attackSpeed":
                attribute = Attribute.GENERIC_ATTACK_SPEED;
                slot = EquipmentSlot.HAND;
                break;
            case "healthBonus":
                attribute = Attribute.GENERIC_MAX_HEALTH;
                break;
            case "armorBonus":
                attribute = Attribute.GENERIC_ARMOR;
                break;
            case "toughnessBonus":
                attribute = Attribute.GENERIC_ARMOR_TOUGHNESS;
                break;
            case "speedBonus":
                attribute = Attribute.GENERIC_MOVEMENT_SPEED;
                op = AttributeModifier.Operation.ADD_SCALAR;
                break;
        }

        if (attribute != null) {
            // Use NamespacedKey-based constructor for 1.21+ compatibility
            NamespacedKey modifierKey = new NamespacedKey(plugin, "mythic_" + key.toLowerCase());

            // Convert EquipmentSlot to EquipmentSlotGroup for 1.21+
            org.bukkit.inventory.EquipmentSlotGroup slotGroup = null;
            if (slot != null) {
                switch (slot) {
                    case HAND:
                    case OFF_HAND:
                        slotGroup = org.bukkit.inventory.EquipmentSlotGroup.HAND;
                        break;
                    case FEET:
                        slotGroup = org.bukkit.inventory.EquipmentSlotGroup.FEET;
                        break;
                    case LEGS:
                        slotGroup = org.bukkit.inventory.EquipmentSlotGroup.LEGS;
                        break;
                    case CHEST:
                        slotGroup = org.bukkit.inventory.EquipmentSlotGroup.CHEST;
                        break;
                    case HEAD:
                        slotGroup = org.bukkit.inventory.EquipmentSlotGroup.HEAD;
                        break;
                    default:
                        slotGroup = org.bukkit.inventory.EquipmentSlotGroup.ANY;
                }
            }

            AttributeModifier modifier;
            if (slotGroup != null) {
                modifier = new AttributeModifier(modifierKey, val, op, slotGroup);
            } else {
                modifier = new AttributeModifier(modifierKey, val, op);
            }

            meta.addAttributeModifier(attribute, modifier);
        }
    }

    private void registerEmbeddedRecipes(List<MythicItemDef> items) {
        if (items == null)
            return;

        for (MythicItemDef def : items) {
            try {
                if (def.id == null)
                    continue;

                // Register Machine Recipes
                if (def.furnace_recipe != null) {
                    processFurnaceRecipe(def.furnace_recipe);
                }

                if (def.compression_recipe != null) {
                    processCompressionRecipe(def.compression_recipe);
                }

                // Register Crafting Recipe (Embedded)
                if (def.recipe != null) {
                    if (def.recipe.id == null) {
                        def.recipe.id = def.id;
                    }
                    if ("crafting_shaped".equals(def.recipe.type)) {
                        registerShapedRecipe(def.recipe);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to register recipes for item: " + def.id, e);
            }
        }
    }

    private void processFurnaceRecipe(MythicFurnaceRecipeDef recipe) {
        if (recipe.input == null || recipe.result == null)
            return;

        Material inputMat = Material.matchMaterial(recipe.input);
        if (inputMat == null) {
            // Maybe custom item? Currently standard furnace only supports Material input
            plugin.getLogger().warning("Invalid input material for furnace recipe: " + recipe.input);
            return;
        }

        ItemStack resultStack = registry.getItem(recipe.result);
        if (resultStack == null) {
            // Try material
            Material resMat = Material.matchMaterial(recipe.result);
            if (resMat != null)
                resultStack = new ItemStack(resMat);
        }

        if (resultStack != null) {
            recipeService.registerCustomFurnaceRecipe(inputMat, resultStack);
        }
    }

    private void processCompressionRecipe(MythicCompressionRecipeDef recipe) {
        if (recipe.input == null || recipe.result == null)
            return;

        // Logic: key in map is usually ID + "_9", or just ID?
        // In MachineProcessor we check: itemId + "_9".
        // So if JSON says input="pebble_stone", we register "pebble_stone_9".

        ItemStack resultStack = registry.getItem(recipe.result);
        if (resultStack == null) {
            Material resMat = Material.matchMaterial(recipe.result);
            if (resMat != null)
                resultStack = new ItemStack(resMat);
        }

        if (resultStack != null) {
            String key = recipe.input + "_9";
            recipeService.registerCustomCompressionRecipe(key, resultStack);
        }
    }

    private void registerGlobalFurnaceRecipes(List<MythicFurnaceRecipeDef> recipes) {
        if (recipes == null)
            return;

        for (MythicFurnaceRecipeDef recipe : recipes) {
            processFurnaceRecipe(recipe);
        }
    }

    private void registerRecipes(List<MythicRecipeDef> recipes) {
        if (recipes == null)
            return;

        for (MythicRecipeDef recipe : recipes) {
            if (recipe.id == null || recipe.result == null)
                continue;

            // Initial basic support for shaped recipes
            if ("crafting_shaped".equals(recipe.type)) {
                registerShapedRecipe(recipe);
            }
        }
    }

    private void registerShapedRecipe(MythicRecipeDef recipe) {
        try {
            NamespacedKey key = new NamespacedKey(plugin, recipe.id);
            ItemStack resultItem = getItemFromRef(recipe.result);
            if (resultItem == null) {
                plugin.getLogger().warning("Invalid result for recipe " + recipe.id);
                return;
            }

            ShapedRecipe shaped = new ShapedRecipe(key, resultItem);

            // Pattern
            String[] shape = recipe.pattern.toArray(new String[0]);
            shaped.shape(shape);

            // Keys
            for (Map.Entry<String, String> entry : recipe.key.entrySet()) {
                char charKey = entry.getKey().charAt(0);
                String ingredientRef = entry.getValue();

                // Determine ingredient
                if (ingredientRef.startsWith("minecraft:")) {
                    Material mat = Material.matchMaterial(ingredientRef);
                    if (mat != null)
                        shaped.setIngredient(charKey, mat);
                } else {
                    // It's a custom item ID?
                    // Bukkit recipes usually require ExactChoice for custom items,
                    // but ShapedRecipe setIngredient takes Material or RecipeChoice.
                    // For simplicity in this iteration, we might map to Material if possible
                    // OR usage RecipeChoice.ExactChoice if we can get the item stack.
                    ItemStack customIngredient = registry.getItem(ingredientRef);
                    if (customIngredient != null) {
                        shaped.setIngredient(charKey,
                                new org.bukkit.inventory.RecipeChoice.ExactChoice(customIngredient));
                    } else {
                        // Fallback: maybe it's a material name without prefix?
                        Material mat = Material.matchMaterial(ingredientRef);
                        if (mat != null)
                            shaped.setIngredient(charKey, mat);
                        else
                            plugin.getLogger()
                                    .warning("Unknown ingredient " + ingredientRef + " in recipe " + recipe.id);
                    }
                }
            }

            plugin.getServer().addRecipe(shaped);
            recipeKeys.add(key);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to register recipe: " + recipe.id, e);
        }
    }

    private ItemStack getItemFromRef(MythicResultDef result) {
        ItemStack stack = registry.getItem(result.item);
        if (stack == null) {
            // Maybe vanilla?
            Material mat = Material.matchMaterial(result.item);
            if (mat != null)
                stack = new ItemStack(mat);
        }
        if (stack != null) {
            stack.setAmount(result.count > 0 ? result.count : 1);
        }
        return stack;
    }

    private String getRarityColor(String rarity) {
        if (rarity == null)
            return "§f";
        switch (rarity.toLowerCase()) {
            case "common":
                return "§f";
            case "uncommon":
                return "§a";
            case "rare":
                return "§9";
            case "epic":
                return "§5";
            case "legendary":
                return "§6";
            case "divine":
                return "§d§l";
            case "mythic":
                return "§c§l";
            default:
                return "§f";
        }
    }

    public List<String> getItemIdsByRarity(String rarity) {
        List<String> validIds = new ArrayList<>();
        if (config == null || config.items == null)
            return validIds;

        for (MythicItemDef def : config.items) {
            if (def.rarity != null && def.rarity.equalsIgnoreCase(rarity)) {
                validIds.add(def.id);
            }
        }
        return validIds;
    }

    // ================= DTOs =================

    public GachaGlobalConfig getGachaConfig() {
        if (config == null)
            return null;
        return config.gacha_config;
    }

    private static class MythicConfig {
        List<MythicItemDef> items;
        List<MythicRecipeDef> crafting_recipes;
        List<MythicFurnaceRecipeDef> furnace_recipes; // Added field
        GachaGlobalConfig gacha_config;
    }

    public static class GachaGlobalConfig {
        public Map<String, GachaCrateDef> crate_types;
        public List<String> legendary_pool;
        public int pity_threshold;
        public String pity_behavior;
    }

    public static class GachaCrateDef {
        public String item;
        public int priceBTC;
        public boolean admin_stock_control;
        public GachaRates default_rates;
    }

    public static class GachaRates {
        public int common;
        public int uncommon;
        public int rare;
        public int epic;
        public int legendary;
    }

    private static class MythicItemDef {
        String id;
        String name;
        String rarity;
        String category;
        String description;
        String usage;

        MythicEffects effects;
        String baseItem;
        Map<String, Object> nbt;
        MythicRecipeDef recipe; // Added recipe field
        MythicFurnaceRecipeDef furnace_recipe;
        MythicCompressionRecipeDef compression_recipe;
    }

    private static class MythicEffects {
        String positive;
        String negative;
    }

    private static class MythicRecipeDef {
        String id;
        String type;
        List<String> pattern;
        Map<String, String> key;
        MythicResultDef result;

    }

    private static class MythicResultDef {
        String item;
        int count;
    }

    private static class MythicFurnaceRecipeDef {

        String input;
        String result;

    }

    private static class MythicCompressionRecipeDef {
        String input;
        String result;
    }
}
