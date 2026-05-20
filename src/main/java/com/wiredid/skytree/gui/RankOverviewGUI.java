package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Rank;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RankOverviewGUI implements Listener {

    private final SkytreePlugin plugin;

    public RankOverviewGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, ComponentUtil.parse("§6§lRanks §8» §7Menu"));

        // Premium Border
        GuiUtil.applyPremiumBorder(inv, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);

        // Rank Display
        Rank currentRank = plugin.getRankService().getRank(player.getUniqueId());

        inv.setItem(10, createRankItem(Rank.IOLITE, currentRank));
        inv.setItem(11, createRankItem(Rank.BERYL, currentRank));
        inv.setItem(12, createRankItem(Rank.GARNET, currentRank));
        inv.setItem(13, createRankItem(Rank.AMETHYST, currentRank));
        inv.setItem(14, createRankItem(Rank.EMERALD, currentRank));
        inv.setItem(15, createRankItem(Rank.DIVINE, currentRank));

        // Info Button
        inv.setItem(31, createInfoItem(player, currentRank));

        player.openInventory(inv);
    }

    private ItemStack createRankItem(Rank rank, Rank current) {
        ItemStack item = new ItemStack(getMaterialForRank(rank));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(rank.getPrefix()));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8» §7Benefits:");
        lore.add(" §f• §7Multiplier: §e" + rank.getMultiplier() + "x");
        lore.add(" §f• §7Member Bonus: §a+" + rank.getMemberBonus());
        lore.add(" §f• §7Spawner Bonus: §a+" + rank.getSpawnerBonus());
        lore.add("");
        lore.add("§8» §7Key Perks:");
        lore.addAll(getPerksForRank(rank));
        lore.add("");

        if (current == rank) {
            lore.add("§a§lCURRENT RANK");
            GuiUtil.addGlow(item);
        } else if (current.ordinal() > rank.ordinal()) {
            lore.add("§7§lUNLOCKED");
        } else {
            lore.add("§c§lLOCKED");
        }

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterialForRank(Rank rank) {
        return switch (rank) {
            case IOLITE -> Material.QUARTZ;
            case BERYL -> Material.DIAMOND;
            case GARNET -> Material.REDSTONE;
            case AMETHYST -> Material.AMETHYST_SHARD;
            case EMERALD -> Material.EMERALD;
            case DIVINE -> Material.NETHER_STAR;
            default -> Material.BARRIER;
        };
    }

    private List<String> getPerksForRank(Rank rank) {
        List<String> perks = new ArrayList<>();
        switch (rank) {
            case IOLITE -> perks.add(" §f• §7Standard Features");
            case BERYL -> perks.add(" §f• §7Access to §b/feed");
            case GARNET -> {
                perks.add(" §f• §7Access to §c/heal");
                perks.add(" §f• §7+2 Auction Slots");
            }
            case AMETHYST -> {
                perks.add(" §f• §7Access to §d/fly");
                perks.add(" §f• §7Amethyst Kit");
            }
            case EMERALD -> {
                perks.add(" §f• §7Access to §a/nick");
                perks.add(" §f• §7+10% Gacha Luck");
            }
            case DIVINE -> {
                perks.add(" §f• §7Divine Glow Effect");
                perks.add(" §f• §7Divine Kit");
                perks.add(" §f• §7Priority Support");
            }
            default -> perks.add(" §f• §7Staff Permissions");
        }
        return perks;
    }

    private ItemStack createInfoItem(Player player, Rank current) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§e§lYour Rank Status"));
        meta.lore(ComponentUtil.parseList(List.of(
                "§7Current Rank: " + current.getPrefix(),
                "§7Income Multiplier: §e" + current.getMultiplier() + "x",
                "",
                "§7Purchase ranks at §a/shop §7or through our store!")));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Ranks » Menu"))
            return;
        event.setCancelled(true);
    }
}
