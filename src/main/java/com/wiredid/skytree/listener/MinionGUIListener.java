package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.MinionService;
import com.wiredid.skytree.gui.MinionInventoryHolder;
import com.wiredid.skytree.gui.MinionStorageGUI;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MinionGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final MinionService minionService;
    private final MinionStorageGUI storageGUI;

    public MinionGUIListener(SkytreePlugin plugin, MinionService minionService) {
        this.plugin = plugin;
        this.minionService = minionService;
        this.storageGUI = new MinionStorageGUI(plugin, minionService);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ComponentUtil.parse("§6§lMinion Menu")))
            return;

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);
        if (!(event.getInventory().getHolder() instanceof MinionInventoryHolder holder))
            return;

        UUID minionId = holder.getMinionId();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        int slot = event.getRawSlot();

        // Storage Button
        if (slot == 29) {
            storageGUI.open(player, minionId);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        }
        // Upgrade Button
        else if (slot == 31) {
            if (minionService.upgradeMinion(minionId, player.getUniqueId())) {
                player.sendMessage("§a[Minion] §fUpgraded successfully!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                // Refresh GUI
                new com.wiredid.skytree.gui.MinionGUI(plugin, minionService).open(player, minionId);
            } else {
                player.sendMessage("§c[Minion] §fNot enough money or max level reached.");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
        }
        // Skins Button
        else if (slot == 33) {
            plugin.getMinionSkinGUI().open(player, minionId);
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
        }
        // Pickup Button
        else if (slot == 49) {
            com.wiredid.skytree.model.MinionData data = minionService.getMinionData(minionId);
            if (data != null && minionService.removeMinion(minionId)) {
                player.sendMessage("§a[Minion] §fMinion picked up!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                player.closeInventory();

                // Give minion item back
                String itemId = "minion_" + data.getType().name().toLowerCase();
                ItemStack minionItem = plugin.getItemRegistry().getItem(itemId);
                if (minionItem != null) {
                    player.getInventory().addItem(minionItem).values()
                            .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                }

                // Drop storage items
                for (ItemStack item : minionService.clearStorage(minionId)) {
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(data.getLocation(), item);
                    }
                }
            }
        }
    }
}
