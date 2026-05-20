package com.wiredid.skytree.api;

import org.bukkit.entity.Player;

import java.util.Map;

public interface TagService {

    /**
     * Get all available tags defined in config
     */
    Map<String, TagData> getAvailableTags();

    /**
     * Get a specific tag's data
     */
    TagData getTag(String id);

    /**
     * Set a player's active tag
     */
    void setActiveTag(Player player, String tagId);

    /**
     * Remove a player's active tag
     */
    void removeActiveTag(Player player);

    /**
     * Get a player's active tag display string
     */
    String getActiveTagDisplay(Player player);

    /**
     * Check if a player has permission for a tag
     */
    boolean hasPermission(Player player, String tagId);

    /**
     * Reload tags from config
     */
    void reload();

    class TagData {
        private final String id;
        private final String display;
        private final String permission;
        private final double cost;

        public TagData(String id, String display, String permission, double cost) {
            this.id = id;
            this.display = display;
            this.permission = permission;
            this.cost = cost;
        }

        public String getId() {
            return id;
        }

        public String getDisplay() {
            return display;
        }

        public String getPermission() {
            return permission;
        }

        public double getCost() {
            return cost;
        }
    }
}
