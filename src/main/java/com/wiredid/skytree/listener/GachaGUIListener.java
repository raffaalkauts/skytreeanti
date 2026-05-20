package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.gui.GachaGUI;
import com.wiredid.skytree.impl.MythicItemManager.GachaCrateDef;
import com.wiredid.skytree.impl.MythicItemManager.GachaGlobalConfig;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class GachaGUIListener implements Listener {
    private final SkytreePlugin plugin;

    public GachaGUIListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GachaGUI)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Identify crate by simple name matching (Fragile but simpler for now)
        // Or based on slot.
        // We know slots 11 and 15.

        GachaGlobalConfig config = plugin.getMythicItemManager().getGachaConfig();
        if (config == null)
            return;

        // Iterate config to find matching match
        String selectedCrate = null;
        String displayName = PlainTextComponentSerializer.plainText()
                .serialize(clickedItem.getItemMeta().displayName());

        for (Map.Entry<String, GachaCrateDef> entry : config.crate_types.entrySet()) {
            if (displayName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                selectedCrate = entry.getKey();
                break;
            }
        }

        if (selectedCrate != null) {
            ItemStack reward = plugin.getGachaService().spinGacha(player, selectedCrate);
            if (reward != null) {
                // Determine transaction outcome
                // Add item to inventory with safety drop
                java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(reward);
                if (!left.isEmpty()) {
                    left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                    player.sendMessage("§cInventory full! Reward dropped on the ground.");
                }

                // Refresh GUI to update pity
                player.openInventory(new GachaGUI(plugin, player).getInventory());
            } else {
                // Spin failed (e.g. money), service sent message.
                // Do nothing or sound effect?
            }
        }
    }

    @EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        // Check if item is a crate
        // We can check NBT or just string matching for now (simpler)
        // Or better: Check against MythicItemManager
        String itemId = plugin.getItemRegistry().getItemId(item);
        if (itemId == null)
            return;

        if (itemId.contains("ORACLE_CHEST") || itemId.contains("oracle_chest") ||
                itemId.contains("GACHA_CRATE") || itemId.contains("gacha_crate")) {
            event.setCancelled(true);

            // Determine crate type from ID
            String crateType = "basic";
            String upperId = itemId.toUpperCase();
            if (upperId.contains("DIVINE")) {
                crateType = "divine";
            } else if (upperId.contains("PREMIUM")) {
                crateType = "premium";
            } else if (upperId.contains("BASIC")) {
                crateType = "basic";
            }

            // Spin directly using the item
            plugin.getGachaService().spinGachaFromItem(event.getPlayer(), crateType, item);
        }
    }
}
