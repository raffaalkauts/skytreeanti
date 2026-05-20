package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.WorthService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class WorthListener implements Listener {

    private final SkytreePlugin plugin;
    private final WorthService worthService;

    public WorthListener(SkytreePlugin plugin, WorthService worthService) {
        this.plugin = plugin;
        this.worthService = worthService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                worthService.updateInventoryLore(event.getPlayer());
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        if (cursor != null && clicked != null && cursor.getType() != Material.AIR
                && clicked.getType() != Material.AIR) {
            if (cursor.getType() == clicked.getType() && !cursor.isSimilar(clicked)) {
                if (worthService.isSimilarIgnoringWorth(cursor, clicked)) {
                    int amount = clicked.getAmount();
                    int onCursor = cursor.getAmount();
                    int max = clicked.getMaxStackSize();

                    if (amount < max) {
                        int canTake = max - amount;
                        int toTake = Math.min(canTake, onCursor);

                        clicked.setAmount(amount + toTake);
                        cursor.setAmount(onCursor - toTake);

                        event.setCancelled(true);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (cursor.getAmount() <= 0) {
                                player.setItemOnCursor(null);
                            } else {
                                player.setItemOnCursor(cursor);
                            }
                            player.updateInventory();
                            scheduleWorthRefresh(player);
                        });
                        return;
                    }
                }
            }
        }

        if (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
            scheduleWorthRefresh(player);
            return;
        }
        scheduleWorthRefresh(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        scheduleWorthRefresh(player);
    }

    private void scheduleWorthRefresh(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                if (plugin.getWorthVisualSystem() != null) {
                    plugin.getWorthVisualSystem().scheduleWorthUpdate(player);
                }
            }
        });
    }
}