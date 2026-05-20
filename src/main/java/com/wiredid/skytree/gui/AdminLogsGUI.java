package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.AdminService;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminLogsGUI implements Listener {

    private static final class AdminLogsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final AdminService adminService;

    public AdminLogsGUI(SkytreePlugin plugin) {
        this.adminService = plugin.getAdminService();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new AdminLogsHolder(), 54,
                ComponentUtil.parse("§c§lAdmin §8» §7Logs"));

        GuiUtil.applyPremiumBorder(inv, Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);

        List<String> logs = adminService.getRecentLogs(21);
        int slot = 10;
        int count = 0;

        for (String log : logs) {
            if (count >= 21) {
                break;
            }
            while (slot % 9 == 0 || slot % 9 == 8 || slot < 9 || slot > 34) {
                slot++;
            }
            inv.setItem(slot++, createLogItem(log));
            count++;
        }

        inv.setItem(39, createActionButton(Material.PAPER, "§fRefresh Logs", "§7Click to update logs."));
        inv.setItem(40, createActionButton(Material.BARRIER, "§cClose", "§7Exit the logs viewer."));
        inv.setItem(41, createActionButton(Material.TNT, "§4Clear Memory",
                "§7Clear logs from memory §8(File persists)."));

        player.openInventory(inv);
    }

    private ItemStack createLogItem(String log) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        String[] parts = log.split(" ", 4);
        String category = parts.length > 2 ? parts[2].replace("[", "").replace("]", "") : "LOG";
        String content = parts.length > 3 ? parts[3] : log;

        meta.displayName(ComponentUtil.parse("§e" + category));
        List<String> lore = new ArrayList<>();
        lore.add("§8" + parts[0] + " " + parts[1]);
        lore.add("");

        String[] words = content.split(" ");
        StringBuilder line = new StringBuilder("§7");
        for (String word : words) {
            if (line.length() + word.length() > 40) {
                lore.add(line.toString());
                line = new StringBuilder("§7");
            }
            line.append(word).append(" ");
        }
        lore.add(line.toString());

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionButton(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        List<String> loreList = new ArrayList<>();
        loreList.add(lore);
        meta.lore(ComponentUtil.parseList(loreList));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AdminLogsHolder)) {
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.PAPER) {
            open(player);
        } else if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        } else if (clicked.getType() == Material.TNT) {
            player.sendMessage("§cLogs cleared from memory.");
            open(player);
        }
    }
}
