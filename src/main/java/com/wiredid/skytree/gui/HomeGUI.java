package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

public class HomeGUI implements Listener {

    private final SkytreePlugin plugin;
    private final NamespacedKey homeNameKey;
    private final NamespacedKey homeOwnerKey;

    public HomeGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.homeNameKey = new NamespacedKey(plugin, "home_name");
        this.homeOwnerKey = new NamespacedKey(plugin, "home_owner");
    }

    public void open(Player player) {
        open(player, player.getUniqueId(), player.getName());
    }

    public void open(Player viewer, UUID targetUuid, String targetName) {
        boolean personalView = viewer.getUniqueId().equals(targetUuid);
        String titleSuffix = personalView ? "\u00A77Personal" : "\u00A77" + targetName;
        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.parse("\u00A76\u00A7lHomes \u00A78\u00BB " + titleSuffix));

        PlayerData data = plugin.getPersistenceService().loadPlayerData(targetUuid);
        Map<String, Location> homes = data.getHomes();

        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        int i = 0;

        for (Map.Entry<String, Location> entry : homes.entrySet()) {
            if (i >= slots.length)
                break;
            inv.setItem(slots[i++], createHomeIcon(entry.getKey(), entry.getValue(), targetUuid, targetName, personalView));
        }

        if (homes.isEmpty()) {
            ItemStack info = new ItemStack(Material.PAPER);
            ItemMeta meta = info.getItemMeta();
            meta.displayName(ComponentUtil.parse("\u00A7cNo Homes Set"));
            if (personalView) {
                meta.lore(ComponentUtil.parseList("\u00A77Use \u00A7e/sethome [name] \u00A77to create one!"));
            } else {
                meta.lore(ComponentUtil.parseList("\u00A77" + targetName + " has no saved homes."));
            }
            info.setItemMeta(meta);
            inv.setItem(13, info);
        }

        GuiUtil.applyPremiumBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE);

        viewer.openInventory(inv);
    }

    private ItemStack createHomeIcon(String name, Location loc, UUID targetUuid, String targetName, boolean personalView) {
        ItemStack item = new ItemStack(Material.CYAN_BED);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("\u00A7b\u00A7lHome: \u00A73" + name));
        if (personalView) {
            meta.lore(ComponentUtil.parseList(
                    "\u00A77Owner: \u00A7f" + targetName,
                    "\u00A77World: \u00A7f" + loc.getWorld().getName(),
                    "\u00A77Location: \u00A7f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                    "",
                    "\u00A7aLeft-Click to teleport",
                    "\u00A7cRight-Click to delete"));
        } else {
            meta.lore(ComponentUtil.parseList(
                    "\u00A77Owner: \u00A7f" + targetName,
                    "\u00A77World: \u00A7f" + loc.getWorld().getName(),
                    "\u00A77Location: \u00A7f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                    "",
                    "\u00A7aLeft-Click to teleport"));
        }

        meta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, name);
        meta.getPersistentDataContainer().set(homeOwnerKey, PersistentDataType.STRING, targetUuid.toString());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onHomeClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Homes"))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE
                || clicked.getType() == Material.WHITE_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.PAPER)
            return;
        if (clicked.getItemMeta() == null)
            return;

        String name = clicked.getItemMeta().getPersistentDataContainer().get(homeNameKey, PersistentDataType.STRING);
        String ownerValue = clicked.getItemMeta().getPersistentDataContainer().get(homeOwnerKey, PersistentDataType.STRING);
        UUID ownerUuid = null;
        if (ownerValue != null) {
            try {
                ownerUuid = UUID.fromString(ownerValue);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (name == null) {
            name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName()).replace("\u00A7b\u00A7lHome: \u00A73", "");
        }

        if (event.isLeftClick()) {
            UUID targetUuid = ownerUuid != null ? ownerUuid : player.getUniqueId();
            PlayerData data = plugin.getPersistenceService().loadPlayerData(targetUuid);
            Location home = data.getHome(name);
            player.closeInventory();
            if (home == null) {
                player.sendMessage(ComponentUtil.parse("\u00A7cHome '\u00A7e" + name + "\u00A7c' not found!"));
                return;
            }
            player.teleport(home);
            player.sendMessage(ComponentUtil.parse("\u00A7aTeleported to '\u00A7e" + name + "\u00A7a'!"));
        } else if (event.isRightClick() && ownerUuid != null && player.getUniqueId().equals(ownerUuid)) {
            player.closeInventory();
            player.performCommand("delhome " + name);
        }
    }
}
