package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.TagService;
import com.wiredid.skytree.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SkytreeTagService implements TagService {

    private final SkytreePlugin plugin;
    private final Map<String, TagData> tags = new HashMap<>();
    private File configFile;
    private YamlConfiguration config;

    public SkytreeTagService(SkytreePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public Map<String, TagData> getAvailableTags() {
        return tags;
    }

    @Override
    public TagData getTag(String id) {
        return tags.get(id);
    }

    @Override
    public void setActiveTag(Player player, String tagId) {
        TagData tag = tags.get(tagId);
        if (tag == null)
            return;

        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        data.setCustomPrefix(tag.getDisplay());
        // No explicit save needed if auto-persistence handles it,
        // but for safe measure:
        plugin.getPersistenceService().savePlayerData(data);
    }

    @Override
    public void removeActiveTag(Player player) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        data.setCustomPrefix(null);
        plugin.getPersistenceService().savePlayerData(data);
    }

    @Override
    public String getActiveTagDisplay(Player player) {
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        return data.getCustomPrefix();
    }

    @Override
    public boolean hasPermission(Player player, String tagId) {
        TagData tag = tags.get(tagId);
        if (tag == null)
            return false;
        return player.hasPermission(tag.getPermission());
    }

    @Override
    public void reload() {
        tags.clear();
        configFile = new File(plugin.getDataFolder(), "tags.yml");
        if (!configFile.exists()) {
            plugin.saveResource("tags.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection section = config.getConfigurationSection("tags");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection tagSec = section.getConfigurationSection(key);
                if (tagSec != null) {
                    tags.put(key, new TagData(
                            key,
                            tagSec.getString("display"),
                            tagSec.getString("permission"),
                            tagSec.getDouble("cost")));
                }
            }
        }
    }
}
