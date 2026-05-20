package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.ItemTransportService;

import org.bukkit.Location;
import org.bukkit.Material;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkytreeItemTransportService implements ItemTransportService {

    private final SkytreePlugin plugin;
    private final Map<Location, List<Location>> links = new ConcurrentHashMap<>();

    public SkytreeItemTransportService(SkytreePlugin plugin) {
        this.plugin = plugin;
        load();
        startTask();
    }

    @Override
    public boolean createLink(Location source, Location destination, UUID playerId) {
        if (source.equals(destination))
            return false;

        // Max 5 links per source
        List<Location> dests = links.computeIfAbsent(source, k -> new ArrayList<>());
        if (dests.size() >= 5)
            return false;

        if (dests.contains(destination))
            return false;

        dests.add(destination);
        return true;
    }

    @Override
    public boolean removeLink(Location source, UUID playerId) {
        return links.remove(source) != null;
    }

    @Override
    public void removeAllLinks(Location location, UUID playerId) {
        links.remove(location);
        // Also remove if it's a destination
        for (List<Location> dests : links.values()) {
            dests.removeIf(loc -> loc.equals(location));
        }
    }

    @Override
    public Map<Location, List<Location>> getLinks(UUID islandId) {
        // For simplicity, returning all links.
        // In a real implementation, we'd filter by island location.
        return links;
    }

    @Override
    public List<Location> getDestinations(Location source) {
        return links.getOrDefault(source, new ArrayList<>());
    }

    @Override
    public boolean isLinked(Location location) {
        return links.containsKey(location);
    }

    @Override
    public void transferItems(Location source) {
        Block block = source.getBlock();
        if (!(block.getState() instanceof InventoryHolder sourceHolder))
            return;

        Inventory sourceInv = sourceHolder.getInventory();
        List<Location> dests = links.get(source);
        if (dests == null || dests.isEmpty())
            return;

        for (int i = 0; i < sourceInv.getSize(); i++) {
            ItemStack stack = sourceInv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR)
                continue;

            // Try to move 1 item from this stack to any destination
            ItemStack toMove = stack.clone();
            toMove.setAmount(1);

            boolean moved = false;
            for (Location dest : dests) {
                if (dest.getBlock().getState() instanceof InventoryHolder destHolder) {
                    Inventory destInv = destHolder.getInventory();
                    HashMap<Integer, ItemStack> leftover = destInv.addItem(toMove);

                    if (leftover.isEmpty()) {
                        // Success!
                        stack.setAmount(stack.getAmount() - 1);
                        sourceInv.setItem(i, stack.getAmount() > 0 ? stack : null);
                        spawnLinkParticle(source, dest);
                        moved = true;
                        break;
                    }
                }
            }

            if (moved)
                return; // One item per cycle per source to keep it balanced
        }
    }

    @Override
    public void transferAllItems() {
        for (Location source : links.keySet()) {
            transferItems(source);
        }
    }

    @Override
    public int getLinkCount(Location source) {
        List<Location> dests = links.get(source);
        return dests != null ? dests.size() : 0;
    }

    @Override
    public boolean isMaxLinksReached(Location source) {
        return getLinkCount(source) >= 5;
    }

    @Override
    public void spawnLinkParticle(Location source, Location destination) {
        Location s = source.clone().add(0.5, 0.5, 0.5);
        Location d = destination.clone().add(0.5, 0.5, 0.5);

        // Draw a line of particles
        double distance = s.distance(d);
        if (distance > 32)
            return; // Too far

        org.bukkit.util.Vector vector = d.toVector().subtract(s.toVector()).normalize().multiply(0.5);
        for (double i = 0; i < distance; i += 0.5) {
            s.add(vector);
            source.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, s, 1, 0, 0, 0, 0);
        }
    }

    public void saveNow() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "transport_links.json");
        try (java.io.Writer writer = new java.io.FileWriter(file)) {
            TransportStorage storage = new TransportStorage();
            for (Map.Entry<Location, List<Location>> entry : links.entrySet()) {
                storage.links.add(new LinkEntry(entry.getKey(), entry.getValue()));
            }
            new com.google.gson.Gson().toJson(storage, writer);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Failed to save transport_links.json: " + e.getMessage());
        }
    }

    private void load() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "transport_links.json");
        if (!file.exists())
            return;

        try (java.io.Reader reader = new java.io.FileReader(file)) {
            TransportStorage storage = new com.google.gson.Gson().fromJson(reader, TransportStorage.class);
            if (storage != null && storage.links != null) {
                for (LinkEntry entry : storage.links) {
                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(entry.world);
                    if (world != null) {
                        Location source = new Location(world, entry.x, entry.y, entry.z);
                        List<Location> dests = new ArrayList<>();
                        for (DestEntry de : entry.destinations) {
                            org.bukkit.World dWorld = org.bukkit.Bukkit.getWorld(de.world);
                            if (dWorld != null) {
                                dests.add(new Location(dWorld, de.x, de.y, de.z));
                            }
                        }
                        links.put(source, Collections.synchronizedList(dests));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load transport_links.json: " + e.getMessage());
        }
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                transferAllItems();
            }
        }.runTaskTimer(plugin, 100L, 100L); // Every 5 seconds
    }

    private static class TransportStorage {
        List<LinkEntry> links = new ArrayList<>();
    }

    private static class LinkEntry {
        String world;
        int x, y, z;
        List<DestEntry> destinations = new ArrayList<>();

        LinkEntry(Location loc, List<Location> dests) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            for (Location d : dests) {
                this.destinations.add(new DestEntry(d));
            }
        }
    }

    private static class DestEntry {
        String world;
        int x, y, z;

        DestEntry(Location loc) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
        }
    }
}
