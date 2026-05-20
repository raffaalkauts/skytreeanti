package com.wiredid.skytree.model;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents multiple warp points on an island
 */
public class IslandWarps {

    private final Map<String, Location> warps = new HashMap<>();

    public void setWarp(String name, Location location) {
        warps.put(name.toLowerCase(), location);
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public void removeWarp(String name) {
        warps.remove(name.toLowerCase());
    }

    public Map<String, Location> getAllWarps() {
        return new HashMap<>(warps);
    }

    public int getWarpCount() {
        return warps.size();
    }
}

