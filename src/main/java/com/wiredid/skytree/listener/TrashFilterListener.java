package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class TrashFilterListener implements Listener {

    private final SkytreePlugin plugin;

    public TrashFilterListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Material material = event.getItem().getItemStack().getType();
        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());

        if (data.getTrashFilter().contains(material.name())) {
            event.setCancelled(true);
            event.getItem().remove(); // Delete the item
        }
    }
}
