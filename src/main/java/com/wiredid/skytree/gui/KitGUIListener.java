package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.command.KitCommand;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class KitGUIListener implements Listener {

    private final KitCommand kitCommand;
    private final NamespacedKey kitKey;

    public KitGUIListener(SkytreePlugin plugin, EconomyService economy, KitCommand kitCommand) {
        this.kitCommand = kitCommand;
        this.kitKey = new NamespacedKey(plugin, "kit_id");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta())
            return;

        PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();
        // Primary check: unique NBT key
        if (!data.has(kitKey, PersistentDataType.STRING))
            return;

        // Secondary check: loose title match to avoid accidental overlap
        String title = ComponentUtil.stripColor(event.getView().title()).toLowerCase();
        if (!title.contains("kit") && !title.contains("selector"))
            return;

        event.setCancelled(true);
        String kitId = data.get(kitKey, PersistentDataType.STRING);

        processKitClaim(player, kitId);
    }

    private void processKitClaim(Player player, String kitId) {
        kitCommand.claimKit(player, kitId);
        player.closeInventory();
    }
}
