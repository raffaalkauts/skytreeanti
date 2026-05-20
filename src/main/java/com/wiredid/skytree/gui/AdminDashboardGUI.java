package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
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

import java.util.List;

public class AdminDashboardGUI implements Listener {

    private static final class AdminDashboardHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final SkytreePlugin plugin;
    private PlayerLookupGUI lookupGUI;
    private PlayerEditorGUI editorGUI;

    public AdminDashboardGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void setGuis(PlayerLookupGUI lookup, PlayerEditorGUI editor) {
        this.lookupGUI = lookup;
        this.editorGUI = editor;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new AdminDashboardHolder(), 27,
                ComponentUtil.parse("§c§lAdmin §8» §7Dashboard"));

        GuiUtil.applyPremiumBorder(inv, Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);

        boolean inStaff = plugin.getAdminService().isInStaffMode(player);
        inv.setItem(10, createItem(Material.DIAMOND_SWORD, "§e§lStaff Mode",
                List.of("§7Status: " + (inStaff ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle!")));

        inv.setItem(12, createItem(Material.BOOK, "§c§lSystem Logs",
                List.of("§7View recent administrative", "§7and system actions.", "", "§eClick to open!")));

        inv.setItem(14, createItem(Material.PLAYER_HEAD, "§b§lPlayer Lookup",
                List.of("§7Manage online players,", "§7edit stats and islands.", "", "§eClick to open!")));

        inv.setItem(16, createItem(Material.GRASS_BLOCK, "§a§lIsland Hub",
                List.of("§7Quick access to island", "§7management tools.", "", "§eClick to open!")));

        player.openInventory(inv);
    }

    public void openLookup(Player admin) {
        if (lookupGUI != null) {
            lookupGUI.open(admin, 0);
        }
    }

    public void openEditorGUI(Player admin, Player target) {
        if (editorGUI != null) {
            editorGUI.open(admin, target);
        }
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AdminDashboardHolder)) {
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

        switch (clicked.getType()) {
            case DIAMOND_SWORD -> {
                plugin.getAdminService().toggleStaffMode(player);
                open(player);
            }
            case BOOK -> plugin.getAdminLogsGUI().open(player);
            case PLAYER_HEAD -> openLookup(player);
            case GRASS_BLOCK -> plugin.getIslandMenuGUI().openMenu(player);
            default -> {
            }
        }
    }
}
