package com.wiredid.skytree.gui;

import com.wiredid.skytree.system.QuestSystem;
import com.wiredid.skytree.system.QuestSystem.QuestData;
import com.wiredid.skytree.system.QuestSystem.CategoryData;
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
import java.util.UUID;

/**
 * GUI for viewing and managing quests (Overhauled)
 */
public class QuestGUI {

    private final QuestSystem questSystem;

    public QuestGUI(QuestSystem questSystem) {
        this.questSystem = questSystem;
    }

    public void open(Player player) {
        // Sync progress before opening
        questSystem.syncProgress(player);

        Inventory gui = Bukkit.createInventory(null, 54, ComponentUtil.smartParse("§6§lQuests §8» §7Challenges"));
        UUID uuid = player.getUniqueId();

        // Fill background with Premium Border
        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui, Material.PURPLE_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE);

        // Active quest (Interior top middle)
        QuestData active = questSystem.getActiveQuest(uuid);
        if (active != null) {
            gui.setItem(13, createActiveQuestItem(player, active));
        } else {
            gui.setItem(13, createNoActiveQuestItem());
        }

        // Categories & Quests (Rows 2, 3, 4)
        int[] catSlots = { 28, 37, 46 }; // Starting from row 3 is too far?
        // Let's use 19, 28, 37.
        catSlots = new int[] { 19, 28, 37 };
        int catIndex = 0;
        for (CategoryData cat : questSystem.getCategories().values()) {
            if (catIndex >= catSlots.length)
                break;
            int catSlot = catSlots[catIndex++];
            gui.setItem(catSlot, createCategoryItem(cat));

            // Quests in this category (Next 6 slots in the same row)
            int questSlot = catSlot + 1;
            List<QuestData> categoryQuests = questSystem.getQuests().values().stream()
                    .filter(q -> q.category.equals(cat.id))
                    .toList();

            for (QuestData quest : categoryQuests) {
                if (questSlot > catSlot + 7 || questSlot >= 54)
                    break;
                // Avoid the right edge border col 8
                if (questSlot % 9 == 8)
                    break;

                gui.setItem(questSlot, createQuestItem(uuid, quest));
                questSlot++;
            }
        }

        // footer centered above border row
        gui.setItem(40, createRecipeBrowserIcon());

        player.openInventory(gui);
    }

    private ItemStack createCategoryItem(CategoryData cat) {
        ItemStack item = new ItemStack(cat.icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse(cat.name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRecipeBrowserIcon() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§2§lRecipe Browser"));
        meta.lore(java.util.List.of(ComponentUtil.smartParse("§7Click to view all custom recipes")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActiveQuestItem(Player player, QuestData quest) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§e§lACTIVE QUEST"));

        int progress = questSystem.getProgress(player.getUniqueId(), quest.id);
        int target = quest.target;
        double percentage = (double) progress / target * 100;

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§6" + quest.name));
        lore.add(ComponentUtil.smartParse("§7" + quest.description));
        lore.add(Component.empty());
        lore.add(ComponentUtil
                .smartParse("§7Progress: §e" + progress + "/" + target + " §7(" + String.format("%.1f", percentage)
                        + "%)"));
        lore.add(ComponentUtil.smartParse(createProgressBar(progress, target)));
        lore.add(ComponentUtil.parse(""));
        lore.add(ComponentUtil.parse("§6Rewards:"));
        if (quest.rewards != null) {
            if (quest.rewards.contains("money"))
                lore.add(ComponentUtil.smartParse(" §e- " + quest.rewards.getDouble("money") + " \u20AE"));
            if (quest.rewards.contains("shards"))
                lore.add(ComponentUtil.smartParse(" §b- " + quest.rewards.getInt("shards") + " Shards"));
            if (quest.rewards.contains("items"))
                lore.add(ComponentUtil.smartParse(" §e- Custom Items"));
        }
        lore.add(ComponentUtil.parse(""));
        lore.add(ComponentUtil.parse("§7Hint: §f" + quest.hint));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNoActiveQuestItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§7No Active Quest"));
        meta.lore(java.util.List.of(ComponentUtil.smartParse("§7Select a quest below to start!")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createQuestItem(UUID uuid, QuestData quest) {
        boolean completed = questSystem.isCompleted(uuid, quest.id);
        boolean locked = !questSystem.canStart(uuid, quest);

        Material material = completed ? Material.EMERALD : (locked ? Material.BARRIER : Material.MAP);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (completed) {
            meta.displayName(ComponentUtil.smartParse("§a§l✓ " + quest.name));
        } else if (locked) {
            meta.displayName(ComponentUtil.smartParse("§c§lLocked: " + quest.name));
        } else {
            meta.displayName(ComponentUtil.smartParse("§e" + quest.name));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7" + quest.description));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§7Target: §e" + quest.target));
        lore.add(Component.empty());

        if (completed) {
            lore.add(ComponentUtil.smartParse("§a§l✓ COMPLETED"));
        } else if (locked) {
            lore.add(ComponentUtil.smartParse("§cRequires previous quest!"));
        } else {
            lore.add(ComponentUtil.smartParse("§eClick to start!"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String createProgressBar(int progress, int target) {
        int bars = 20;
        int filled = (int) ((double) progress / target * bars);
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) {
            bar.append(i < filled ? "§a█" : "§7█");
        }
        bar.append("§7]");
        return bar.toString();
    }
}
