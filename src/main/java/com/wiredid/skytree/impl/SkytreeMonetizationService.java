package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MonetizationService;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkytreeMonetizationService implements MonetizationService {

    private final SkytreePlugin plugin;
    private double globalMultiplier = 1.0;
    private long globalMultiplierExpiry = 0;
    private final Map<UUID, Booster> playerBoosters = new HashMap<>();

    public SkytreeMonetizationService(SkytreePlugin plugin) {
        this.plugin = plugin;
        startExpiryTask();
    }

    private void startExpiryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (globalMultiplierExpiry > 0 && now >= globalMultiplierExpiry) {
                    globalMultiplier = 1.0;
                    globalMultiplierExpiry = 0;
                    plugin.getLogger().info("Global money booster has expired.");
                }
                playerBoosters.entrySet()
                        .removeIf(entry -> entry.getValue().expiry > 0 && now >= entry.getValue().expiry);
            }
        }.runTaskTimer(plugin, 20L * 30, 20L * 30); // Every 30 seconds
    }

    @Override
    public double getGlobalMoneyMultiplier() {
        return (globalMultiplierExpiry > 0 && System.currentTimeMillis() < globalMultiplierExpiry) ? globalMultiplier
                : 1.0;
    }

    @Override
    public void setGlobalMoneyMultiplier(double multiplier, long durationMinutes) {
        this.globalMultiplier = multiplier;
        this.globalMultiplierExpiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000);
    }

    @Override
    public double getPlayerMultiplier(UUID playerId) {
        Booster b = playerBoosters.get(playerId);
        return (b != null && (b.expiry == 0 || System.currentTimeMillis() < b.expiry)) ? b.multiplier : 1.0;
    }

    @Override
    public void addPlayerBooster(UUID playerId, double multiplier, long durationMinutes) {
        playerBoosters.put(playerId, new Booster(multiplier,
                durationMinutes > 0 ? System.currentTimeMillis() + (durationMinutes * 60 * 1000) : 0));
    }

    @Override
    public boolean hasActiveBooster(UUID playerId) {
        return getPlayerMultiplier(playerId) > 1.0;
    }

    private static class Booster {
        double multiplier;
        long expiry;

        Booster(double multiplier, long expiry) {
            this.multiplier = multiplier;
            this.expiry = expiry;
        }
    }
}
