package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.model.PlayerData;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of USDT economy system
 */
public class SkytreeEconomyService implements EconomyService {

    private final SkytreePlugin plugin;
    private final PersistenceService persistence;
    private final Map<UUID, Double> balanceCache;
    private final double startingBalance;

    public SkytreeEconomyService(SkytreePlugin plugin, PersistenceService persistence) {
        this.plugin = plugin;
        this.persistence = persistence;
        this.balanceCache = new java.util.concurrent.ConcurrentHashMap<>();
        this.startingBalance = plugin.getConfig().getDouble("economy.starting_balance", 100.0);
    }

    @Override
    public double getBalance(UUID uuid) {
        if (!balanceCache.containsKey(uuid)) {
            double balance;
            if (persistence.hasAccount(uuid)) {
                balance = persistence.loadBalance(uuid);
            } else {
                balance = startingBalance;
                persistence.saveBalance(uuid, balance);
            }
            balanceCache.put(uuid, balance);
        }
        return balanceCache.get(uuid);
    }

    @Override
    public void setBalance(UUID uuid, double amount) {
        double oldForLog = getBalance(uuid);
        balanceCache.put(uuid, amount);
        persistence.saveBalance(uuid, amount);

        // Log manual set
        if (amount > oldForLog) {
            logTransaction(uuid, amount - oldForLog, "DEPOSIT", "Admin/System Set");
        } else if (amount < oldForLog) {
            logTransaction(uuid, oldForLog - amount, "WITHDRAW", "Admin/System Set");
        }
    }

    @Override
    public void addBalance(UUID uuid, double amount) {
        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().recalculateM2();
        }
        double current = getBalance(uuid);
        String reason = resolveReason();

        // Apply Rank Multiplier for non-admin/transfer deposits
        if (amount > 0 && !reason.contains("Admin") && !reason.contains("Transfer")) {
            double multiplier = plugin.getRankService().getMultiplier(uuid);
            if (multiplier > 1.0) {
                amount *= multiplier;
            }
        }

        // Do not call setBalance to avoid double log. Directly put and save.
        balanceCache.put(uuid, current + amount);
        persistence.saveBalance(uuid, current + amount);

        logTransaction(uuid, amount, "DEPOSIT", reason);

        // Quest Progress: MONEY_EARN
        if (amount > 0 && plugin.getQuestSystem() != null) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getQuestSystem().addProgress(player, com.wiredid.skytree.system.QuestSystem.QuestType.MONEY_EARN,
                        (int) amount);
            }
        }
    }

    @Override
    public boolean removeBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) {
            return false;
        }
        // Do not call setBalance to avoid double log.
        balanceCache.put(uuid, current - amount);
        persistence.saveBalance(uuid, current - amount);

        logTransaction(uuid, amount, "WITHDRAW", resolveReason());

        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().recalculateM2();
        }
        return true;
    }

    @Override
    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount <= 0)
            return false;

        double fromBal = getBalance(from);
        if (fromBal < amount)
            return false;

        balanceCache.put(from, fromBal - amount);
        persistence.saveBalance(from, fromBal - amount);
        logTransaction(from, amount, "WITHDRAW", "Transfer to " + to.toString().substring(0, 8));

        double toBal = getBalance(to);
        balanceCache.put(to, toBal + amount);
        persistence.saveBalance(to, toBal + amount);
        logTransaction(to, amount, "DEPOSIT", "Transfer from " + from.toString().substring(0, 8));

        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().recalculateM2();
        }
        return true;
    }

    @Override
    public String format(double amount) {
        return com.wiredid.skytree.util.NumberUtil.formatCurrency(amount);
    }

    @Override
    public void logTransaction(UUID uuid, double amount, String type, String reason) {
        PlayerData data = persistence.loadPlayerData(uuid);
        java.util.List<com.wiredid.skytree.api.Transaction> history = data.getEconHistory();

        history.add(0, new com.wiredid.skytree.api.Transaction(java.time.LocalDateTime.now(), amount, type, reason));

        // Limit to 50
        while (history.size() > 50) {
            history.remove(history.size() - 1);
        }

        persistence.savePlayerData(data);

        // Admin Logging
        if (amount >= 1000 || reason.contains("Admin") || type.contains("SET")) {
            plugin.getAdminService().logAction(uuid, "ECONOMY",
                    String.format("%s: %s (Reason: %s)", type, plugin.getEconomyService().format(amount), reason));
        }
    }

    @Override
    public java.util.List<com.wiredid.skytree.api.Transaction> getTransactions(UUID uuid) {
        PlayerData data = persistence.loadPlayerData(uuid);
        return data.getEconHistory();
    }

    @Override
    public Map<UUID, Double> getAllBalances() {
        return persistence.getAllBalances();
    }

    private String resolveReason() {
        // Simple heuristic to find caller
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (!className.equals(SkytreeEconomyService.class.getName())
                    && !className.startsWith("java.")
                    && !className.startsWith("jdk.")) {

                // Return simple class name logic
                String simple = className.substring(className.lastIndexOf('.') + 1);
                if (simple.contains("Listener"))
                    return simple.replace("Listener", "");
                if (simple.contains("Command"))
                    return simple.replace("Command", "");
                if (simple.contains("GUI"))
                    return simple.replace("GUI", "");
                if (simple.contains("Service"))
                    return simple;
                return simple;
            }
        }
        return "Unknown";
    }

    public void unloadBalance(UUID uuid) {
        balanceCache.remove(uuid);
    }
}
