package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.GachaService;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.impl.MythicItemManager.GachaCrateDef;
import com.wiredid.skytree.impl.MythicItemManager.GachaGlobalConfig;
import com.wiredid.skytree.impl.MythicItemManager.GachaRates;
import com.wiredid.skytree.model.PlayerData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkytreeGachaService implements GachaService {

    private final SkytreePlugin plugin;
    private final PersistenceService persistenceService;
    private final MythicItemManager mythicItemManager;
    private final SkytreeItemRegistry itemRegistry;
    private final EconomyService economyService;
    private final Random random = new Random();

    public SkytreeGachaService(SkytreePlugin plugin, PersistenceService persistenceService,
            MythicItemManager mythicItemManager, SkytreeItemRegistry itemRegistry,
            EconomyService economyService) {
        this.plugin = plugin;
        this.persistenceService = persistenceService;
        this.mythicItemManager = mythicItemManager;
        this.itemRegistry = itemRegistry;
        this.economyService = economyService;
    }

    @Override
    public void reload() {
        // Gacha relies on MythicItemManager config
        mythicItemManager.reload();
    }

    @Override
    public ItemStack spinGacha(Player player, String crateType) {
        GachaGlobalConfig config = mythicItemManager.getGachaConfig();
        if (config == null || config.crate_types == null)
            return null;

        GachaCrateDef crate = config.crate_types.get(crateType);
        if (crate == null) {
            player.sendMessage(Component.text("Invalid crate type.", NamedTextColor.RED));
            return null;
        }

        // Check cost
        double cashUSDT = plugin.getEconomyService().getBalance(player.getUniqueId());
        if (cashUSDT < crate.priceBTC) {
            player.sendMessage(Component.text("Insufficient funds! Need \u20AE " + crate.priceBTC, NamedTextColor.RED));
            return null;
        }
        // Pity Logic
        PlayerData data = persistenceService.loadPlayerData(player.getUniqueId());
        int currentPity = (data != null) ? data.getGachaPity() : 0;
        boolean forceLegendary = false;

        if (currentPity >= config.pity_threshold - 1) { // 99 -> 100th is guaranteed
            forceLegendary = true;
        }

        String rarity = forceLegendary ? "legendary" : rollRarity(crate.default_rates);

        // Deduct cost
        economyService.removeBalance(player.getUniqueId(), crate.priceBTC);

        if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().addToReserve(crate.priceBTC);
        }

        // Update Pity
        if (data != null) {
            if ("legendary".equalsIgnoreCase(rarity)) {
                data.setGachaPity(0);
                if (forceLegendary) {
                    player.sendMessage(
                            Component.text("Pity Guarantee Reached! Legendary obtain!", NamedTextColor.GOLD));
                }
            } else {
                data.setGachaPity(currentPity + 1);
            }
            persistenceService.savePlayerData(data);
        }

        // Get Item
        ItemStack reward = getRandomItemOfRarity(rarity, config);

        // Start Animation instead of giving directly
        com.wiredid.skytree.task.GachaAnimationTask task = new com.wiredid.skytree.task.GachaAnimationTask(plugin,
                player, crate, this, crateType);
        task.prepare(reward, config);
        task.runTaskTimer(plugin, 1L, 1L);

        // Admin Logging
        plugin.getAdminService().logAction(player.getUniqueId(), "GACHA",
                String.format("Pulled %s crate (Rarity: %s, Price: %.2f USDT)", crateType, rarity.toUpperCase(),
                        crate.priceBTC));

        return null; // Item given by task
    }

    @Override
    public ItemStack spinGachaFromItem(Player player, String crateType, ItemStack paidItem) {
        GachaGlobalConfig config = mythicItemManager.getGachaConfig();
        if (config == null || config.crate_types == null)
            return null;

        GachaCrateDef crate = config.crate_types.get(crateType);
        if (crate == null) {
            player.sendMessage(Component.text("Invalid crate type.", NamedTextColor.RED));
            return null;
        }

        // Pity Logic
        PlayerData data = persistenceService.loadPlayerData(player.getUniqueId());
        int currentPity = data.getGachaPity();
        boolean forceLegendary = false;

        if (currentPity >= config.pity_threshold - 1) {
            forceLegendary = true;
        }

        String rarity = forceLegendary ? "legendary" : rollRarity(crate.default_rates);

        // Update Pity
        if ("legendary".equalsIgnoreCase(rarity)) {
            data.setGachaPity(0);
            if (forceLegendary) {
                player.sendMessage(Component.text("Pity Guarantee Reached! Legendary obtain!", NamedTextColor.GOLD));
            }
        } else {
            data.setGachaPity(currentPity + 1);
        }
        persistenceService.savePlayerData(data);

        // Consume Item (1 amount)
        paidItem.setAmount(paidItem.getAmount() - 1);

        // Get Item
        ItemStack reward = getRandomItemOfRarity(rarity, config);

        // Animation
        com.wiredid.skytree.task.GachaAnimationTask task = new com.wiredid.skytree.task.GachaAnimationTask(plugin,
                player, crate, this, crateType);
        task.prepare(reward, config);
        task.runTaskTimer(plugin, 1L, 1L);

        return null;
    }

    public void playWinEffects(Player player, ItemStack reward) {
        // Effects
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

        // Standard Win Presentation
        String name = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                ? com.wiredid.skytree.util.ComponentUtil.toLegacy(reward.getItemMeta().displayName())
                : reward.getType().name();

        player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("§a§lCONGRATULATIONS!"),
                Component.text("§7You pulled: §f" + name),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(250),
                        java.time.Duration.ofMillis(1500), java.time.Duration.ofMillis(500))));

        // Broadcast for Rare/Legendary
        if (reward.hasItemMeta() && reward.getItemMeta().hasLore()) {
            List<String> lore = reward.getItemMeta().lore().stream()
                    .map(com.wiredid.skytree.util.ComponentUtil::toLegacy).toList();
            for (String line : lore) {
                if (line.contains("LEGENDARY") || line.contains("MYTHIC") || line.contains("DIVINE")
                        || line.contains("RARE")) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    plugin.getServer().broadcast(Component.text("§6§l[Gacha] §f" + player.getName()
                            + " §7just pulled a §6§l" + line.toUpperCase().trim() + " §7item: §e" + name + "§7!"));
                    break;
                }
            }
        }
    }

    public ItemStack getRandomVisualItem(GachaGlobalConfig config) {
        // Return a random item for the rolling animation (visual only)
        // Weighted random for better visual feel?
        // Reuse rollRarity but with safe defaults
        GachaCrateDef dummyCrate = config.crate_types.values().iterator().next(); // Grab first as default
        String rarity = rollRarity(dummyCrate.default_rates);
        return getRandomItemOfRarity(rarity, config);
    }

    @Override
    public int getPity(Player player) {
        return persistenceService.loadPlayerData(player.getUniqueId()).getGachaPity();
    }

    @Override
    public String getCrateType(ItemStack item) {
        return null;
    }

    private String rollRarity(GachaRates rates) {
        int total = rates.common + rates.uncommon + rates.rare + rates.epic + rates.legendary;
        int roll = random.nextInt(total);

        if (roll < rates.legendary)
            return "legendary";
        roll -= rates.legendary;
        if (roll < rates.epic)
            return "epic";
        roll -= rates.epic;
        if (roll < rates.rare)
            return "rare";
        roll -= rates.rare;
        if (roll < rates.uncommon)
            return "uncommon";

        return "common";
    }

    private ItemStack getRandomItemOfRarity(String rarity, GachaGlobalConfig config) {
        List<String> pool = new ArrayList<>();

        if ("legendary".equalsIgnoreCase(rarity)) {
            if (config.legendary_pool != null)
                pool.addAll(config.legendary_pool);
        } else {
            // Fetch valid items from MythicItemManager based on rarity
            pool = mythicItemManager.getItemIdsByRarity(rarity);

            // Fallback if pool is empty for that rarity
            if (pool.isEmpty()) {
                if ("epic".equalsIgnoreCase(rarity)) {
                    pool.add("DIAMOND_BLOCK"); // Fallback
                } else {
                    pool.add("COBBLESTONE"); // Fallback
                }
            }
        }

        if (pool.isEmpty())
            return new ItemStack(Material.ROTTEN_FLESH);

        String id = pool.get(random.nextInt(pool.size()));

        // Special handling for spawners
        if (id.startsWith("spawner_")) {
            try {
                String entityName = id.replace("spawner_", "").toUpperCase();
                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(entityName);

                ItemStack item = new ItemStack(Material.SPAWNER);
                org.bukkit.inventory.meta.BlockStateMeta meta = (org.bukkit.inventory.meta.BlockStateMeta) item
                        .getItemMeta();
                org.bukkit.block.CreatureSpawner spawner = (org.bukkit.block.CreatureSpawner) meta.getBlockState();
                spawner.setSpawnedType(type);
                meta.setBlockState(spawner);

                String name = type.name().charAt(0) + type.name().substring(1).toLowerCase().replace("_", " ");
                meta.displayName(com.wiredid.skytree.util.ComponentUtil.parse("§e" + name + " Spawner"));
                item.setItemMeta(meta);
                return item;
            } catch (Exception ignored) {
            }
        }

        // Try to get from ItemRegistry
        ItemStack stack = itemRegistry.getItem(id);
        if (stack == null) {
            // Try as Material
            try {
                return new ItemStack(Material.valueOf(id));
            } catch (Exception e) {
                return new ItemStack(Material.STONE);
            }
        }
        return stack.clone();
    }
}
