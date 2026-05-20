package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VaultListener implements Listener {

    private final SkytreePlugin plugin;

    public VaultListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.startsWith("§8§lVAULT")) {
            return;
        }

        Inventory inv = event.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null) {
                items.add(item);
            }
        }

        PlayerData data = plugin.getPersistenceService().loadPlayerData(event.getPlayer().getUniqueId());
        data.getVaultItems().clear();
        data.getVaultItems().addAll(items);
        plugin.getPersistenceService().savePlayerData(data);
    }
}
