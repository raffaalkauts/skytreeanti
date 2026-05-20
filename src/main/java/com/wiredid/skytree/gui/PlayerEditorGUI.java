package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
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

import java.util.List;

public class PlayerEditorGUI implements Listener {

    private final SkytreePlugin plugin;

    public PlayerEditorGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, Player target) {
        Inventory inv = Bukkit.createInventory(null, 36, ComponentUtil.parse("§c§lEditor §8» §7" + target.getName()));

        // Premium Border
        GuiUtil.applyPremiumBorder(inv, Material.ORANGE_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);

        // Target Info
        inv.setItem(13, createPlayerHead(target));

        // Economy Actions
        inv.setItem(10, createItem(Material.GOLD_INGOT, "§6Manage Balance",
                List.of("§7Current: §f"
                        + NumberUtil.formatCurrency(plugin.getEconomyService().getBalance(target.getUniqueId())),
                        "", "§eLeft-Click: §a+1,000 USDT", "§eRight-Click: §c-1,000 USDT",
                        "§eMiddle-Click: §fSet to 0")));

        // Inventory Actions
        inv.setItem(11, createItem(Material.CHEST, "§eOpen Inventory",
                List.of("§7View and edit target's inventory.", "", "§eClick to open!")));
        inv.setItem(15, createItem(Material.ENDER_CHEST, "§dOpen Ender Chest",
                List.of("§7View and edit target's ender chest.", "", "§eClick to open!")));

        // Teleport Actions
        inv.setItem(19, createItem(Material.ENDER_PEARL, "§bTeleport to Player",
                List.of("§7Instantly warp to this player.", "", "§eClick to warp!")));
        inv.setItem(25, createItem(Material.GRASS_BLOCK, "§aTeleport to Island",
                List.of("§7Instantly warp to player's island.", "", "§eClick to warp!")));

        // Rank Actions
        com.wiredid.skytree.model.Rank rank = plugin.getRankService().getRank(target.getUniqueId());
        inv.setItem(22, createItem(Material.NETHER_STAR, "§bManage Rank",
                List.of("§7Current Rank: " + rank.getPrefix(),
                        "", "§eClick to cycle to the next rank")));

        // Back button
        inv.setItem(22, createItem(Material.ARROW, "§cBack to Lookup", List.of("§7Return to the player list.")));

        admin.openInventory(inv);
    }

    private ItemStack createPlayerHead(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        var meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        com.wiredid.skytree.model.Rank rank = plugin.getRankService().getRank(target.getUniqueId());
        meta.displayName(ComponentUtil.parse("§b§l" + target.getName()));
        meta.lore(ComponentUtil.parseList(List.of("§7Rank: " + rank.getPrefix())));
        head.setItemMeta(meta);
        return head;
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
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Editor » "))
            return;
        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player admin = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        String targetName = title.split(" » ")[1].substring(2);
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            admin.sendMessage("§cPlayer is no longer online.");
            admin.closeInventory();
            return;
        }

        switch (clicked.getType()) {
            case GOLD_INGOT -> {
                if (event.isLeftClick()) {
                    plugin.getEconomyService().addBalance(target.getUniqueId(), 1000);
                    admin.sendMessage("§aAdded 1,000 USDT to " + target.getName());
                } else if (event.isRightClick()) {
                    plugin.getEconomyService().removeBalance(target.getUniqueId(), 1000);
                    admin.sendMessage("§cRemoved 1,000 USDT from " + target.getName());
                } else if (event.getClick().isCreativeAction()) { // Middle click
                    plugin.getEconomyService().setBalance(target.getUniqueId(), 0);
                    admin.sendMessage("§7Set balance of " + target.getName() + " to 0");
                }
                open(admin, target); // Refresh
            }
            case CHEST -> plugin.getAdminService().openInventory(admin, target);
            case ENDER_CHEST -> plugin.getAdminService().openEnderChest(admin, target);
            case ENDER_PEARL -> {
                admin.teleport(target.getLocation());
                admin.sendMessage("§aTeleported to " + target.getName());
            }
            case GRASS_BLOCK -> {
                plugin.getIslandService().getIsland(target.getUniqueId()).ifPresentOrElse(
                        island -> {
                            admin.teleport(island.getCenter());
                            admin.sendMessage("§aTeleported to " + target.getName() + "'s island");
                        },
                        () -> admin.sendMessage("§cTarget has no island."));
            }
            case NETHER_STAR -> {
                com.wiredid.skytree.model.Rank current = plugin.getRankService().getRank(target.getUniqueId());
                com.wiredid.skytree.model.Rank[] ranks = com.wiredid.skytree.model.Rank.values();
                int nextIdx = (current.ordinal() + 1) % ranks.length;
                com.wiredid.skytree.model.Rank next = ranks[nextIdx];

                plugin.getRankService().setRank(target.getUniqueId(), next);
                admin.sendMessage("§a§l[Skytree] §aUpdated " + target.getName() + "'s rank to " + next.getPrefix());
                target.setGlowing(next == com.wiredid.skytree.model.Rank.DIVINE);
                open(admin, target);
            }
            case ARROW -> plugin.getAdminDashboardGUI().openLookup(admin);
            default -> {
            }
        }
    }
}
