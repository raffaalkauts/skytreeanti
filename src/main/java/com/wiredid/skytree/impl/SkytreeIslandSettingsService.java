package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandSettingsService;
import com.wiredid.skytree.model.Island;

/**
 * Island settings service implementation
 */
public class SkytreeIslandSettingsService implements IslandSettingsService {

    private final SkytreePlugin plugin;

    public SkytreeIslandSettingsService(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean getSetting(Island island, String key) {
        return island.getSettings().getOrDefault(key, false);
    }

    @Override
    public void setSetting(Island island, String key, boolean value) {
        island.getSettings().put(key, value);

        // Save to disk to persist settings
        try {
            // Get persistence service when needed (lazy loading to avoid circular
            // dependency)
            if (plugin.getPersistenceService() != null) {
                plugin.getPersistenceService().saveIsland(island);
                plugin.getLogger()
                        .fine("Saved island setting: " + key + " = " + value + " for island " + island.getIslandId());
            }
        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Failed to save island settings for island " + island.getIslandId() + ": "
                            + e.getMessage());
        }
    }
}
