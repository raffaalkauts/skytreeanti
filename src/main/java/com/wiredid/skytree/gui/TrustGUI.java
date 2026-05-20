package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.TrustLevel;
import com.wiredid.skytree.util.GuiUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

/**
 * GUI for managing trusted players on an island
 */
public class TrustGUI implements Listener {

    private final SkytreePlugin plugin;
    private static final String GUI_TITLE = "§8Island Trusted Players";
    private static final org.bukkit.NamespacedKey PLAYER_UUID_KEY = new org.bukkit.NamespacedKey("skytree",
            "player_uuid");

    public TrustGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Island island) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE));

        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);

        Map<UUID, TrustLevel> trusted = island.getTrustedPlayers();
        int slot = 10;

        for (Map.Entry<UUID, TrustLevel> entry : trusted.entrySet()) {
            if (slot > 43)
                break; // Limit to one page for now
            if (slot % 9 == 0 || slot % 9 == 8)
                slot++;
            if (slot % 9 == 0 || slot % 9 == 8)
                slot++;

            inv.setItem(slot, createPlayerItem(entry.getKey(), entry.getValue()));
            slot++;
        }

        // Add trust someone button
        inv.setItem(49, GuiUtil.createItem(Material.GOLD_NUGGET, "§6§lTrust New Player",
                "§7Click to trust a new player!",
                "§eUsage: §f/trust <player> [level]"));

        player.openInventory(inv);
    }

    private ItemStack createPlayerItem(UUID uuid, TrustLevel level) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        meta.setOwningPlayer(offlinePlayer);
        meta.displayName(Component.text(offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown",
                NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        meta.lore(java.util.List.of(
                Component.text("§7Level: " + getLevelColor(level) + level.name()),
                Component.text(""),
                Component.text("§eLeft-Click §7to Cycle Level"),
                Component.text("§cRight-Click §7to Untrust")));

        meta.getPersistentDataContainer().set(PLAYER_UUID_KEY, PersistentDataType.STRING, uuid.toString());
        item.setItemMeta(meta);
        return item;
    }

    private String getLevelColor(TrustLevel level) {
        return switch (level) {
            case NONE -> "§7";
            case VISITOR -> "§f";
            case BUILDER -> "§a";
            case MODERATOR -> "§d";
            case CO_OWNER -> "§6";
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text(GUI_TITLE)))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        Island island = plugin.getIslandService().getIslandAtLocation(player.getLocation()).orElse(null);
        if (island == null || !island.getOwnerUUID().equals(player.getUniqueId()))
            return;

        if (item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            String uuidStr = meta.getPersistentDataContainer().get(PLAYER_UUID_KEY, PersistentDataType.STRING);
            if (uuidStr == null)
                return;

            UUID targetUuid = UUID.fromString(uuidStr);
            TrustLevel current = island.getTrustLevel(targetUuid);

            if (event.isLeftClick()) {
                // Cycle level
                TrustLevel next = cycleLevel(current);
                plugin.getIslandService().trustPlayer(island, targetUuid, next);
                player.sendMessage("§a§l[Skytree] §aUpdated " + Bukkit.getOfflinePlayer(targetUuid).getName() + " to §e"
                        + next.name());
                open(player, island); // Refresh
            } else if (event.isRightClick()) {
                // Untrust
                plugin.getIslandService().untrustPlayer(island, targetUuid);
                player.sendMessage(
                        "§c§l[Skytree] §cRemoved trust for " + Bukkit.getOfflinePlayer(targetUuid).getName());
                open(player, island); // Refresh
            }
        }
    }

    private TrustLevel cycleLevel(TrustLevel current) {
        TrustLevel[] values = TrustLevel.values();
        int nextIdx = (current.ordinal() + 1) % values.length;
        return values[nextIdx];
    }
}
