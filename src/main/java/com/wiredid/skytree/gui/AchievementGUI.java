package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.system.AchievementSystem;
import com.wiredid.skytree.system.AchievementSystem.Achievement;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for viewing achievements
 */
public class AchievementGUI {

    private final AchievementSystem achievementSystem;

    public AchievementGUI(SkytreePlugin plugin, AchievementSystem achievementSystem) {

        this.achievementSystem = achievementSystem;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.parse("§6§lAchievements §8» §7Progress"));

        // Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.BLACK_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE);

        java.util.List<Integer> interiorSlots = com.wiredid.skytree.util.GuiUtil.getInteriorSlots(54);
        int listIndex = 0;
        for (Achievement achievement : Achievement.values()) {
            if (listIndex >= interiorSlots.size())
                break;

            boolean unlocked = achievementSystem.hasAchievement(player.getUniqueId(), achievement);
            gui.setItem(interiorSlots.get(listIndex++), createAchievementItem(achievement, unlocked));
        }

        // Stats item (Middle of row 4)
        gui.setItem(40, createStatsItem(player));

        player.openInventory(gui);
    }

    private ItemStack createAchievementItem(Achievement achievement, boolean unlocked) {
        Material material = unlocked ? Material.DIAMOND : Material.COAL;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (unlocked) {
            meta.displayName(ComponentUtil.parse("§a§l✓ " + achievement.getName()));
        } else {
            meta.displayName(ComponentUtil.parse("§7" + achievement.getName()));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.parse("§7" + achievement.getDescription()));
        lore.add(ComponentUtil.parse(""));
        lore.add(ComponentUtil.parse("§6Reward: §e" + achievement.getReward() + " \u20AE"));
        lore.add(ComponentUtil.parse(""));

        if (unlocked) {
            lore.add(ComponentUtil.parse("§a§l✓ UNLOCKED"));
        } else {
            lore.add(ComponentUtil.parse("§c§l✗ LOCKED"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse("§e§lYour Progress"));

        int count = achievementSystem.getAchievementCount(player.getUniqueId());
        int total = Achievement.values().length;
        double percentage = achievementSystem.getCompletionPercentage(player.getUniqueId());

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.parse("§7Achievements: §e" + count + "/" + total));
        lore.add(ComponentUtil.parse("§7Completion: §e" + String.format("%.1f", percentage) + "%"));
        lore.add(ComponentUtil.parse(""));
        lore.add(ComponentUtil.parse("§aKeep unlocking achievements"));
        lore.add(ComponentUtil.parse("§ato earn \u20AE rewards!"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
