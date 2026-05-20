package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AdminService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AdminServiceImpl implements AdminService {

    private final SkytreePlugin plugin;
    private final File logFile;
    private final Set<UUID> staffModePlayers = ConcurrentHashMap.newKeySet();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final LinkedList<String> memoryLogs = new LinkedList<>();
    private final int MAX_MEMORY_LOGS = 100;

    public AdminServiceImpl(SkytreePlugin plugin) {
        this.plugin = plugin;
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists())
            logsDir.mkdirs();
        this.logFile = new File(logsDir, "admin_actions.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create admin logs file: " + e.getMessage());
            }
        }
    }

    @Override
    public void logAction(UUID admin, String category, String action) {
        String adminName = admin == null ? "SYSTEM" : Bukkit.getOfflinePlayer(admin).getName();
        String entry = String.format("[%s] [%s] %s: %s", dateFormat.format(new Date()), category, adminName, action);

        // Write to file
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileWriter fw = new FileWriter(logFile, true);
                    PrintWriter pw = new PrintWriter(fw)) {
                pw.println(entry);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write to admin log: " + e.getMessage());
            }
        });

        // Store in memory for GUI
        synchronized (memoryLogs) {
            memoryLogs.addFirst(entry);
            if (memoryLogs.size() > MAX_MEMORY_LOGS) {
                memoryLogs.removeLast();
            }
        }
    }

    @Override
    public List<String> getRecentLogs(int limit) {
        synchronized (memoryLogs) {
            return new ArrayList<>(memoryLogs.subList(0, Math.min(limit, memoryLogs.size())));
        }
    }

    @Override
    public List<String> getPlayerLogs(UUID target, int limit) {
        String targetName = Bukkit.getOfflinePlayer(target).getName();
        List<String> results = new ArrayList<>();
        synchronized (memoryLogs) {
            for (String log : memoryLogs) {
                if (log.contains(targetName)) {
                    results.add(log);
                    if (results.size() >= limit)
                        break;
                }
            }
        }
        return results;
    }

    @Override
    public void toggleStaffMode(Player player) {
        if (staffModePlayers.contains(player.getUniqueId())) {
            staffModePlayers.remove(player.getUniqueId());
            player.removeMetadata("staff_mode", plugin);
            // Disable vanish
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(plugin, player);
            }
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage("§e§l[Admin] §7Staff Mode: §cDISABLED");
            logAction(player.getUniqueId(), "ADMIN", "Disabled staff mode");
        } else {
            staffModePlayers.add(player.getUniqueId());
            player.setMetadata("staff_mode", new FixedMetadataValue(plugin, true));
            // Enable vanish
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("skytree.admin")) {
                    p.hidePlayer(plugin, player);
                }
            }
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setMetadata("skytree_fly", new FixedMetadataValue(plugin, true));
            player.sendMessage("§e§l[Admin] §7Staff Mode: §aENABLED §8(Vanished & Flight active)");
            logAction(player.getUniqueId(), "ADMIN", "Enabled staff mode");
        }
    }

    @Override
    public boolean isInStaffMode(Player player) {
        return staffModePlayers.contains(player.getUniqueId());
    }

    @Override
    public void openInventory(Player admin, Player target) {
        admin.openInventory(target.getInventory());
        admin.sendMessage("§e§l[Admin] §7Opened inventory of §f" + target.getName());
        logAction(admin.getUniqueId(), "ADMIN", "Opened inventory of " + target.getName());
    }

    @Override
    public void openEnderChest(Player admin, Player target) {
        admin.openInventory(target.getEnderChest());
        admin.sendMessage("§e§l[Admin] §7Opened enderchest of §f" + target.getName());
        logAction(admin.getUniqueId(), "ADMIN", "Opened enderchest of " + target.getName());
    }
}
