package com.wiredid.skytree.fishing;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.fishing.FishingModels.RodEnchant;
import com.wiredid.skytree.fishing.FishingModels.RodTier;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class EnchantService {

    private final EconomyService economy;
    private final Random random = new Random();

    public EnchantService(SkytreePlugin plugin, EconomyService economy, FishingService fishingService) {
        this.economy = economy;
    }

    public record AppliedEnchant(RodEnchant enchant, int level) {
    }

    public double getEnchantCost(ItemStack rod) {
        return 200000.0; // 200k per enchant
    }

    public Set<AppliedEnchant> getAppliedEnchants(ItemStack rod) {
        String enchantsStr = NbtUtils.getString(rod, NbtUtils.KEY_ROD_ENCHANTS);
        if (enchantsStr == null || enchantsStr.isEmpty()) {
            return new HashSet<>();
        }
        Set<AppliedEnchant> applied = new HashSet<>();
        for (String pair : enchantsStr.split(",")) {
            if (pair.contains(":")) {
                String[] parts = pair.split(":");
                try {
                    applied.add(new AppliedEnchant(RodEnchant.valueOf(parts[0]), Integer.parseInt(parts[1])));
                } catch (Exception ignored) {
                }
            } else {
                // Legacy support
                try {
                    applied.add(new AppliedEnchant(RodEnchant.valueOf(pair), 1));
                } catch (Exception ignored) {
                }
            }
        }
        return applied;
    }

    public boolean canEnchant(ItemStack rod) {
        if (!NbtUtils.isCustomRod(rod))
            return false;
        RodTier tier = NbtUtils.getRodTier(rod);
        int current = getAppliedEnchants(rod).size();
        return current < tier.getSlots();
    }

    public void rollEnchant(Player player, ItemStack rod) {
        double cost = getEnchantCost(rod);

        if (economy.getBalance(player.getUniqueId()) < cost) {
            player.sendMessage("§cInsufficient USDT! Need: " + cost);
            return;
        }

        // 1. Roll from all enchants
        Set<AppliedEnchant> owned = getAppliedEnchants(rod);
        RodEnchant[] all = RodEnchant.values();
        RodEnchant won = all[random.nextInt(all.length)];

        // 2. Logic: If owned, upgrade. If not, add at lvl 1 IF slots available.
        int currentLevel = 0;
        AppliedEnchant existing = null;
        for (AppliedEnchant ae : owned) {
            if (ae.enchant() == won) {
                currentLevel = ae.level();
                existing = ae;
                break;
            }
        }

        if (existing != null) {
            if (currentLevel >= won.getMaxLevel()) {
                player.sendMessage("§cEnchant " + won.getDisplayName() + " is already at max level!");
                return;
            }
            // Upgrade
            owned.remove(existing);
            owned.add(new AppliedEnchant(won, currentLevel + 1));
            player.sendMessage("§aUpgraded " + won.getDisplayName() + " to level " + (currentLevel + 1) + "!");
        } else {
            RodTier tier = NbtUtils.getRodTier(rod);
            if (owned.size() >= tier.getSlots()) {
                player.sendMessage("§cNo more enchantment slots available (Max " + tier.getSlots() + ")!");
                return;
            }
            owned.add(new AppliedEnchant(won, 1));
            player.sendMessage("§aEnchant Success! Added: §e" + won.getDisplayName() + " I");
        }

        // 3. Deduct money
        if (!economy.removeBalance(player.getUniqueId(), cost)) {
            player.sendMessage("§cTransaction failed.");
            return;
        }

        // 4. Save
        String newData = owned.stream()
                .map(ae -> ae.enchant().name() + ":" + ae.level())
                .collect(Collectors.joining(","));
        NbtUtils.setString(rod, NbtUtils.KEY_ROD_ENCHANTS, newData);

        // 5. Update Lore
        updateRodLore(rod, owned);
    }

    private void updateRodLore(ItemStack rod, Set<AppliedEnchant> enchants) {
        ItemMeta meta = rod.getItemMeta();
        RodTier tier = NbtUtils.getRodTier(rod);

        List<net.kyori.adventure.text.Component> newLore = new ArrayList<>();
        newLore.add(net.kyori.adventure.text.Component.text("Tier: " + tier.getDisplayName())
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        newLore.add(net.kyori.adventure.text.Component.text("Slots: " + tier.getSlots())
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        newLore.add(net.kyori.adventure.text.Component.text(""));
        newLore.add(net.kyori.adventure.text.Component.text("Enchants:")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));

        if (enchants.isEmpty()) {
            newLore.add(net.kyori.adventure.text.Component.text(" (Empty)")
                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
        } else {
            for (AppliedEnchant ae : enchants) {
                String roman = toRoman(ae.level());
                newLore.add(net.kyori.adventure.text.Component.text(" - " + ae.enchant().getDisplayName() + " " + roman)
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                newLore.add(net.kyori.adventure.text.Component.text("   §7" + ae.enchant().getDescription())
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
            }
        }

        meta.lore(newLore);
        rod.setItemMeta(meta);
    }

    private String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
