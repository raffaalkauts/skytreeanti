package com.wiredid.skytree.command;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TrashCommand implements CommandExecutor, TabCompleter, Listener {

    private final SkytreePlugin plugin;

    public TrashCommand(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (args.length == 0) {
            org.bukkit.inventory.Inventory trashInv = org.bukkit.Bukkit.createInventory(null, 27,
                    com.wiredid.skytree.util.ComponentUtil.smartParse("§c§lTrash Disposal"));
            for (int i = 0; i < 27; i++) {
                trashInv.setItem(i, new ItemStack(org.bukkit.Material.RED_STAINED_GLASS_PANE));
            }
            ItemStack infoItem = new ItemStack(org.bukkit.Material.BARRIER);
            org.bukkit.inventory.meta.ItemMeta meta = infoItem.getItemMeta();
            meta.displayName(com.wiredid.skytree.util.ComponentUtil.smartParse("§c§lTrash Can"));
            meta.lore(java.util.Arrays.asList(
                    com.wiredid.skytree.util.ComponentUtil.smartParse("§7Put items here to delete them"),
                    com.wiredid.skytree.util.ComponentUtil.smartParse("§7Items are permanently removed!")));
            infoItem.setItemMeta(meta);
            trashInv.setItem(13, infoItem);
            player.openInventory(trashInv);
            return true;
        }

        com.wiredid.skytree.model.PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        java.util.Set<String> filter = data.getTrashFilter();

        switch (args[0].toLowerCase()) {
            case "add":
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage("§cHold an item to add to trash filter.");
                } else {
                    filter.add(hand.getType().name());
                    plugin.getPersistenceService().savePlayerData(data);
                    player.sendMessage("§aAdded §e" + hand.getType().name() + " §ato trash filter.");
                }
                break;
            case "remove":
                ItemStack handRem = player.getInventory().getItemInMainHand();
                if (filter.remove(handRem.getType().name())) {
                    plugin.getPersistenceService().savePlayerData(data);
                    player.sendMessage("§aRemoved §e" + handRem.getType().name() + " §efrom trash filter.");
                } else {
                    player.sendMessage("§cItem not in filter.");
                }
                break;
            case "list":
                player.sendMessage("§6Trash Filter: §7" + filter.toString());
                break;
            case "clear":
                filter.clear();
                plugin.getPersistenceService().savePlayerData(data);
                player.sendMessage("§aTrash filter cleared.");
                break;
            default:
                sender.sendMessage("§cUsage: /trash <add|remove|list|clear>");
        }
        return true;
    }

    private boolean isTrashGUI(org.bukkit.inventory.InventoryView view) {
        return view != null && ComponentUtil.toLegacy(view.title()).equals("§c§lTrash Disposal");
    }

    @EventHandler
    public void onTrashClick(InventoryClickEvent event) {
        if (!isTrashGUI(event.getView())) return;
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTrashDrag(InventoryDragEvent event) {
        if (isTrashGUI(event.getView())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    if (event.getView().getTopInventory().getItem(slot) != null
                            && event.getView().getTopInventory().getItem(slot).getType() == Material.RED_STAINED_GLASS_PANE) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTrashClose(InventoryCloseEvent event) {
        if (!isTrashGUI(event.getView())) return;
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.RED_STAINED_GLASS_PANE) {
                item.setAmount(0);
            }
        }
    }

    public boolean isBlacklisted(Player player, Material material) {
        com.wiredid.skytree.model.PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
        return data.getTrashFilter().contains(material.name());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove", "list", "clear").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
