package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.EconomyService;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.gui.*;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Universal GUI listener for all island GUIs
 */
public class IslandGUIListener implements Listener {

    private final SkytreePlugin plugin;
    private final IslandService islandService;
    private final EconomyService economy;
    private final PersistenceService persistence;

    public IslandGUIListener(SkytreePlugin plugin, IslandService islandService,
            EconomyService economy, PersistenceService persistence) {
        this.plugin = plugin;
        this.islandService = islandService;
        this.economy = economy;
        this.persistence = persistence;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());

        // Specific Island GUI checks instead of broad color-based check
        boolean isIslandGui = title.contains("Change Island Biome") ||
                title.contains("Island Upgrades") ||
                title.contains("Island Permissions") ||
                title.contains("Top Islands");

        if (isIslandGui) {
            if (!(event.getWhoClicked() instanceof Player player))
                return;

            if (event.getClickedInventory() == player.getInventory())
                return;

            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            // Route to appropriate handler
            if (title.contains("Change Island Biome")) {
                handleBiomeClick(player, clicked);
            } else if (title.contains("Island Upgrades")) {
                handleUpgradeClick(player, clicked);
            } else if (title.contains("Island Permissions")) {
                handlePermissionsClick(player);
            }
        }
    }

    private void handleBiomeClick(Player player, ItemStack clicked) {
        islandService.getIsland(player.getUniqueId()).ifPresent(island -> {
            String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                    ? ComponentUtil.toLegacy(clicked.getItemMeta().displayName())
                    : "";
            String biome = extractBiomeName(name);
            double cost = extractCost(clicked);

            double adjustedCost = cost;
            if (plugin.getEconomyManager() != null) {
                adjustedCost = cost * plugin.getEconomyManager().getPriceMultiplier();
                plugin.getEconomyManager().addToReserve(adjustedCost);
            }
            if (adjustedCost > 0 && economy.removeBalance(player.getUniqueId(), adjustedCost)) {
                island.setBiome(biome);
                persistence.saveIsland(island);
                player.sendMessage("§a§l[Skytree] §aBiome changed to " + biome + "!");
                player.closeInventory();

                // Change actual biome in world (simplified)
                changeBiome(island, biome);
            } else if (adjustedCost > 0) {
                player.sendMessage("§c§l[Skytree] §cInsufficient funds! Need §e"
                        + com.wiredid.skytree.util.NumberUtil.formatCurrency(cost));
            }
        });
    }

    private void handleUpgradeClick(Player player, ItemStack clicked) {
        islandService.getIsland(player.getUniqueId()).ifPresent(island -> {
            String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                    ? ComponentUtil.toLegacy(clicked.getItemMeta().displayName())
                    : "";
            double cost = extractCost(clicked);

            double adjustedCost = cost;
            if (plugin.getEconomyManager() != null) {
                adjustedCost = cost * plugin.getEconomyManager().getPriceMultiplier();
                plugin.getEconomyManager().addToReserve(adjustedCost);
            }
            if (name.contains("Island Size")) {
                int currentLevel = island.getUpgrades().getOrDefault("size", 0);
                if (currentLevel < 5 && economy.removeBalance(player.getUniqueId(), adjustedCost)) {
                    island.getUpgrades().put("size", currentLevel + 1);
                    island.setSize(island.getSize() + 50);
                    persistence.saveIsland(island);
                    player.sendMessage("§a§l[Skytree] §aIsland size upgraded!");
                    new UpgradeGUI(plugin).open(player, island);
                } else if (currentLevel >= 5) {
                    player.sendMessage("§c§l[Skytree] §cMax level reached!");
                } else {
                    player.sendMessage("§c§l[Skytree] §cInsufficient funds!");
                }
            } else if (name.contains("Max Members")) {
                double adjMembers = cost;
                if (plugin.getEconomyManager() != null) {
                    adjMembers = cost * plugin.getEconomyManager().getPriceMultiplier();
                    plugin.getEconomyManager().addToReserve(adjMembers);
                }
                int currentLevel = island.getUpgrades().getOrDefault("members", 0);
                if (currentLevel < 5 && economy.removeBalance(player.getUniqueId(), adjMembers)) {
                    island.getUpgrades().put("members", currentLevel + 1);
                    persistence.saveIsland(island);
                    player.sendMessage("§a§l[Skytree] §aMember limit upgraded!");
                    new UpgradeGUI(plugin).open(player, island);
                } else if (currentLevel >= 5) {
                    player.sendMessage("§c§l[Skytree] §cMax level reached!");
                } else {
                    player.sendMessage("§c§l[Skytree] §cInsufficient funds!");
                }
            } else if (name.contains("Cobble Generator")) {
                double adjGenerator = cost;
                if (plugin.getEconomyManager() != null) {
                    adjGenerator = cost * plugin.getEconomyManager().getPriceMultiplier();
                    plugin.getEconomyManager().addToReserve(adjGenerator);
                }
                int currentLevel = island.getUpgrades().getOrDefault("generator", 0);
                if (currentLevel < 10 && economy.removeBalance(player.getUniqueId(), adjGenerator)) {
                    island.getUpgrades().put("generator", currentLevel + 1);
                    persistence.saveIsland(island);
                    player.sendMessage("§a§l[Skytree] §aGenerator upgraded!");
                    new UpgradeGUI(plugin).open(player, island);
                } else if (currentLevel >= 10) {
                    player.sendMessage("§c§l[Skytree] §cMax level reached!");
                } else {
                    player.sendMessage("§c§l[Skytree] §cInsufficient funds!");
                }
            }
        });
    }

    private void handlePermissionsClick(Player player) {
        // Permission changes - simplified for now
        player.sendMessage("§e§l[Skytree] §7Use §e/is invite <player> §7to add members");
    }

    private String extractBiomeName(String displayName) {
        // Remove color codes and extract biome name
        return displayName.replaceAll("§.", "").trim().toUpperCase().replace(" ", "_");
    }

    private double extractCost(ItemStack item) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore())
            return 0;

        java.util.List<net.kyori.adventure.text.Component> adventureLore = meta.lore();
        List<String> lore = adventureLore != null
                ? adventureLore.stream().map(ComponentUtil::toLegacy).collect(Collectors.toList())
                : new java.util.ArrayList<>();
        for (String line : lore) {
            if (line.contains("Cost:") || line.contains("Price:")) {
                try {
                    return Double.parseDouble(line.replaceAll("[^0-9.]", ""));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private void changeBiome(Island island, String biomeName) {
        org.bukkit.block.Biome biome;
        try {
            biome = org.bukkit.block.Biome.valueOf(biomeName);
        } catch (IllegalArgumentException e) {
            return;
        }

        Location center = island.getCenter();
        int radius = island.getSize() / 2;
        World world = center.getWorld();
        if (world == null)
            return;

        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Set biome for the entire vertical column
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 8) {
                        world.setBiome(x, y, z, biome);
                    }
                }
            }
        });
    }
}
