package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.model.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Handles inventory swapping between normal world and island world
 */
public class WorldInventoryListener implements Listener {

    private final PersistenceService persistenceService;
    private final String islandWorldName;

    public WorldInventoryListener(SkytreePlugin plugin) {
        this.persistenceService = plugin.getPersistenceService();
        this.islandWorldName = plugin.getConfig().getString("world.name", "skytree_world");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = persistenceService.loadPlayerData(player.getUniqueId());

        // Load appropriate inventory based on current world
        boolean isInIslandWorld = player.getWorld().getName().equals(islandWorldName);
        loadInventoryForWorld(player, data, isInIslandWorld);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData data = persistenceService.loadPlayerData(player.getUniqueId());

        // Save current inventory to appropriate storage
        boolean isInIslandWorld = player.getWorld().getName().equals(islandWorldName);
        saveInventoryForWorld(player, data, isInIslandWorld);

        persistenceService.savePlayerData(data);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlayerData data = persistenceService.loadPlayerData(player.getUniqueId());

        // Determine which worlds we're transitioning between
        boolean wasInIslandWorld = event.getFrom().getName().equals(islandWorldName);
        boolean isInIslandWorld = player.getWorld().getName().equals(islandWorldName);

        // Only swap if actually changing between island and normal worlds
        if (wasInIslandWorld != isInIslandWorld) {
            // CRITICAL: Close inventory to force cursor items back into inventory before
            // saving
            player.closeInventory();

            // Save current inventory to the world we're leaving
            saveInventoryForWorld(player, data, wasInIslandWorld);

            // Load inventory for the world we're entering
            loadInventoryForWorld(player, data, isInIslandWorld);

            persistenceService.savePlayerData(data);
        }
    }

    /**
     * Save player's current inventory to the appropriate storage
     */
    private void saveInventoryForWorld(Player player, PlayerData data, boolean isIslandWorld) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = new ItemStack[41];

        // Save main inventory (36 slots)
        ItemStack[] contents = inv.getContents();
        System.arraycopy(contents, 0, storage, 0, Math.min(contents.length, 36));

        // Save armor (4 slots)
        ItemStack[] armor = inv.getArmorContents();
        System.arraycopy(armor, 0, storage, 36, Math.min(armor.length, 4));

        // Save offhand (1 slot)
        storage[40] = inv.getItemInOffHand();

        // Store in appropriate field
        if (isIslandWorld) {
            data.setIslandInventory(storage);
        } else {
            data.setNormalInventory(storage);
        }
    }

    /**
     * Load inventory from storage for the current world
     */
    private void loadInventoryForWorld(Player player, PlayerData data, boolean isIslandWorld) {
        ItemStack[] storage = isIslandWorld ? data.getIslandInventory() : data.getNormalInventory();

        // If no saved inventory exists, clear the player's inventory
        if (storage == null) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(null);
            return;
        }

        PlayerInventory inv = player.getInventory();

        // Load main inventory (36 slots)
        ItemStack[] contents = new ItemStack[36];
        System.arraycopy(storage, 0, contents, 0, 36);
        inv.setContents(contents);

        // Load armor (4 slots)
        ItemStack[] armor = new ItemStack[4];
        System.arraycopy(storage, 36, armor, 0, 4);
        inv.setArmorContents(armor);

        // Load offhand (1 slot)
        inv.setItemInOffHand(storage[40]);
    }
}
