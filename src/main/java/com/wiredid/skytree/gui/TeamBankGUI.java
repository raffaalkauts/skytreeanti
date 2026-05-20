package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import com.wiredid.skytree.util.NumberUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TeamBankGUI implements Listener {

    private final SkytreePlugin plugin;

    public TeamBankGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        var islandOpt = plugin.getIslandService().getIsland(player.getUniqueId());
        if (islandOpt.isEmpty()) {
            player.sendMessage("§cYou must belong to an island to access the team bank!");
            return;
        }

        Island island = islandOpt.get();
        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.smartParse("§3§lIsland Team Bank"));
        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);

        double balance = plugin.getTeamBankService().getBalance(island.getIslandId());

        // Balance Info
        inv.setItem(13, GuiUtil.createItem(Material.GOLD_BLOCK, "§6§lTeam Balance",
                "§fCurrent: §a" + NumberUtil.formatCurrency(balance),
                "§7Sharing is caring!"));

        // Deposit Actions
        inv.setItem(11, GuiUtil.createItem(Material.LIME_DYE, "§a§lDeposit 1,000 USDT",
                "§7Click to deposit from your personal account."));
        inv.setItem(10, GuiUtil.createItem(Material.LIME_WOOL, "§a§lDeposit 10,000 USDT",
                "§7Click to deposit from your personal account."));

        // Withdraw Actions (Only for trustworthy members?)
        inv.setItem(15, GuiUtil.createItem(Material.ORANGE_DYE, "§6§lWithdraw 1,000 USDT",
                "§7Click to withdraw to your personal account."));
        inv.setItem(16, GuiUtil.createItem(Material.ORANGE_WOOL, "§6§lWithdraw 10,000 USDT",
                "§7Click to withdraw to your personal account."));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Island Team Bank"))
            return;

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        var islandOpt = plugin.getIslandService().getIsland(player.getUniqueId());
        if (islandOpt.isEmpty())
            return;
        UUID islandId = islandOpt.get().getIslandId();

        double amount = 0;
        boolean deposit = false;

        switch (event.getSlot()) {
            case 11 -> {
                amount = 1000;
                deposit = true;
            }
            case 10 -> {
                amount = 10000;
                deposit = true;
            }
            case 15 -> {
                amount = 1000;
                deposit = false;
            }
            case 16 -> {
                amount = 10000;
                deposit = false;
            }
        }

        if (amount > 0) {
            if (deposit) {
                if (plugin.getEconomyService().getBalance(player.getUniqueId()) >= amount) {
                    plugin.getTeamBankService().deposit(player.getUniqueId(), islandId, amount);
                    player.sendMessage("§a§l[Bank] §fDeposited §e" + amount + " USDT §fto the team bank.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                } else {
                    player.sendMessage("§cInsufficient personal balance!");
                }
            } else {
                // Check if island owner or trusted (for now just check if member)
                if (plugin.getTeamBankService().withdraw(player.getUniqueId(), islandId, amount)) {
                    player.sendMessage("§6§l[Bank] §fWithdrew §e" + amount + " USDT §ffrom the team bank.");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    player.sendMessage("§cInsufficient team bank balance!");
                }
            }
            open(player); // Refresh
        }
    }
}
