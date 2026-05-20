package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.api.ThirstService;
import com.wiredid.skytree.model.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkytreeThirstService implements ThirstService {

    private final SkytreePlugin plugin;
    private final PersistenceService persistenceService;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Double> thirstCache = new HashMap<>();
    private BukkitTask task;

    public SkytreeThirstService(SkytreePlugin plugin, PersistenceService persistenceService) {
        this.plugin = plugin;
        this.persistenceService = persistenceService;
    }

    @Override
    public void startTask() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateThirstLoop(player);
            }
        }, 20L, 20L); // Run every second
    }

    @Override
    public void stopTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        for (java.util.Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
        thirstCache.clear();
    }

    @Override
    public void registerPlayer(Player player) {
        // Prevent duplicate bars
        if (bossBars.containsKey(player.getUniqueId())) {
            player.hideBossBar(bossBars.get(player.getUniqueId()));
            bossBars.remove(player.getUniqueId());
        }

        PlayerData data = persistenceService.loadPlayerData(player.getUniqueId());
        double thirst = data.getThirst();
        thirstCache.put(player.getUniqueId(), thirst);

        BossBar bar = BossBar.bossBar(
                Component.text("Thirst", NamedTextColor.BLUE),
                (float) (thirst / 100.0),
                BossBar.Color.BLUE,
                BossBar.Overlay.NOTCHED_10);
        player.showBossBar(bar);
        bossBars.put(player.getUniqueId(), bar);
    }

    // ...

    @Override
    public void unregisterPlayer(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }

        Double thirst = thirstCache.remove(player.getUniqueId());
        if (thirst != null) {
            PlayerData data = persistenceService.loadPlayerData(player.getUniqueId());
            data.setThirst(thirst);
            persistenceService.savePlayerData(data);
        }
    }

    @Override
    public double getThirst(Player player) {
        return thirstCache.getOrDefault(player.getUniqueId(), 100.0);
    }

    @Override
    public void setThirst(Player player, double amount) {
        amount = Math.max(0, Math.min(100, amount));
        thirstCache.put(player.getUniqueId(), amount);
        updateBar(player, amount);
    }

    @Override
    public void addThirst(Player player, double amount) {
        setThirst(player, getThirst(player) + amount);
    }

    @Override
    public void handleConsumption(Player player, ItemStack item) {
        if (item == null)
            return;

        String itemId = plugin.getItemRegistry().getItemId(item);

        // Custom water items
        if ("purified_water".equals(itemId)) {
            addThirst(player, 40.0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0));
            player.sendMessage(Component.text("You feel hydrated!", NamedTextColor.AQUA));
        } else if ("blessed_water".equals(itemId)) {
            addThirst(player, 50.0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 300, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
            player.sendMessage(Component.text("Divine waters restore your vitality!", NamedTextColor.GOLD));
        } else if (itemId != null && itemId.contains("juice")) {
            addThirst(player, 60.0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.sendMessage(Component.text("Delicious and refreshing!", NamedTextColor.GREEN));
        } else {
            // Check for vanilla water bottle
            if (item.getType() == Material.POTION) {
                org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
                if (meta != null) {
                    // Check if it is water (no effects) or explicitly WATER type
                    boolean isWater = meta.getBasePotionType() == org.bukkit.potion.PotionType.WATER;

                    if (isWater) {
                        addThirst(player, 15.0); // Simple water
                        // 30% chance of "dirty water" effect
                        if (Math.random() < 0.3) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                            player.sendMessage(Component.text("The water tastes a bit dirty...", NamedTextColor.GREEN));
                        } else {
                            player.sendMessage(Component.text("Refreshing...", NamedTextColor.BLUE));
                        }
                    }
                }
            } else if (item.getType() == Material.MELON_SLICE) {
                addThirst(player, 5.0);
            }
        }
    }

    private void updateThirstLoop(Player player) {
        double current = getThirst(player);
        // Decay calculation: 100 thirst / (8 hours * 3600 seconds) = ~0.003472/sec
        double decay = 0.0035; // Adjusted decay per second for ~8 hours full drain

        if (player.isSprinting())
            decay *= 2;
        if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER)
            decay *= 2;

        double next = Math.max(0, current - decay);
        thirstCache.put(player.getUniqueId(), next);
        updateBar(player, next);

        if (next <= 0) {
            player.damage(1.0);
            if (plugin.getPersistenceService().loadPlayerData(player.getUniqueId()).getSettings()
                    .getOrDefault("actionbar", true)) {
                player.sendActionBar(Component.text("You are dehydrated!", NamedTextColor.RED));
            }
        }
    }

    private void updateBar(Player player, double thirst) {
        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar != null) {
            bar.progress((float) (thirst / 100.0));
            bar.name(Component.text("Thirst: " + String.format("%.1f", thirst) + "%", NamedTextColor.BLUE));
        }
    }
}
