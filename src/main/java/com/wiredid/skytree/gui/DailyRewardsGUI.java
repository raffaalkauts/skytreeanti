package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.system.DailyRewardsSystem;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class DailyRewardsGUI implements Listener {

    private final DailyRewardsSystem dailySystem;
    private final NamespacedKey DAY_KEY;
    private final SkytreePlugin plugin;

    public DailyRewardsGUI(SkytreePlugin plugin, DailyRewardsSystem dailySystem) {
        this.plugin = plugin;
        this.dailySystem = dailySystem;
        this.DAY_KEY = new NamespacedKey(plugin, "daily_day");
    }

    private int getMaxDays() {
        return plugin.getConfig().getInt("daily_rewards.max_streak_days", 7);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, ComponentUtil.smartParse("§8Daily Rewards"));
        GuiUtil.applyPremiumBorder(inv, Material.BLACK_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);

        int currentStreak = dailySystem.getStreak(player);
        boolean claimedToday = !dailySystem.canClaim(player);

        // Handling weekly reset visually
        // If they missed a day, streak reset logic happens on CLAIM attempt, so visuals
        // might look old until they claim.
        // Actually, let's pre-check reset logic just for visuals?
        // No, keep it consistent with Logic.

        // But if they claimed today, text should say "Claimed".

        int maxDays = getMaxDays();
        for (int day = 1; day <= maxDays; day++) {
            int slot;
            if (day <= 7) {
                slot = 18 + day;
            } else if (day <= 14) {
                slot = 27 + (day - 7);
            } else {
                slot = 36 + (day - 14);
            }
            ConfigurationSection reward = dailySystem.getConfig().getConfigurationSection("rewards." + day);
            if (reward == null)
                continue;

            String iconName = reward.getString("icon", "CHEST").toUpperCase();
            Material mat = Material.matchMaterial(iconName);
            if (mat == null)
                mat = Material.CHEST;

            String name = reward.getString("name");

            // Visual Status
            // Visual Status
            // Logic fix:
            // If streak is 0, next is 1.
            // If streak is 2 and claimed today, we are at day 2 (done). Next is 3
            // (tomorrow).
            // If streak is 2 and NOT claimed today, we are at day 3? No, we are at day 3 to
            // be claimed?
            // Wait: Logic is: Streak increases AFTER claim.
            // So if streak is 2, it means I finished 2 days. Today I am claiming Day 3.
            // So current target is streak + 1.

            // Correction: If claimed today, I am waiting for tomorrow (streak+1).
            // If NOT claimed today, I am waiting to claim (streak+1).
            // Actually, if I claimed Day 2 today, my streak becomes 2. Next claim is Day 3
            // (tomorrow).

            boolean isPast = day <= currentStreak;
            boolean isTarget = day == currentStreak + 1;

            String status;
            if (isPast) {
                status = "§a§lCLAIMED";
                mat = Material.MINECART; // Already claimed icon
            } else if (isTarget && !claimedToday) {
                status = "§e§lCLICK TO CLAIM";
                mat = Material.CHEST_MINECART; // Ready
            } else if (isTarget && claimedToday) {
                status = "§7Come back tomorrow";
                mat = Material.MINECART;
            } else {
                status = "§7LOCKED";
                mat = Material.MINECART; // Locked
            }

            if (day == maxDays)
                mat = Material.ENDER_CHEST; // Special for final day

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(ComponentUtil.smartParse("§eDay " + day));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(ComponentUtil.smartParse("§7Reward: " + name));
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ComponentUtil.smartParse(status));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(DAY_KEY, PersistentDataType.INTEGER, day);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
        }

        // Info Item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(ComponentUtil.smartParse("§bInformation"));
        List<net.kyori.adventure.text.Component> iLore = new ArrayList<>();
        iLore.add(ComponentUtil.smartParse("§7Current Streak: §e" + currentStreak + " Days"));
        iLore.add(ComponentUtil.smartParse("§7Claim daily to keep your streak!"));
        iLore.add(ComponentUtil.smartParse("§7Day " + getMaxDays() + " gives a §dSpecial Reward§7!"));
        iMeta.lore(iLore);
        info.setItemMeta(iMeta);
        inv.setItem(31, info);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ComponentUtil.smartParse("§8Daily Rewards")))
            return;
        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta())
            return;

        if (clicked.getItemMeta().getPersistentDataContainer().has(DAY_KEY, PersistentDataType.INTEGER)) {
            int day = clicked.getItemMeta().getPersistentDataContainer().get(DAY_KEY, PersistentDataType.INTEGER);
            int currentStreak = dailySystem.getStreak(player);

            // Logic: Can only claim next day
            // If claimed today, cannot claim.
            // If not claimed, target is streak + 1.

            if (!dailySystem.canClaim(player)) {
                player.sendMessage("§cCome back tomorrow for your next reward!");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            int target = currentStreak + 1;
            int maxDays = getMaxDays();
            if (target > maxDays)
                target = 1;

            if (day == target) {
                dailySystem.claimReward(player);
                player.closeInventory();
                // Optionally reopen to show update
            } else {
                player.sendMessage("§cYou can only claim Day " + target + "!");
            }
        }
    }
}
