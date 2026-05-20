package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.LeaderboardService;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardGUI implements Listener {

    private final SkytreePlugin plugin;
    private final LeaderboardService leaderboardService;

    public LeaderboardGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.leaderboardService = plugin.getLeaderboardService();
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lRankings §8» §7Global"));

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE);

        // Money Leaderboard
        inv.setItem(10, createCategoryItem(Material.GOLD_INGOT, "§6§lTop Money", "§7View the wealthiest players."));
        // Shard Leaderboard
        inv.setItem(12, createCategoryItem(Material.PRISMARINE_SHARD, "§b§lTop Shards",
                "§7View players with the most shards."));
        // Playtime Leaderboard
        inv.setItem(14, createCategoryItem(Material.CLOCK, "§e§lTop Playtime", "§7View the most active players."));
        // Bounty Leaderboard
        inv.setItem(16, createCategoryItem(Material.SKELETON_SKULL, "§c§lTop Bounties",
                "§7View players with the highest bounties."));
        // Island Leaderboard
        inv.setItem(13, createCategoryItem(Material.GRASS_BLOCK, "§a§lTop Islands",
                "§7View the highest level islands."));

        player.openInventory(inv);
    }

    private ItemStack createCategoryItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        List<String> loreList = new ArrayList<>();
        loreList.add(lore);
        loreList.add("");
        loreList.add("§eClick to view!");
        meta.lore(ComponentUtil.parseList(loreList));
        item.setItemMeta(meta);
        return item;
    }

    public void openCategory(Player player, String category) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.parse("§6§lRankings §8» §7" + category));

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(ComponentUtil.parse("§cBack to Categories"));
        back.setItemMeta(backMeta);
        inv.setItem(40, back);

        java.util.List<Integer> interiorSlots = com.wiredid.skytree.util.GuiUtil.getInteriorSlots(54);
        int listIndex = 0;

        switch (category) {
            case "Money":
                for (Map.Entry<UUID, Double> entry : leaderboardService.getTopMoney(27)) {
                    if (listIndex >= interiorSlots.size())
                        break;
                    inv.setItem(interiorSlots.get(listIndex++),
                            createPlayerHead(entry.getKey(), listIndex,
                                    NumberUtil.formatCurrency(entry.getValue())));
                }
                break;
            case "Shards":
                for (Map.Entry<UUID, Integer> entry : leaderboardService.getTopShards(27)) {
                    if (listIndex >= interiorSlots.size())
                        break;
                    inv.setItem(interiorSlots.get(listIndex++),
                            createPlayerHead(entry.getKey(), listIndex, "§b" + entry.getValue() + " Shards"));
                }
                break;
            case "Playtime":
                for (Map.Entry<UUID, Long> entry : leaderboardService.getTopPlaytime(27)) {
                    if (listIndex >= interiorSlots.size())
                        break;
                    inv.setItem(interiorSlots.get(listIndex++), createPlayerHead(entry.getKey(), listIndex,
                            "§e" + plugin.getPlaytimeService().getFormattedPlaytime(entry.getKey())));
                }
                break;
            case "Bounties":
                for (Map.Entry<UUID, Double> entry : leaderboardService.getTopBounties(27)) {
                    if (listIndex >= interiorSlots.size())
                        break;
                    inv.setItem(interiorSlots.get(listIndex++),
                            createPlayerHead(entry.getKey(), listIndex,
                                    NumberUtil.formatCurrency(entry.getValue())));
                }
                break;
            case "Islands":
                for (Map.Entry<UUID, Integer> entry : leaderboardService.getTopIslands(27)) {
                    if (listIndex >= interiorSlots.size())
                        break;
                    inv.setItem(interiorSlots.get(listIndex++),
                            createPlayerHead(entry.getKey(), listIndex, "§aLevel " + entry.getValue()));
                }
                break;
        }

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.BLACK_STAINED_GLASS_PANE,
                category.equals("Money") ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);

        player.openInventory(inv);
    }

    private ItemStack createPlayerHead(UUID uuid, int rank, String value) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(op);
        meta.displayName(ComponentUtil.parse("§6#" + rank + " §f" + (op.getName() != null ? op.getName() : "Unknown")));
        List<String> lore = new ArrayList<>();
        lore.add("§7Value: " + value);
        meta.lore(ComponentUtil.parseList(lore));
        head.setItemMeta(meta);
        return head;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Rankings") && !title.contains("Leaderboard") && !title.startsWith("§8Top "))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (title.contains("Global")) {
            if (clicked.getType() == Material.GOLD_INGOT)
                openCategory(player, "Money");
            else if (clicked.getType() == Material.PRISMARINE_SHARD)
                openCategory(player, "Shards");
            else if (clicked.getType() == Material.CLOCK)
                openCategory(player, "Playtime");
            else if (clicked.getType() == Material.SKELETON_SKULL)
                openCategory(player, "Bounties");
            else if (clicked.getType() == Material.GRASS_BLOCK)
                openCategory(player, "Islands");
        } else if (clicked.getType() == Material.ARROW) {
            openMain(player);
        }
    }
}
