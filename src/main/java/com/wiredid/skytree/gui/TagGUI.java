package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.TagService;
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
import java.util.List;
import java.util.Map;

public class TagGUI implements Listener {

    private final SkytreePlugin plugin;
    private final TagService tagService;

    public TagGUI(SkytreePlugin plugin, TagService tagService) {
        this.plugin = plugin;
        this.tagService = tagService;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§6§lSelect Your Tag"));
        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);

        Map<String, TagService.TagData> allTags = tagService.getAvailableTags();
        String activeTag = tagService.getActiveTagDisplay(player);

        int slot = 10;
        for (TagService.TagData tag : allTags.values()) {
            if (slot == 17 || slot == 26 || slot == 35)
                slot += 2;
            if (slot >= 40) // Stop before reset button
                break;

            ItemStack item = createTagItem(player, tag, activeTag);
            inv.setItem(slot++, item);
        }

        // Close/Reset button (Row 4 Interior Center)
        ItemStack reset = GuiUtil.createItem(Material.BARRIER, "§c§lRemove Tag", "§7Click to clear your active tag.");
        inv.setItem(40, reset);

        player.openInventory(inv);
    }

    private ItemStack createTagItem(Player player, TagService.TagData tag, String activeTag) {
        boolean hasPerm = tagService.hasPermission(player, tag.getId());
        boolean isActive = activeTag != null && activeTag.equals(tag.getDisplay());

        Material mat = isActive ? Material.ENCHANTED_BOOK : (hasPerm ? Material.BOOK : Material.PAPER);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§eTag: " + tag.getDisplay()));

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7ID: §f" + tag.getId()));
        lore.add(Component.empty());

        if (isActive) {
            lore.add(ComponentUtil.smartParse("§a§lACTIVE"));
        } else if (hasPerm) {
            lore.add(ComponentUtil.smartParse("§aUnlocked"));
            lore.add(ComponentUtil.smartParse("§eClick to select!"));
        } else {
            if (tag.getCost() < 0) {
                lore.add(ComponentUtil.smartParse("§cExclusive Tag"));
                lore.add(ComponentUtil.smartParse("§7Unlock via ranks or events."));
            } else {
                lore.add(ComponentUtil.smartParse("§7Cost: §f₮ " + plugin.getEconomyService().format(tag.getCost())));
                lore.add(ComponentUtil.smartParse("§eClick to purchase!"));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Select Your Tag"))
            return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (clicked.getType() == Material.BARRIER) {
            tagService.removeActiveTag(player);
            player.sendMessage("§aYour active tag has been removed.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            open(player);
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (!meta.hasLore())
            return;

        // Find ID from lore
        String idLore = ComponentUtil.toLegacy(meta.lore().get(0));
        String id = idLore.substring(idLore.indexOf("§f") + 2).trim();

        TagService.TagData tag = tagService.getTag(id);
        if (tag == null)
            return;

        if (tagService.hasPermission(player, id)) {
            tagService.setActiveTag(player, id);
            player.sendMessage("§aYou have selected the " + tag.getDisplay() + " §atag!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
            player.closeInventory();
        } else {
            if (tag.getCost() >= 0) {
                double balance = plugin.getEconomyService().getBalance(player.getUniqueId());
                if (balance >= tag.getCost()) {
                    if (plugin.getEconomyService().removeBalance(player.getUniqueId(), tag.getCost())) {
                        // Grant permission (In a real server, we use Vault/LuckPerms)
                        // For this plugin, we'll assume a simplified permission grant or
                        // just let them select it if they paid.
                        // Better: We should have a way to persist "Unlocked Tags" in PlayerData.
                        // I'll add 'unlockedTags' to PlayerData.

                        plugin.getRankService().grantPermission(player.getUniqueId(), tag.getPermission());
                        player.sendMessage("§aYou purchased the " + tag.getDisplay() + " §atag!");
                        tagService.setActiveTag(player, id);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
                        player.closeInventory();
                    }
                } else {
                    player.sendMessage("§cYou cannot afford this tag!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            } else {
                player.sendMessage("§cThis tag is exclusive and cannot be purchased.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }
}
