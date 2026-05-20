package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.PlayerData;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerSettingsGUI implements Listener {

    private static final class PlayerSettingsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final SkytreePlugin plugin;
    private final NamespacedKey settingKey;

    public PlayerSettingsGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.settingKey = new NamespacedKey(plugin, "setting_key");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new PlayerSettingsHolder(), 27,
                ComponentUtil.parse("§6§lPlayer §8» §7Settings"));

        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        Map<String, Boolean> settings = data.getSettings();

        inv.setItem(11, createSettingItem("Score HUD", "scoreboard", settings.getOrDefault("scoreboard", true),
                Material.PAINTING, "§7Toggle your on-screen score HUD."));
        inv.setItem(13, createSettingItem("Actionbar", "actionbar", settings.getOrDefault("actionbar", true),
                Material.NAME_TAG, "§7Toggle actionbar status messages."));
        inv.setItem(15, createSettingItem("Private Messages", "pms", settings.getOrDefault("pms", true),
                Material.PAPER, "§7Toggle receiving private messages."));
        inv.setItem(16, createSettingItem("Worth Display", "worth_display",
                settings.getOrDefault("worth_display", true), Material.GOLD_NUGGET,
                "§7Toggle worth in tooltip or lore."));

        inv.setItem(18, createButton(Material.ARROW, "§cBack", "BACK"));

        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    @EventHandler
    public void onSettingClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PlayerSettingsHolder)) {
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

        String key = clicked.getItemMeta().getPersistentDataContainer().get(settingKey, PersistentDataType.STRING);
        if (key == null) {
            return;
        }

        if ("BACK".equals(key)) {
            plugin.getSettingsMainGUI().open(player);
            return;
        }

        PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        boolean current = data.getSettings().getOrDefault(key, true);
        data.getSettings().put(key, !current);
        plugin.getPersistenceService().savePlayerData(data);

        if ("scoreboard".equals(key)) {
            if (!current) {
                plugin.getScoreboardSystem().setupScoreboard(player);
            } else {
                plugin.getScoreboardSystem().removeScoreboard(player);
            }
        }

        if ("worth_display".equals(key)) {
            plugin.getWorthService().updateInventoryLore(player);
            plugin.getWorthVisualSystem().refreshPlayer(player);
        }

        player.sendMessage(ComponentUtil.parse(
                "§aSetting '§e" + key + "§a' set to " + (current ? "§cDISABLED" : "§aENABLED")));
        open(player);
    }

    private ItemStack createButton(Material mat, String name, String key) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.getPersistentDataContainer().set(settingKey, PersistentDataType.STRING, key);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingItem(String name, String key, boolean enabled, Material mat, String description) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse((enabled ? "§a" : "§c") + "§l" + name));
        meta.getPersistentDataContainer().set(settingKey, PersistentDataType.STRING, key);

        List<String> lore = new ArrayList<>();
        lore.add(description);
        lore.add("");
        lore.add("§7Status: " + (enabled ? "§a§lENABLED" : "§c§lDISABLED"));
        lore.add("");
        lore.add("§eClick to toggle!");
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
