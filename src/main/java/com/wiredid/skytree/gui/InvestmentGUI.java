package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.InvestmentService;
import com.wiredid.skytree.model.Investment;
import com.wiredid.skytree.model.InvestmentType;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvestmentGUI implements Listener {

    private final SkytreePlugin plugin;
    private final InvestmentService investmentService;

    public InvestmentGUI(SkytreePlugin plugin, InvestmentService investmentService) {
        this.plugin = plugin;
        this.investmentService = investmentService;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§3§lInvestment Exchange"));
        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE);

        // Stocks Section (Slots 10, 11, 12)
        inv.setItem(10, createStockItem(player, "emerald_corp", Material.EMERALD));
        inv.setItem(11, createStockItem(player, "diamond_mining", Material.DIAMOND));
        inv.setItem(12, createStockItem(player, "skytree_logistics", Material.CHEST_MINECART));

        // Bonds Section (Slots 14, 15, 16)
        inv.setItem(14, createBondItem(player, "short_term", Material.PAPER));
        inv.setItem(15, createBondItem(player, "medium_term", Material.BOOK));
        inv.setItem(16, createBondItem(player, "long_term", Material.WRITABLE_BOOK));

        // Portfolio Title
        inv.setItem(31,
                GuiUtil.createItem(Material.COMPASS, "§6§lYour Portfolio", "§7View your active investments below."));

        // Active Investments (Bottom Row)
        List<Investment> active = investmentService.getPlayerInvestments(player.getUniqueId());
        int slot = 37;
        for (Investment invData : active) {
            if (slot > 43)
                break;
            inv.setItem(slot++, createPortfolioItem(invData));
        }

        player.openInventory(inv);
    }

    private ItemStack createStockItem(Player player, String id, Material icon) {
        double price = investmentService.getStockPrice(id);
        int owned = investmentService.getPlayerShares(player.getUniqueId(), id);

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§b§lStock: §f" + id));
        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7Current Price: §a₮ " + plugin.getEconomyService().format(price)));
        lore.add(ComponentUtil.smartParse("§7You Own: §e" + owned + " shares"));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§eLeft-Click §7to Buy 1 Share"));
        lore.add(ComponentUtil.smartParse("§eRight-Click §7to Sell 1 Share"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBondItem(Player player, String id, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§d§lBond: §f" + id));
        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7Reliable returns over time."));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§eClick to purchase this bond."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPortfolioItem(Investment inv) {
        Material mat = inv.getType() == InvestmentType.STOCK ? Material.PAPER : Material.KNOWLEDGE_BOOK;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§6§l" + inv.getType().name() + ": " + inv.getAssetId()));

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7Invested: §f₮ " + plugin.getEconomyService().format(inv.getAmount())));
        if (inv.getType() == InvestmentType.STOCK) {
            lore.add(ComponentUtil.smartParse("§7Shares: §f" + inv.getShares()));
            double current = investmentService.getStockPrice(inv.getAssetId()) * inv.getShares();
            double profit = current - inv.getAmount();
            String color = profit >= 0 ? "§a+" : "§c";
            lore.add(ComponentUtil
                    .smartParse("§7Current Value: " + color + "₮ " + plugin.getEconomyService().format(current)));
        } else {
            long remaining = (inv.getMaturityTime() - System.currentTimeMillis()) / 1000;
            if (remaining <= 0) {
                lore.add(ComponentUtil.smartParse("§a§lMATURED"));
                lore.add(ComponentUtil.smartParse("§eClick to claim reward!"));
            } else {
                lore.add(ComponentUtil
                        .smartParse("§7Time Remaining: §f" + (remaining / 60) + "m " + (remaining % 60) + "s"));
            }
        }

        meta.lore(lore);
        // Store ID for claiming
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "inv_id"),
                org.bukkit.persistence.PersistentDataType.STRING, inv.getInvestmentId().toString());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Investment Exchange"))
            return;

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());
        if (name.contains("Stock:")) {
            String id = name.split(": ")[1].substring(2);
            if (event.isLeftClick()) {
                if (investmentService.buyStock(player.getUniqueId(), id, 1)) {
                    player.sendMessage("§aPurchased 1 share of " + id);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    player.sendMessage("§cInsufficient funds!");
                }
            } else if (event.isRightClick()) {
                if (investmentService.sellStock(player.getUniqueId(), id, 1)) {
                    player.sendMessage("§aSold 1 share of " + id);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.5f);
                } else {
                    player.sendMessage("§cYou don't own any shares of " + id);
                }
            }
            open(player);
        } else if (name.contains("Bond:")) {
            String id = name.split(": ")[1].substring(2);
            // Just open a fixed purchase for now (10k)
            if (investmentService.purchaseBond(player.getUniqueId(), id, 10000) != null) {
                player.sendMessage("§aBond purchased! It will mature in the future.");
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f);
            } else {
                player.sendMessage("§cCannot afford bond (10,000 USDT)!");
            }
            open(player);
        } else if (event.getSlot() >= 37 && event.getSlot() <= 43) {
            String idStr = clicked.getItemMeta().getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey(plugin, "inv_id"), org.bukkit.persistence.PersistentDataType.STRING);
            if (idStr != null) {
                if (investmentService.claimBond(player.getUniqueId(), UUID.fromString(idStr))) {
                    player.sendMessage("§aBond claimed successfully!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    open(player);
                }
            }
        }
    }
}
