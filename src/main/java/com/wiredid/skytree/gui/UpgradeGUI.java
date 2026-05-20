package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import com.wiredid.skytree.util.NumberUtil;
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

public class UpgradeGUI implements Listener {

        private final SkytreePlugin plugin;

        public UpgradeGUI(SkytreePlugin plugin) {
                this.plugin = plugin;
        }

        public void open(Player player, Island island) {
                Inventory inv = Bukkit.createInventory(null, 36, ComponentUtil.parse("§6§lIsland §8» §7Upgrades"));

                // Premium Border
                GuiUtil.applyPremiumBorder(inv, Material.CYAN_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE);

                // Upgrade Tracks
                inv.setItem(10, createUpgradeItem(island, "size", "Island Size", Material.GRASS_BLOCK,
                                "Current: " + island.getSize() + "x" + island.getSize(),
                                "Next: " + (island.getSize() + 25) + "x" + (island.getSize() + 25), 5000, 5));

                inv.setItem(12, createUpgradeItem(island, "members", "Max Members", Material.PLAYER_HEAD,
                                "Current: " + island.getMemberLimit() + " members",
                                "Next: " + (island.getMemberLimit() + 2) + " members", 3000, 5));

                inv.setItem(14, createUpgradeItem(island, "spawners", "Spawner Limit", Material.SPAWNER,
                                "Current: " + island.getSpawnerLimit() + " spawners",
                                "Next: " + (island.getSpawnerLimit() + 8) + " spawners", 7500, 5));

                inv.setItem(16, createUpgradeItem(island, "generator", "Generator Rates", Material.COBBLESTONE,
                                "Current: +" + (int) ((island.getGeneratorMultiplier() - 1.0) * 100) + "% rate",
                                "Next: +" + (int) ((island.getGeneratorMultiplier() - 0.9) * 100) + "% rate", 2000,
                                10));

                // Info Button
                inv.setItem(22, createInfoItem());

                player.openInventory(inv);
        }

        private ItemStack createUpgradeItem(Island island, String key, String name, Material mat, String current,
                        String next, double baseCost, int maxLevel) {
                int level = island.getUpgrades().getOrDefault(key, 0);
                double rawCost = baseCost * Math.pow(1.5, level);
                double cost = plugin.getEconomyManager() != null
                        ? rawCost * plugin.getEconomyManager().getPriceMultiplier()
                        : rawCost;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ComponentUtil.parse("§b§l" + name));

                List<String> lore = new ArrayList<>();
                lore.add("§7Level: §e" + level + "/" + maxLevel);
                lore.add("");
                lore.add("§f" + current);
                if (level < maxLevel) {
                        lore.add("§a" + next);
                        lore.add("");
                        lore.add("§7Cost: §6" + NumberUtil.formatCurrency(cost));
                        lore.add("");
                        lore.add("§eClick to upgrade!");
                } else {
                        lore.add("");
                        lore.add("§cMAX LEVEL REACHED");
                }

                meta.lore(ComponentUtil.parseList(lore));
                item.setItemMeta(meta);
                return item;
        }

        private ItemStack createInfoItem() {
                ItemStack item = new ItemStack(Material.BOOK);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ComponentUtil.parse("§e§lUpgrade Information"));
                meta.lore(ComponentUtil.parseList(List.of(
                                "§7Upgrading your island grants permanent",
                                "§7benefits to all members.",
                                "",
                                "§7Costs increase with each level.")));
                item.setItemMeta(meta);
                return item;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Island » Upgrades"))
            return;
        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;
        event.setCancelled(true);

                if (!(event.getWhoClicked() instanceof Player))
                        return;
                Player player = (Player) event.getWhoClicked();
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR)
                        return;

                plugin.getIslandService().getIsland(player.getUniqueId()).ifPresent(island -> {
                        String upgradeKey = null;
                        double baseCost = 0;
                        int maxLevel = 0;

                        switch (clicked.getType()) {
                                case GRASS_BLOCK -> {
                                        upgradeKey = "size";
                                        baseCost = 5000;
                                        maxLevel = 5;
                                }
                                case PLAYER_HEAD -> {
                                        upgradeKey = "members";
                                        baseCost = 3000;
                                        maxLevel = 5;
                                }
                                case SPAWNER -> {
                                        upgradeKey = "spawners";
                                        baseCost = 7500;
                                        maxLevel = 5;
                                }
                                case COBBLESTONE -> {
                                        upgradeKey = "generator";
                                        baseCost = 2000;
                                        maxLevel = 10;
                                }
                                default -> {
                                }
                        }

                        if (upgradeKey != null) {
                                handleUpgrade(player, island, upgradeKey, baseCost, maxLevel);
                        }
                });
        }

        private void handleUpgrade(Player player, Island island, String key, double baseCost, int maxLevel) {
                int level = island.getUpgrades().getOrDefault(key, 0);
                if (level >= maxLevel) {
                        player.sendMessage("§cYou have already reached the maximum level for this upgrade!");
                        return;
                }

                double rawCost = baseCost * Math.pow(1.5, level);
                double cost = plugin.getEconomyManager() != null
                        ? rawCost * plugin.getEconomyManager().getPriceMultiplier()
                        : rawCost;
                if (plugin.getEconomyService().removeBalance(player.getUniqueId(), cost)) {
                        if (plugin.getEconomyManager() != null) {
                                plugin.getEconomyManager().addToReserve(cost);
                        }
                        island.getUpgrades().put(key, level + 1);

                        // Special handling for size
                        if (key.equals("size")) {
                                island.setSize(island.getSize() + 25);
                        }

                        island.getUpgrades().put(key, level + 1);

                        if (key.equals("size")) {
                                island.setSize(island.getSize() + 25);
                        }

                        plugin.getPersistenceService().saveIsland(island);
                        player.sendMessage("§a§l[Skytree] §aSuccessfully upgraded " + key + " to level " + (level + 1)
                                        + "!");
                        open(player, island);

                        // Log to Admin
                        plugin.getAdminService().logAction(player.getUniqueId(), "ISLAND",
                                        String.format("Upgraded %s to level %d (Cost: %s)", key, level + 1,
                                                        NumberUtil.formatCurrency(cost)));
                } else {
                        player.sendMessage(
                                        "§c§l[Skytree] §cInsufficient funds! Need " + NumberUtil.formatCurrency(cost));
                }
        }
}
