package com.wiredid.skytree.api;

import com.wiredid.skytree.model.Island;

/**
 * Service for managing advanced island settings
 */
public interface IslandSettingsService {
    /**
     * Gets an island's setting value
     * 
     * @param island The island
     * @param key    The setting key
     * @return The setting value
     */
    boolean getSetting(Island island, String key);

    /**
     * Sets an island's setting
     * 
     * @param island The island
     * @param key    The setting key
     * @param value  The new value
     */
    void setSetting(Island island, String key, boolean value);
}

