package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.ChatMessage;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatHistoryGUI implements Listener {

    private final SkytreePlugin plugin;

    public ChatHistoryGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§3§lChat History"));
        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        List<ChatMessage> history = new ArrayList<>(plugin.getChatService().getHistory());
        Collections.reverse(history); // Latest first

        int slot = 10;
        for (ChatMessage msg : history) {
            if (slot > 43)
                break;
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot += 2; // Skip borders
            }
            if (slot > 43)
                break;

            inv.setItem(slot++, createMessageItem(msg));
        }

        player.openInventory(inv);
    }

    private ItemStack createMessageItem(ChatMessage msg) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§b§l" + msg.senderName()));

        List<Component> lore = new ArrayList<>();
        lore.add(msg.message());
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§7Time: §f" + java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(java.time.Instant.ofEpochMilli(msg.timestamp()).atZone(java.time.ZoneId.systemDefault()))));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§eLeft-Click §7to react with ❤️"));
        lore.add(ComponentUtil.smartParse("§eRight-Click §7to react with 👍"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Chat History"))
            return;

        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        String senderName = ComponentUtil.toLegacy(clicked.getItemMeta().displayName()).substring(4); // Remove prefix

        if (event.isLeftClick()) {
            player.sendMessage("§c§l[Skytree] §fYou sent a ❤️ to §b" + senderName);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        } else if (event.isRightClick()) {
            player.sendMessage("§a§l[Skytree] §fYou sent a 👍 to §b" + senderName);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }
}
