package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandQuestService;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.impl.SkytreeIslandQuestService;
import com.wiredid.skytree.model.Island;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IslandQuestGUI implements InventoryHolder, Listener {

    private final SkytreePlugin plugin;
    private final IslandService islandService;
    private final IslandQuestService questService;

    public IslandQuestGUI(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.islandService = plugin.getIslandService();
        this.questService = plugin.getIslandQuestService();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(this, 36, Component.text("Island Daily Quests"));
    }

    public void open(Player player) {
        // Or member check? For now owner/member check logic similar to service.
        Island island = null;
        for (Island is : islandService.getLoadedIslands()) {
            if (is.isMember(player.getUniqueId())) {
                island = is;
                break;
            }
        }

        if (island == null) {
            player.sendMessage(
                    Component.text("You must be part of an island to view island quests!", NamedTextColor.RED));
            return;
        }

        Inventory inv = getInventory();

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE);

        // Quests are at slots 11, 13, 15
        int[] slots = { 11, 13, 15 };

        // Sort keys to be deterministic if possible, but map is unordered usually.
        // We'll just iterate. 3 slots max.

        int slotIdx = 0;
        SkytreeIslandQuestService impl = (SkytreeIslandQuestService) questService; // Cast to access templates

        for (Map.Entry<String, Integer> entry : island.getActiveDailyQuests().entrySet()) {
            if (slotIdx >= slots.length)
                break;

            String qId = entry.getKey();
            int progress = entry.getValue();
            SkytreeIslandQuestService.QuestTemplate template = impl.getTemplate(qId);

            if (template == null)
                continue;

            boolean completed = island.getCompletedDailyQuests().contains(qId);
            int req = template.getRequirement(template.getTier() == null ? "bronze" : template.getTier()); // Fallback

            ItemStack icon;
            Material mat = Material.PAPER;
            if (completed)
                mat = Material.FILLED_MAP;
            else if (progress >= req)
                mat = Material.BOOK; // Ready to claim

            icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(
                    Component.text(template.getName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component
                    .text(template.getDescription().replace("%amount%", String.valueOf(req)), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            // Progress Bar
            lore.add(Component.text("Progress: ", NamedTextColor.WHITE)
                    .append(Component.text(Math.min(progress, req) + "/" + req, NamedTextColor.YELLOW)));

            float pct = (float) progress / req;
            StringBuilder bar = new StringBuilder();
            int bars = 20;
            int filled = (int) (pct * bars);
            // Cap filled at bars
            filled = Math.min(filled, bars);

            bar.append("§8[");
            bar.append("§a").append("|".repeat(filled));
            bar.append("§7").append("|".repeat(bars - filled));
            bar.append("§8]");
            lore.add(Component.text(bar.toString()));

            lore.add(Component.empty());
            lore.add(Component.text("Rewards:", NamedTextColor.GOLD));
            int usdt = template.getRewardUSDT(template.getTier());
            if (usdt > 0)
                lore.add(Component.text("• " + usdt + " USDT", NamedTextColor.GREEN));
            int shards = template.getRewardShards(template.getTier());
            if (shards > 0)
                lore.add(Component.text("• " + shards + " Shards", NamedTextColor.AQUA));
            lore.add(Component.text("• 10 Quest Points", NamedTextColor.LIGHT_PURPLE));

            lore.add(Component.empty());
            if (completed) {
                lore.add(Component.text("✔ COMPLETED", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else if (progress >= req) {
                lore.add(Component.text("! CLICK TO CLAIM !", NamedTextColor.GOLD, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("In Progress...", NamedTextColor.YELLOW));
            }

            meta.lore(lore);
            // Store quest ID in PersistentDataContainer if needed, or identify by
            // slot/name.
            // Simplest is to assume mapping by index if strict, but order might change?
            // Actually, activeQuests order in Map is not guaranteed.
            // I should store quest ID in localized name or PDC.
            // PDC is safest.
            // But for now, simple NBT/PDC wrapper is good.
            // I'll stick to legacy NBT if no PDC handy, or just helper.
            // Wait, Paper API components... I can use PDC.
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "quest_id");
            pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, qId);

            icon.setItemMeta(meta);
            inv.setItem(slots[slotIdx], icon);
            slotIdx++;
        }

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.displayName(Component.text("Close", NamedTextColor.RED));
        close.setItemMeta(closeMeta);
        inv.setItem(23, close);

        // Reset Info
        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Resets in: ", NamedTextColor.GOLD));
        // Calculate time to midnight
        Calendar now = Calendar.getInstance();
        long midnight = now.getTimeInMillis();
        now.add(Calendar.DAY_OF_YEAR, 1);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        long diff = now.getTimeInMillis() - midnight;
        long hours = diff / (1000 * 60 * 60);
        long mins = (diff / (1000 * 60)) % 60;

        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text(hours + "h " + mins + "m", NamedTextColor.YELLOW));
        infoMeta.lore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(21, info);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null)
            return;

        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "quest_id");
        if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            String qId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);

            // Try claim
            if (questService.claimReward(player.getUniqueId(), qId)) {
                player.sendMessage(Component.text("Rewards claimed!", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                open(player); // Refresh
            } else {
                // Determine why fail (not complete or already claimed)
                // Service returns false for both.
                // Assuming GUI visual handled "Already completed", so effectively "Not complete
                // yet"
                // Or "Already claimed" if clicked again quickly.
                // Silent usually.
            }
        }
    }
}
