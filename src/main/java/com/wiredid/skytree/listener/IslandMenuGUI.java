package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
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

import java.util.Arrays;

/**
 * Main Island Menu to replace chat commands for Bedrock users
 */
public class IslandMenuGUI implements Listener {

    private static final String TITLE = "§6§lIsland §8» §7Navigator";

    private static final class IslandMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final SkytreePlugin plugin;

    public IslandMenuGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        Inventory gui = Bukkit.createInventory(new IslandMenuHolder(), 27, ComponentUtil.parse(TITLE));
        gui.setItem(11, createIcon(Material.GRASS_BLOCK, "§a§lCreate / Home", "§7Go to your island",
                "§7or create one"));
        gui.setItem(13, createIcon(Material.PLAYER_HEAD, "§e§lTeam Members", "§7Manage your team",
                "§7Invite/Kick/Trust"));
        gui.setItem(15, createIcon(Material.COMPARATOR, "§b§lSettings & Upgrades", "§7Island configuration",
                "§7and powerful upgrades"));
        gui.setItem(26, createIcon(Material.BARRIER, "§cClose", ""));

        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.CYAN_STAINED_GLASS_PANE,
                Material.WHITE_STAINED_GLASS_PANE);

        player.openInventory(gui);
    }

    private ItemStack createIcon(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(Arrays.asList(lore)));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof IslandMenuHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Don't cancel when clicking in player's own inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        switch (event.getSlot()) {
            case 11 -> {
                player.closeInventory();
                player.performCommand("is home");
            }
            case 13 -> plugin.getIslandService().getIsland(player.getUniqueId()).ifPresentOrElse(
                    island -> plugin.getTeamGUI().open(player, island),
                    () -> player.sendMessage("§c§l[Skytree] §cYou must have an island to manage team!"));
            case 15 -> player.performCommand("is settings");
            case 26 -> player.closeInventory();
            default -> {
            }
        }
    }
}
