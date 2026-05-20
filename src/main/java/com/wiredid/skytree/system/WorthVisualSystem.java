package com.wiredid.skytree.system;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.WorthService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Always-On Worth Lore System (DonutSMP style).
 *
 * Worth lore is applied persistently to items in the player's inventory.
 * It is NOT removed when the inventory is closed, ensuring zero-delay feel.
 * Lore is only stripped from items when they leave the inventory (dropped).
 */
public class WorthVisualSystem implements Listener {

    private final SkytreePlugin plugin;
    private final WorthService worthService;

    public WorthVisualSystem(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.worthService = plugin.getWorthService();
    }

    /**
     * Trigger an instant worth lore update for a player.
     * Checks worth_display setting before applying.
     */
    public void scheduleWorthUpdate(Player player) {
        if (!player.isOnline() || plugin.getPersistenceService() == null) return;
        com.wiredid.skytree.model.PlayerData data = plugin.getPersistenceService()
                .loadPlayerData(player.getUniqueId());
        if (data != null && data.getSettings().getOrDefault("worth_display", true)) {
            worthService.updateInventoryLore(player);
        }
    }

    /**
     * Re-evaluate worth display setting and apply/strip accordingly.
     */
    public void refreshPlayer(Player player) {
        if (plugin.getPersistenceService() == null) return;
        com.wiredid.skytree.model.PlayerData data = plugin.getPersistenceService()
                .loadPlayerData(player.getUniqueId());
        if (data != null && data.getSettings().getOrDefault("worth_display", true)) {
            worthService.updateInventoryLore(player);
        } else {
            worthService.removeInventoryLore(player);
        }
    }

    /**
     * Strip worth lore from a player's inventory (used on quit / setting disable).
     */
    public void clearPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            worthService.removeInventoryLore(player);
        }
    }

    // ──────────────────────────────────────────────
    //  Event Handlers
    // ──────────────────────────────────────────────

    /**
     * Apply lore when player opens any inventory (player inv, chest, etc.).
     * Combined with persistent lore, this ensures items always look up-to-date
     * the moment the GUI is visible.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            scheduleWorthUpdate(player);
        }
    }

    /**
     * Apply lore to newly picked-up items immediately.
     * Lore is applied to the Item on the ground BEFORE it enters inventory,
     * so Bukkit's stacking logic sees matching lore and stacks correctly.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.getPersistenceService() != null) {
            com.wiredid.skytree.model.PlayerData data = plugin.getPersistenceService()
                    .loadPlayerData(event.getEntity().getUniqueId());
            if (data == null || !data.getSettings().getOrDefault("worth_display", true)) return;
        }

        // Pre-apply lore to the ground item so it matches inventory items on stack
        org.bukkit.entity.Item itemEntity = event.getItem();
        org.bukkit.inventory.ItemStack stack = itemEntity.getItemStack();
        if (stack != null && stack.getType() != org.bukkit.Material.AIR) {
            worthService.updateItemLore(stack);
            itemEntity.setItemStack(stack);
        }
    }

    /**
     * Strip worth lore from items when the player drops them.
     * Prevents items on the ground from displaying worth lore.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Item dropped = event.getItemDrop();
        // Strip on next tick to avoid interfering with vanilla drop logic
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (dropped.isValid()) {
                worthService.stripSingleItemLore(dropped.getItemStack());
                dropped.setItemStack(dropped.getItemStack()); // Force entity update
            }
        });
    }

    /**
     * Clean up lore when player quits so saved inventory is lore-free.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer().getUniqueId());
    }
}
