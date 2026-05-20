package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.TeamBankService;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkytreeTeamBankService implements TeamBankService {

    private final SkytreePlugin plugin;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final File file;
    private YamlConfiguration config;

    public SkytreeTeamBankService(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "team_banks.yml");
        reload();
    }

    @Override
    public double getBalance(UUID islandId) {
        return balances.getOrDefault(islandId, 0.0);
    }

    @Override
    public void deposit(UUID playerId, UUID islandId, double amount) {
        if (plugin.getEconomyService().removeBalance(playerId, amount)) {
            double current = getBalance(islandId);
            balances.put(islandId, current + amount);
            save();
        }
    }

    @Override
    public boolean withdraw(UUID playerId, UUID islandId, double amount) {
        double current = getBalance(islandId);
        if (current >= amount) {
            balances.put(islandId, current - amount);
            plugin.getEconomyService().addBalance(playerId, amount);
            save();
            return true;
        }
        return false;
    }

    @Override
    public void reload() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        balances.clear();
        for (String key : config.getKeys(false)) {
            balances.put(UUID.fromString(key), config.getDouble(key));
        }
    }

    @Override
    public void save() {
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
