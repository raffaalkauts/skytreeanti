package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.BaitService;
import com.wiredid.skytree.model.BaitType;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
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

public class BaitGUI implements Listener {

    private final SkytreePlugin plugin;
    private final BaitService baitService;

    public BaitGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.baitService = plugin.getBaitService();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lBait Bag §8» §7Fishing"));

        // Premium Border
        GuiUtil.applyPremiumBorder(inv, Material.BROWN_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);

        int[] slots = { 10, 12, 14, 16 };
        BaitType[] types = BaitType.values();

        for (int i = 0; i < types.length; i++) {
            inv.setItem(slots[i], createIcon(player, types[i]));
        }

        // Active Status
        inv.setItem(13, createActiveInfo(player));

        player.openInventory(inv);
    }

    private ItemStack createIcon(Player player, BaitType type) {
        ItemStack item = ((com.wiredid.skytree.impl.SkytreeBaitService) baitService).createBaitItem(type);
        ItemMeta meta = item.getItemMeta();

        List<net.kyori.adventure.text.Component> lore = meta.lore();
        if (lore == null)
            lore = new ArrayList<>();

        int count = baitService.getBaitCount(player.getUniqueId(), type);
        lore.add(ComponentUtil.parse(" "));
        lore.add(ComponentUtil.parse("§7You have: §e" + count + "x"));
        lore.add(ComponentUtil.parse(count > 0 ? "§aClick to equip!" : "§cNo bait owned"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActiveInfo(Player player) {
        var active = baitService.getActiveBait(player.getUniqueId());
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§e§lCurrently Equipped"));

        List<String> lore = new ArrayList<>();
        if (active != null) {
            lore.add("§7Type: §6" + active.getType().name());
            lore.add("§7Remaining: §e" + active.getQuantity() + "x");
            lore.add("");
            lore.add("§cClick to unequip");
        } else {
            lore.add("§7None");
        }

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Bait Bag"))
            return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (clicked.getType() == Material.FISHING_ROD) {
            baitService.removeBait(player.getUniqueId());
            player.sendMessage("§aUnequipped active bait.");
            open(player);
            return;
        }

        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "bait_type");
        if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(key,
                org.bukkit.persistence.PersistentDataType.STRING)) {
            String typeStr = clicked.getItemMeta().getPersistentDataContainer().get(key,
                    org.bukkit.persistence.PersistentDataType.STRING);
            BaitType type = BaitType.fromString(typeStr);
            if (type == null)
                return;

            int count = baitService.getBaitCount(player.getUniqueId(), type);
            if (count <= 0) {
                player.sendMessage("§cYou don't have any §e" + type.name().toLowerCase() + " §cbait!");
                return;
            }

            // Remove items from inventory
            removeItems(player, type, count);

            // Equip in service
            baitService.equipBait(player.getUniqueId(), type, count);
            player.sendMessage("§aEquipped §e" + count + "x " + type.name().toLowerCase() + " §abait.");
            open(player);
        }
    }

    private void removeItems(Player player, BaitType type, int amount) {
        int left = amount;
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "bait_type");
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta())
                continue;
            String storedType = item.getItemMeta().getPersistentDataContainer().get(key,
                    org.bukkit.persistence.PersistentDataType.STRING);
            if (type.name().equalsIgnoreCase(storedType)) {
                int stack = item.getAmount();
                if (stack <= left) {
                    left -= stack;
                    item.setAmount(0);
                } else {
                    item.setAmount(stack - left);
                    break;
                }
            }
        }
    }
}
