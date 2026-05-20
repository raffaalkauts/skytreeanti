package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class SettingsMainGUI implements Listener {

    private static final class SettingsMainHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final SkytreePlugin plugin;
    private final NamespacedKey actionKey;

    public SettingsMainGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "settings_action");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsMainHolder(), 27,
                ComponentUtil.parse("§8» §6§lSettings Menu"));

        inv.setItem(11, createItem(Material.PLAYER_HEAD, "§e§lPlayer Settings", "PLAYER",
                "§7Configure your personal preferences",
                "§7like scoreboard, messages, etc."));
        inv.setItem(15, createItem(Material.GRASS_BLOCK, "§a§lIsland Settings", "ISLAND",
                "§7Configure your island rules",
                "§7like PvP, visitors, spawning, etc."));

        inv.setItem(18, createItem(Material.ARROW, "§cBack", "BACK", "§7Return to Main Menu"));

        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String action, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(lore));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsMainHolder)) {
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null)
            return;

        switch (action) {
            case "PLAYER":
                plugin.getPlayerSettingsGUI().open(player);
                break;
            case "ISLAND":
                if (plugin.getIslandService().hasIsland(player.getUniqueId())) {
                    plugin.getIslandSettingsGUI().open(player);
                } else {
                    player.sendMessage("§cYou don't have an island to configure!");
                }
                break;
            case "BACK":
                plugin.getMainMenuGUI().open(player);
                break;
        }
    }
}
