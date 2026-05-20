package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.BountyService;
import com.wiredid.skytree.model.Bounty;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BountyGUI implements Listener {

    private final SkytreePlugin plugin;
    private final BountyService bountyService;

    public BountyGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.bountyService = plugin.getBountyService();
    }

    public void open(Player player) {
        openPage(player, 1);
    }

    // Sort logic enum? For now let's just sort by highest value.

    private void openPage(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ComponentUtil.parse("§6§lBounty §8» §7Wanted §8(" + page + ")"));

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE);

        List<Bounty> allBounties = bountyService.getAllBounties();
        allBounties.sort(Comparator.comparingDouble(Bounty::getAmount).reversed());

        // Paginator logic (Interior slots only)
        java.util.List<Integer> interiorSlots = com.wiredid.skytree.util.GuiUtil.getInteriorSlots(54);
        int itemsPerPage = interiorSlots.size(); // 28 slots
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allBounties.size());

        int listIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Bounty bounty = allBounties.get(i);
            inv.setItem(interiorSlots.get(listIndex++), createBountyItem(bounty));
        }

        // Navigation Buttons
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.displayName(ComponentUtil.parse("§ePrevious Page"));
            prev.setItemMeta(meta);
            inv.setItem(39, prev);
        }

        if (endIndex < allBounties.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.displayName(ComponentUtil.parse("§eNext Page"));
            next.setItemMeta(meta);
            inv.setItem(41, next);
        }

        // Close Button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        meta.displayName(ComponentUtil.parse("§cClose"));
        close.setItemMeta(meta);
        inv.setItem(40, close);

        player.setMetadata("bounty_page", new FixedMetadataValue(plugin, page));
        player.openInventory(inv);
    }

    private ItemStack createBountyItem(Bounty bounty) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        OfflinePlayer target = Bukkit.getOfflinePlayer(bounty.getTarget());
        meta.setOwningPlayer(target);
        meta.displayName(ComponentUtil.parse("§c§l" + (target.getName() != null ? target.getName() : "Unknown")));

        List<String> lore = new ArrayList<>();
        lore.add("§7Reward: §a$" + NumberUtil.formatCurrency(bounty.getAmount()));
        lore.add("§7Issuer: §e" + Bukkit.getOfflinePlayer(bounty.getIssuer()).getName());
        lore.add("§7Active for: §f" + formatDuration(System.currentTimeMillis() - bounty.getTimestamp()));
        lore.add("");
        lore.add("§eKill this player to claim!");

        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String formatDuration(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        if (hours > 0)
            return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (!title.contains("Bounty"))
            return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (!player.hasMetadata("bounty_page"))
            return;
        int currentPage = player.getMetadata("bounty_page").get(0).asInt();

        if (clicked.getType() == Material.ARROW) {
            String name = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());
            if (name.contains("Next")) {
                openPage(player, currentPage + 1);
            } else if (name.contains("Previous")) {
                openPage(player, currentPage - 1);
            }
        } else if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        String title = ComponentUtil.toLegacy(event.getView().title());
        if (title.contains("Bounty")) {
            event.getPlayer().removeMetadata("bounty_page", plugin);
        }
    }
}
