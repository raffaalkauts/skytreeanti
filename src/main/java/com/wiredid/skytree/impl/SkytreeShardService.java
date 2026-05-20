package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ShardService;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkytreeShardService implements ShardService {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Integer> cache = new HashMap<>();

    public SkytreeShardService(SkytreePlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "shards.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create shards.yml!");
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int amount = config.getInt(key);
                cache.put(uuid, amount);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public void addShards(UUID player, int amount) {
        setShards(player, getShards(player) + amount);
    }

    @Override
    public void removeShards(UUID player, int amount) {
        setShards(player, Math.max(0, getShards(player) - amount));
    }

    @Override
    public void setShards(UUID player, int amount) {
        cache.put(player, amount);
        config.set(player.toString(), amount);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getShards(UUID player) {
        return cache.getOrDefault(player, 0);
    }

    @Override
    public boolean hasShards(UUID player, int amount) {
        return getShards(player) >= amount;
    }

    @Override
    public Map<UUID, Integer> getAllShards() {
        return new HashMap<>(cache);
    }
}
