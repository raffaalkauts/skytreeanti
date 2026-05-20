package com.wiredid.skytree.gui;

import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class PerformanceMonitorGUI implements Listener {

    public PerformanceMonitorGUI() {
    }

    public void open(Player player) {
        if (!player.isOp()) {
            player.sendMessage("§cYou must be an operator to view performance stats.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.smartParse("§4§lPerformance Monitor"));
        GuiUtil.applyPremiumBorder(inv, Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);

        // TPS
        double tps = Bukkit.getTPS()[0]; // 1-minute average
        Material tpsMat = tps >= 19.0 ? Material.GREEN_WOOL : tps >= 15.0 ? Material.YELLOW_WOOL : Material.RED_WOOL;
        inv.setItem(11, GuiUtil.createItem(tpsMat, "§a§lTPS (1m Avg)",
                "§fCurrent: §e" + String.format("%.2f", tps),
                tps >= 19.0 ? "§aExcellent!" : tps >= 15.0 ? "§eGood" : "§cPoor"));

        // Memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;
        double usage = (usedMemory * 100.0) / maxMemory;

        Material memMat = usage < 70 ? Material.GREEN_CONCRETE
                : usage < 85 ? Material.YELLOW_CONCRETE : Material.RED_CONCRETE;
        inv.setItem(13, GuiUtil.createItem(memMat, "§6§lMemory Usage",
                "§fUsed: §e" + usedMemory + " MB §7/ §e" + maxMemory + " MB",
                "§fUsage: §e" + String.format("%.1f%%", usage),
                usage < 70 ? "§aHealthy" : usage < 85 ? "§eModerate" : "§cHigh!"));

        // Online Players
        int online = Bukkit.getOnlinePlayers().size();
        inv.setItem(15, GuiUtil.createItem(Material.PLAYER_HEAD, "§b§lOnline Players",
                "§fCurrent: §a" + online,
                "§7Click to refresh"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Performance Monitor"))
            return;

        event.setCancelled(true);

        // Refresh on any click
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            open(player);
        }
    }
}
