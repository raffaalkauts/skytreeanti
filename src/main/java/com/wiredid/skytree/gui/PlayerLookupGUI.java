package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerLookupGUI implements Listener {

    private static final class PlayerLookupHolder implements InventoryHolder {
        private final int page;

        private PlayerLookupHolder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final SkytreePlugin plugin;

    public PlayerLookupGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, int page) {
        Inventory inv = Bukkit.createInventory(new PlayerLookupHolder(page), 54,
                ComponentUtil.parse("§c§lAdmin §8» §7Player Lookup"));

        GuiUtil.applyPremiumBorder(inv, Material.CYAN_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int start = page * 21;
        int slot = 10;

        for (int i = start; i < Math.min(start + 21, players.size()); i++) {
            Player target = players.get(i);
            while (slot % 9 == 0 || slot % 9 == 8 || slot < 9 || slot > 34) {
                slot++;
            }
            inv.setItem(slot++, createPlayerHead(target));
        }

        if (page > 0) {
            inv.setItem(39, createNavButton(Material.ARROW, "§ePrevious Page"));
        }
        inv.setItem(40, createNavButton(Material.BARRIER, "§cBack to Dashboard"));
        if (start + 21 < players.size()) {
            inv.setItem(41, createNavButton(Material.ARROW, "§eNext Page"));
        }

        admin.openInventory(inv);
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(ComponentUtil.parse("§b" + player.getName()));

        List<String> lore = new ArrayList<>();
        lore.add("§7Balance: §f" + NumberUtil.formatCurrency(plugin.getEconomyService().getBalance(player.getUniqueId())));
        lore.add("§7Island: §f" + (plugin.getIslandService().hasIsland(player.getUniqueId()) ? "§aYes" : "§cNo"));
        lore.add("");
        lore.add("§eClick to edit player!");

        meta.lore(ComponentUtil.parseList(lore));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createNavButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PlayerLookupHolder holder)) {
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            OfflinePlayer target = meta.getOwningPlayer();
            if (target != null && target.isOnline()) {
                plugin.getAdminDashboardGUI().openEditorGUI(admin, target.getPlayer());
            }
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            plugin.getAdminDashboardGUI().open(admin);
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());
            if (name.contains("Next Page")) {
                open(admin, holder.page + 1);
            } else if (name.contains("Previous Page") && holder.page > 0) {
                open(admin, holder.page - 1);
            }
        }
    }
}
