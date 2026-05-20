package com.wiredid.skytree.economy;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobPersistenceService {

    private final SkytreePlugin plugin;
    private final File dataFile;
    private YamlConfiguration config;

    private final Map<UUID, JobData> cache = new ConcurrentHashMap<>();

    public JobPersistenceService(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "jobs_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[Jobs] Failed to create jobs_data.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public JobData load(UUID playerId) {
        if (cache.containsKey(playerId)) {
            return cache.get(playerId);
        }

        JobData data = new JobData(playerId);
        String path = "jobs." + playerId.toString();
        ConfigurationSection sec = config.getConfigurationSection(path);
        if (sec != null) {
            for (String jobId : sec.getKeys(false)) {
                int level = sec.getInt(jobId + ".level", 0);
                double xp = sec.getDouble(jobId + ".xp", 0);
                double earned = sec.getDouble(jobId + ".total_earned", 0);
                int actions = sec.getInt(jobId + ".actions", 0);
                data.getJobs().put(jobId, new JobData.JobProgress(jobId, level, xp, earned, actions));
            }
        }

        cache.put(playerId, data);
        return data;
    }

    public void save(UUID playerId, JobData data) {
        String path = "jobs." + playerId.toString();
        config.set(path, null);

        for (Map.Entry<String, JobData.JobProgress> entry : data.getJobs().entrySet()) {
            String jp = path + "." + entry.getKey();
            config.set(jp + ".level", entry.getValue().level);
            config.set(jp + ".xp", entry.getValue().xp);
            config.set(jp + ".total_earned", entry.getValue().totalEarned);
            config.set(jp + ".actions", entry.getValue().actions);
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Jobs] Failed to save jobs_data.yml: " + e.getMessage());
        }

        cache.put(playerId, data);
    }

    public void unload(UUID playerId) {
        cache.remove(playerId);
    }

    public void saveAll() {
        for (Map.Entry<UUID, JobData> entry : cache.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    public void reload() {
        cache.clear();
        this.config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public List<UUID> getAllPlayerIds() {
        ConfigurationSection sec = config.getConfigurationSection("jobs");
        if (sec == null) return new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            try {
                ids.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {}
        }
        return ids;
    }
}
