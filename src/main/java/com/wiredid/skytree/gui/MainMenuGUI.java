package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGUI implements Listener {

    private final SkytreePlugin plugin;
    private final NamespacedKey ACTION_KEY;

    public MainMenuGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.ACTION_KEY = new NamespacedKey(plugin, "menu_action");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§8§lMain Menu"));

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, createPane());
        }

        inv.setItem(13, createPlayerHead(player));

        // 🏝️ ISLAND (row 1: 9-17)
        inv.setItem(9, createButton(Material.GRASS_BLOCK, "§a§lISLAND HOME", "ISLAND_HOME", "§7Teleport to your island", "", "§eClick"));
        inv.setItem(10, createButton(Material.ENDER_PEARL, "§d§lWARPS", "WARPS", "§7Island warp points", "", "§eClick"));
        inv.setItem(11, createButton(Material.ZOMBIE_HEAD, "§2§lMINIONS", "MINIONS", "§7Manage your minions", "", "§eClick"));
        inv.setItem(14, createButton(Material.ANVIL, "§6§lUPGRADES", "UPGRADES", "§7Upgrade your island", "", "§eClick"));
        inv.setItem(15, createButton(Material.PLAYER_HEAD, "§b§lMEMBERS", "MEMBERS", "§7Manage island members", "", "§eClick"));
        inv.setItem(16, createButton(Material.OAK_FENCE_GATE, "§7§lISLAND SETTINGS", "ISLAND_SETTINGS", "§7Configure island", "", "§eClick"));

        // 💰 ECONOMY (row 2: 18-26)
        inv.setItem(18, createButton(Material.GOLD_INGOT, "§6§lBANK", "BANK", "§7Deposit, withdraw, transfer", "", "§eClick"));
        inv.setItem(19, createButton(Material.DIAMOND_PICKAXE, "§b§lJOBS", "JOBS", "§7View job progression", "", "§eClick"));
        inv.setItem(20, createButton(Material.EMERALD, "§a§lSHOP", "SHOP", "§7Buy and sell items", "", "§eClick"));
        inv.setItem(21, createButton(Material.GOLDEN_HORSE_ARMOR, "§e§lMARKET", "MARKET", "§7Player Auction House", "", "§eClick"));
        inv.setItem(22, createButton(Material.TOTEM_OF_UNDYING, "§c§lBOUNTIES", "BOUNTIES", "§7View and set bounties", "", "§eClick"));
        inv.setItem(23, createButton(Material.ENDER_EYE, "§d§lSHARD SHOP", "SHARDS", "§7Spend your shards", "", "§eClick"));
        inv.setItem(24, createButton(Material.SUNFLOWER, "§e§lINVESTMENTS", "INVESTMENTS", "§7Bonds and investments", "", "§eClick"));

        // 🎮 FEATURES (row 3: 27-35)
        inv.setItem(27, createButton(Material.KNOWLEDGE_BOOK, "§d§lQUESTS", "QUESTS", "§7Complete quests for rewards", "", "§eClick"));
        inv.setItem(28, createButton(Material.CHEST, "§5§lCRATES", "CRATES", "§7Open crates and keys", "", "§eClick"));
        inv.setItem(29, createButton(Material.CLOCK, "§6§lDAILY REWARDS", "DAILY", "§7Claim daily rewards", "", "§eClick"));
        inv.setItem(30, createButton(Material.BEACON, "§b§lLEADERBOARD", "LEADERBOARD", "§7View server top lists", "", "§eClick"));
        inv.setItem(31, createButton(Material.CRAFTING_TABLE, "§a§lITEMS CATALOG", "ITEMS", "§7Browse all custom items", "", "§eClick"));
        inv.setItem(32, createButton(Material.DIAMOND_SWORD, "§b§lKITS", "KITS", "§7Claim your kits", "", "§eClick"));
        inv.setItem(34, createButton(Material.GOLD_BLOCK, "§6§lTEAM BANK", "TEAM_BANK", "§7Shared island bank", "", "§eClick"));

        // ⚙️ BOTTOM (row 5: 45-53)
        inv.setItem(45, createButton(Material.COMPARATOR, "§7§lSETTINGS", "SETTINGS", "§7Configure preferences", "", "§eClick"));
        inv.setItem(47, createButton(Material.FISHING_ROD, "§b§lFISHING", "FISHING", "§7Manage fishing", "", "§eClick"));
        inv.setItem(49, createButton(Material.BOOK, "§a§lGUIDE", "GUIDE", "§7How to play Skytree", "", "§eClick"));
        inv.setItem(53, createButton(Material.BARRIER, "§c§lCLOSE", "CLOSE", "§7Close this menu"));

        player.openInventory(inv);
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(ComponentUtil.smartParse("§e§l" + player.getName()));

        List<Component> lore = new ArrayList<>();
        String rankDisplay = plugin.getRankService().getRank(player.getUniqueId()).getDisplayName();
        lore.add(ComponentUtil.smartParse("§7Rank: " + rankDisplay));
        lore.add(ComponentUtil.smartParse(" "));
        lore.add(ComponentUtil.smartParse("§fBalance: §e"
                + NumberUtil.formatCurrency(plugin.getEconomyService().getBalance(player.getUniqueId()))));
        lore.add(ComponentUtil.smartParse("§fShards: §b" + plugin.getShardService().getShards(player.getUniqueId())));
        lore.add(ComponentUtil.smartParse(" "));

        var island = plugin.getIslandService().getIsland(player.getUniqueId());
        if (island.isPresent()) {
            lore.add(ComponentUtil.smartParse("§fIsland Level: §a" + island.get().getLevel()));
        } else {
            lore.add(ComponentUtil.smartParse("§fIsland: §cNone"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material material, String name, String action, String... description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse(name));

        List<Component> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(ComponentUtil.smartParse(line));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(" "));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ComponentUtil.smartParse("§8§lMain Menu")))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta())
            return;

        String action = clicked.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        if (action == null)
            return;

        player.closeInventory();

        switch (action) {
            case "ISLAND_HOME" -> player.performCommand("is home");
            case "WARPS" -> player.performCommand("warp");
            case "MINIONS" -> player.performCommand("minion");
            case "UPGRADES" -> player.performCommand("is upgrades");
            case "MEMBERS" -> player.performCommand("trustlist");
            case "ISLAND_SETTINGS" -> player.performCommand("settings");
            case "BANK" -> player.performCommand("bank");
            case "JOBS" -> player.performCommand("jobs");
            case "SHOP" -> plugin.getShopService().openShop(player);
            case "MARKET" -> plugin.getAuctionHouseGUI().open(player,
                    com.wiredid.skytree.gui.AuctionHouseGUI.ViewType.LISTINGS, 0,
                    com.wiredid.skytree.gui.AuctionHouseGUI.SortType.NEWEST);
            case "BOUNTIES" -> player.performCommand("bounty");
            case "SHARDS" -> player.performCommand("shards");
            case "INVESTMENTS" -> player.performCommand("bank");
            case "QUESTS" -> player.performCommand("quest");
            case "CRATES" -> player.performCommand("crates");
            case "DAILY" -> player.performCommand("daily");
            case "LEADERBOARD" -> player.performCommand("leaderboard");
            case "ITEMS" -> player.performCommand("items");
            case "KITS" -> player.performCommand("kits");
            case "TEAM_BANK" -> player.performCommand("bank");
            case "SETTINGS" -> player.performCommand("settings");
            case "FISHING" -> player.performCommand("fish");
            case "GUIDE" -> player.performCommand("guide");
            case "CLOSE" -> {}
        }
    }
}
