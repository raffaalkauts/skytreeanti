package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class DailyRewardsSystem {

    private final SkytreePlugin plugin;
    private YamlConfiguration config;
    private final NamespacedKey LAST_CLAIM_KEY;
    private final NamespacedKey STREAK_KEY;

    public DailyRewardsSystem(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.LAST_CLAIM_KEY = new NamespacedKey(plugin, "daily_last_claim");
        this.STREAK_KEY = new NamespacedKey(plugin, "daily_streak");
        loadConfig();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "daily_rewards.yml");
        if (!file.exists()) {
            plugin.saveResource("daily_rewards.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void claimReward(Player player) {
        if (!canClaim(player)) {
            player.sendMessage("§cYou have already claimed your reward today!");
            return;
        }

        int streak = getStreak(player);
        long lastClaim = getLastClaim(player);
        LocalDate lastDate = lastClaim == 0 ? LocalDate.MIN : LocalDate.ofEpochDay(lastClaim);
        LocalDate today = LocalDate.now();

        // Check if streak is broken (missed more than 1 day)
        if (lastDate.plusDays(1).isBefore(today) && lastClaim != 0) {
            streak = 0;
            player.sendMessage("§cYou missed a day! Streak reset.");
        }

        streak++;
        int maxStreak = plugin.getConfig().getInt("daily_rewards.max_streak_days", 7);
        if (streak > maxStreak)
            streak = 1;

        // Give Reward
        ConfigurationSection reward = config.getConfigurationSection("rewards." + streak);
        if (reward != null) {
            List<String> commands = reward.getStringList("cmd");
            if (commands.isEmpty() && reward.contains("cmd")) {
                commands = List.of(reward.getString("cmd"));
            }

            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            }

            player.sendMessage("§a§lDaily Reward! §7You claimed Day " + streak + ": " + reward.getString("name"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        // Save Data
        setStreak(player, streak);
        setLastClaim(player, today.toEpochDay());
    }

    public boolean canClaim(Player player) {
        long lastClaim = getLastClaim(player);
        return lastClaim != LocalDate.now().toEpochDay();
    }

    public int getStreak(Player player) {
        return player.getPersistentDataContainer().getOrDefault(STREAK_KEY, PersistentDataType.INTEGER, 0);
    }

    public long getLastClaim(Player player) {
        return player.getPersistentDataContainer().getOrDefault(LAST_CLAIM_KEY, PersistentDataType.LONG, 0L);
    }

    private void setStreak(Player player, int streak) {
        player.getPersistentDataContainer().set(STREAK_KEY, PersistentDataType.INTEGER, streak);
    }

    private void setLastClaim(Player player, long epochDay) {
        player.getPersistentDataContainer().set(LAST_CLAIM_KEY, PersistentDataType.LONG, epochDay);
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}
