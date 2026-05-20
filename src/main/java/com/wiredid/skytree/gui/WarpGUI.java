package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class WarpGUI implements Listener {

    private final SkytreePlugin plugin;

    public WarpGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lFast Travel §8» §7Warps"));

        plugin.getIslandService().getIsland(player.getUniqueId()).ifPresent(island -> {
            Map<String, org.bukkit.Location> warps = island.getWarps().getAllWarps();
            int slot = 10;

            for (String name : warps.keySet()) {
                if (slot > 16)
                    break;
                inv.setItem(slot++, createWarpIcon(name, island.getWarps().getWarp(name)));
            }
        });

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.BLUE_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE);

        player.openInventory(inv);
    }

    private ItemStack createWarpIcon(String name, org.bukkit.Location loc) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§e§lWarp: §6" + name));
        meta.lore(ComponentUtil.parseList(
                "§7Location: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                "",
                "§aClick to teleport!"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onWarpClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Fast Travel"))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() != Material.COMPASS)
            return;

        String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName()).replace("§e§lWarp: §6", "");
        player.closeInventory();
        player.performCommand("warp " + name);
    }
}
