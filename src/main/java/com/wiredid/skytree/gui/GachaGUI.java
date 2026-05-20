package com.wiredid.skytree.gui;
import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.GachaService;
import com.wiredid.skytree.impl.MythicItemManager.GachaCrateDef;
import com.wiredid.skytree.impl.MythicItemManager.GachaGlobalConfig;
import com.wiredid.skytree.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GachaGUI implements InventoryHolder {

    private final SkytreePlugin plugin;
    private final Inventory inventory;
    private final Player player;
    public GachaGUI(SkytreePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27,
                com.wiredid.skytree.util.ComponentUtil.parse("§6§lGacha §8» §7Rewards"));
        initializeItems();
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inventory, Material.CYAN_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE);
    }

    private void initializeItems() {
        GachaService gachaService = plugin.getGachaService();
        GachaGlobalConfig config = plugin.getMythicItemManager().getGachaConfig();

        if (config == null || config.crate_types == null) {
            inventory.setItem(13,
                    createGuiItem(Material.BARRIER, Component.text("Gacha Config Error", NamedTextColor.RED)));
            return;
        }

        int[] slots = { 11, 15 }; // Two main slots for now
        int i = 0;

        for (Map.Entry<String, GachaCrateDef> entry : config.crate_types.entrySet()) {
            if (i >= slots.length)
                break;

            String key = entry.getKey();
            GachaCrateDef def = entry.getValue();

            Material mat = Material.ENDER_CHEST;
            if (key.equalsIgnoreCase("divine"))
                mat = Material.TRAPPED_CHEST;

            inventory.setItem(slots[i], createGuiItem(mat,
                    Component.text(formatName(key) + " Crate", NamedTextColor.GOLD),
                    Component.text("Cost: " + NumberUtil.formatCurrency(def.priceBTC), NamedTextColor.YELLOW),
                    Component.text("Left-Click to Spin!", NamedTextColor.GRAY)));
            i++;
        }

        // Pity Indicator
        int pity = gachaService.getPity(player);
        inventory.setItem(22, createGuiItem(Material.AMETHYST_SHARD,
                Component.text("Pity Counter", NamedTextColor.LIGHT_PURPLE),
                Component.text("Current Pity: " + pity + "/" + config.pity_threshold, NamedTextColor.WHITE),
                Component.text("Guaranteed Legendary at " + config.pity_threshold, NamedTextColor.GRAY)));

        // Close Button
        inventory.setItem(26, createGuiItem(Material.BARRIER, Component.text("Close", NamedTextColor.RED)));
    }

    private ItemStack createGuiItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        List<Component> loreList = new ArrayList<>();
        for (Component c : lore) {
            loreList.add(c);
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private String formatName(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
