package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.IslandMember;
import com.wiredid.skytree.model.IslandRole;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import java.util.List;

/**
 * GUI for managing island member permissions
 */
public class PermissionsGUI implements Listener {

    private final SkytreePlugin plugin;
    private static final String TITLE = "§6§lIsland §8» §7Permissions";

    public PermissionsGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Island island) {
        Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse(TITLE));

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.CYAN_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE);

        // Owner info
        gui.setItem(4, createMemberItem(island.getOwnerUUID(), IslandRole.OWNER));

        // Members
        int slot = 10;
        for (IslandMember member : island.getMembers()) {
            if (slot >= 44)
                break;
            gui.setItem(slot, createMemberItem(member.getUuid(), member.getRole()));
            slot++;
        }

        // Permission options info
        gui.setItem(48, createInfo(Material.BOOK, "§ePermission Levels",
                "§7§lOWNER: §fFull control",
                "§7§lCO-OWNER: §fCan manage members",
                "§7§lMEMBER: §fCan build",
                "§7§lCOOP: §fTemporary access",
                "§7§lVISITOR: §fRead-only"));

        gui.setItem(50, createInfo(Material.EMERALD, "§aInvite Players",
                "§7Use §e/is invite <player>",
                "§7to add new members"));

        // Trust Level Link
        ItemStack trustLink = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta trustMeta = trustLink.getItemMeta();
        trustMeta.displayName(ComponentUtil.parse("§6§lGranular Trust Levels"));
        trustMeta.lore(ComponentUtil.parseList(List.of(
                "§7Manage non-member access",
                "§7and granular permissions.",
                "",
                "§eClick to manage Trust")));
        trustLink.setItemMeta(trustMeta);
        gui.setItem(46, trustLink);

        player.openInventory(gui);
    }

    private ItemStack createMemberItem(java.util.UUID uuid, IslandRole role) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        String name = p.getName() != null ? p.getName() : "Unknown";

        Material mat = switch (role) {
            case OWNER -> Material.DIAMOND;
            case CO_OWNER -> Material.GOLD_INGOT;
            case MEMBER -> Material.IRON_INGOT;
            case COOP -> Material.EMERALD;
            default -> Material.STONE;
        };

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§f" + name));
        meta.lore(ComponentUtil.parseList(
                "§7Role: §f" + role.name(),
                "",
                role == IslandRole.OWNER ? "§6Island Owner" : "§7Click to change role"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfo(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ComponentUtil.toLegacy(event.getView().title()).equals(TITLE))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        if (item.getType() == Material.GOLD_BLOCK) {
            plugin.getIslandService().getIsland(player.getUniqueId())
                    .ifPresent(island -> plugin.getTrustGUI().open(player, island));
        } else if (item.getType() == Material.DIAMOND || item.getType() == Material.GOLD_INGOT
                || item.getType() == Material.IRON_INGOT || item.getType() == Material.EMERALD) {
            // Role cycling could be implemented here
            player.sendMessage("§e§l[Skytree] §7Use §e/is team promote/demote §7for roles.");
        }
    }
}
