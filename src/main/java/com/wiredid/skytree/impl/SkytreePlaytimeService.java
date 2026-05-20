package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.PlaytimeService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SkytreePlaytimeService implements PlaytimeService, Listener {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Long> totalPlaytimeCache = new HashMap<>();
    private final Map<UUID, Long> joinTimeCache = new HashMap<>();

    public SkytreePlaytimeService(SkytreePlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "playtime.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playtime.yml!");
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        loadAll();

        for (Player p : Bukkit.getOnlinePlayers()) {
            joinTimeCache.put(p.getUniqueId(), System.currentTimeMillis());
            if (!totalPlaytimeCache.containsKey(p.getUniqueId())) {
                totalPlaytimeCache.put(p.getUniqueId(), config.getLong(p.getUniqueId().toString(), 0L));
            }
        }
    }

    private void loadAll() {
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                totalPlaytimeCache.put(uuid, config.getLong(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save(UUID uuid) {
        Long stored = totalPlaytimeCache.get(uuid);
        if (stored != null) {
            config.set(uuid.toString(), stored);
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAll() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (joinTimeCache.containsKey(id)) {
                long session = now - joinTimeCache.get(id);
                long currentTotal = totalPlaytimeCache.getOrDefault(id, 0L);
                config.set(id.toString(), currentTotal + session);
            } else {
                config.set(id.toString(), totalPlaytimeCache.getOrDefault(id, 0L));
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getPlaytimeMillis(UUID player) {
        long base = totalPlaytimeCache.getOrDefault(player, 0L);
        if (joinTimeCache.containsKey(player)) {
            return base + (System.currentTimeMillis() - joinTimeCache.get(player));
        }
        return base;
    }

    @Override
    public String getFormattedPlaytime(UUID player) {
        long millis = getPlaytimeMillis(player);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return String.format("%dh %dm", hours, minutes);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        joinTimeCache.put(id, System.currentTimeMillis());
        if (!totalPlaytimeCache.containsKey(id)) {
            totalPlaytimeCache.put(id, config.getLong(id.toString(), 0L));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (joinTimeCache.containsKey(id)) {
            long session = System.currentTimeMillis() - joinTimeCache.get(id);
            long newTotal = totalPlaytimeCache.getOrDefault(id, 0L) + session;
            totalPlaytimeCache.put(id, newTotal);
            joinTimeCache.remove(id);
            save(id);
        }
    }

    @Override
    public Map<UUID, Long> getAllPlaytimeMillis() {
        Map<UUID, Long> all = new HashMap<>(totalPlaytimeCache);
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : joinTimeCache.entrySet()) {
            all.put(entry.getKey(), totalPlaytimeCache.getOrDefault(entry.getKey(), 0L) + (now - entry.getValue()));
        }
        return all;
    }
}
