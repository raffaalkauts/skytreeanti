package com.wiredid.skytree.fishing;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.fishing.FishingModels.Mutation;
import com.wiredid.skytree.fishing.FishingModels.Rarity;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for applying visual effects to custom fish items
 * Handles tier-based styling, mutation effects, and dynamic lore
 */
public class CustomFishService {

    private final SkytreePlugin plugin;

    public CustomFishService(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Apply complete visual styling to a fish item
     *
     * @param item     The item to apply visuals to
     * @param fishId   The fish identifier
     * @param mutation The mutation type (can be null for NORMAL)
     */
    public void applyVisuals(ItemStack item, String fishId, Mutation mutation) {
        if (item == null || fishId == null) {
            return;
        }

        // Default mutation to NORMAL if null
        if (mutation == null) {
            mutation = Mutation.NORMAL;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // Determine rarity from fish database
        Rarity rarity = getRarityForFish(fishId);

        // 1. Apply tier-based name formatting
        Component displayName = createFishDisplayName(fishId, rarity, mutation);
        meta.displayName(displayName);

        // 2. Build dynamic lore with mutation indicators
        List<Component> lore = buildFishLore(fishId, rarity, mutation);
        meta.lore(lore);

        // 3. Apply glow effect for rare fish (Rare and above)
        if (shouldApplyGlow(rarity, mutation)) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // 4. Set custom model data for visual variety
        int customModelData = getModelDataForTier(rarity, mutation);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        item.setItemMeta(meta);
    }

    /**
     * Create formatted display name with rarity color and mutation prefix
     */
    private Component createFishDisplayName(String fishId, Rarity rarity, Mutation mutation) {
        // Format fish name (capitalize and replace underscores)
        String formattedName = formatFishName(fishId);

        // Build name with mutation prefix if not normal
        String nameText;
        String color = rarity.getColor();

        if (mutation != Mutation.NORMAL) {
            nameText = color + mutation.getDisplayName() + " " + formattedName;
        } else {
            nameText = color + formattedName;
        }

        // Add sparkles for Legend/Limited
        if (rarity == Rarity.LIMITED) {
            // Use MiniMessage for Limited to keep rainbow consistent
            nameText = "<rainbow><b>✦ " + formattedName + " ✦";
        } else if (rarity == Rarity.LEGEND) {
            nameText = "§6§l✦ " + formattedName + " ✦";
        }

        return ComponentUtil.smartParse(nameText).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Build comprehensive lore with stats and mutation info
     */
    private List<Component> buildFishLore(String fishId, Rarity rarity, Mutation mutation) {
        List<Component> lore = new ArrayList<>();

        // Rarity line
        lore.add(ComponentUtil.parse("§7Rarity: " + rarity.getColor() + rarity.getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        // Mutation line (if not normal)
        if (mutation != Mutation.NORMAL) {
            lore.add(ComponentUtil.parse("§7Mutation: " + getMutationColor(mutation) + mutation.getDisplayName()
                    + " §7(§e+" + String.format("%.0f", (mutation.getPriceMultiplier() - 1) * 100) + "%§7)")
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Empty line
        lore.add(Component.empty());

        // Mutation-specific lore effects
        switch (mutation) {
            case BIG:
                lore.add(ComponentUtil.parse("§7This fish is §eunusually large§7!")
                        .decoration(TextDecoration.ITALIC, true));
                break;
            case SHINY:
                lore.add(ComponentUtil.parse("§7Glistening scales §bshimmer§7 in the light")
                        .decoration(TextDecoration.ITALIC, true));
                break;
            case GOLD:
                lore.add(ComponentUtil.parse("§6§l✦ §6Radiates golden energy §6§l✦")
                        .decoration(TextDecoration.ITALIC, false));
                break;
            case ANCIENT:
                lore.add(ComponentUtil.parse("§5§kaa§r §d§lAncient Power§r §5§kaa")
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(ComponentUtil.parse("§7A relic from ages past...")
                        .decoration(TextDecoration.ITALIC, true));
                break;
            case NORMAL:
                break;
        }

        // Rarity-specific flavor text
        if (rarity == Rarity.LIMITED) {
            lore.add(Component.empty());
            lore.add(ComponentUtil.smartParse("<rainbow><b>✦ LIMITED CATCH ✦")
                    .decoration(TextDecoration.ITALIC, false));
        } else if (rarity == Rarity.LEGEND) {
            lore.add(Component.empty());
            lore.add(ComponentUtil.parse("§6§l✦ LEGENDARY CATCH ✦")
                    .decoration(TextDecoration.ITALIC, false));
        } else if (rarity == Rarity.MYTHIC) {
            lore.add(Component.empty());
            lore.add(ComponentUtil.parse("§5§lMythic Discovery")
                    .decoration(TextDecoration.ITALIC, false));
        }

        return lore;
    }

    /**
     * Determine if glow effect should be applied
     */
    private boolean shouldApplyGlow(Rarity rarity, Mutation mutation) {
        // Rare and above get glow
        if (rarity == Rarity.RARE || rarity == Rarity.MYTHIC ||
                rarity == Rarity.LEGEND || rarity == Rarity.LIMITED) {
            return true;
        }

        // Special mutations always glow
        return mutation == Mutation.GOLD || mutation == Mutation.ANCIENT;
    }

    /**
     * Get custom model data based on tier and mutation
     */
    private int getModelDataForTier(Rarity rarity, Mutation mutation) {
        // Base model data on rarity
        int baseModel = switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case MYTHIC -> 3;
            case LEGEND -> 4;
            case LIMITED -> 5;
        };

        // Add mutation offset (0-5)
        int mutationOffset = switch (mutation) {
            case NORMAL -> 0;
            case BIG -> 10;
            case SHINY -> 20;
            case GOLD -> 30;
            case ANCIENT -> 40;
        };

        return baseModel + mutationOffset;
    }

    /**
     * Get color code for mutation display
     */
    private String getMutationColor(Mutation mutation) {
        return switch (mutation) {
            case BIG -> "§a"; // Green
            case SHINY -> "§b"; // Aqua
            case GOLD -> "§6"; // Gold
            case ANCIENT -> "§5"; // Purple
            default -> "§7"; // Gray
        };
    }

    /**
     * Format fish name for display (Snake_case → Title Case)
     */
    private String formatFishName(String fishId) {
        String[] words = fishId.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                formatted.append(word.substring(1).toLowerCase());
                formatted.append(" ");
            }
        }

        return formatted.toString().trim();
    }

    /**
     * Get rarity for a specific fish ID from the fishing service
     * Falls back to COMMON if not found
     */
    private Rarity getRarityForFish(String fishId) {
        // Try to get from FishingService database
        FishingService fishingService = plugin.getFishingService();
        if (fishingService != null) {
            try {
                FishingModels.Fish fish = fishingService.getFishById(fishId);
                if (fish != null) {
                    return fish.rarity();
                }
            } catch (Exception e) {
                // Fish not found, use default
            }
        }

        // Fallback rarity based on fish name
        return getRarityFromName(fishId);
    }

    /**
     * Fallback rarity determination based on fish name patterns
     */
    private Rarity getRarityFromName(String fishId) {
        String lower = fishId.toLowerCase();

        // Legend indicators
        if (lower.contains("diamond") || lower.contains("dolphin") ||
                lower.contains("whale") || lower.contains("megalodon")) {
            return Rarity.LEGEND;
        }

        // Mythic indicators
        if (lower.contains("eel") || lower.contains("giant") ||
                lower.contains("kraken") || lower.contains("hammerhead")) {
            return Rarity.MYTHIC;
        }

        // Rare indicators
        if (lower.contains("jewel") || lower.contains("pearl") ||
                lower.contains("starfish") || lower.contains("octopus")) {
            return Rarity.RARE;
        }

        // Uncommon indicators
        if (lower.contains("tuna") || lower.contains("salmon") ||
                lower.contains("swordfish") || lower.contains("puffer")) {
            return Rarity.UNCOMMON;
        }

        // Default to common
        return Rarity.COMMON;
    }
}
