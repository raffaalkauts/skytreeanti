package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.economy.JobData;
import com.wiredid.skytree.economy.JobService;
import com.wiredid.skytree.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class JobGUI implements Listener {

    private final SkytreePlugin plugin;
    private final JobService jobService;

    public JobGUI(SkytreePlugin plugin, JobService jobService) {
        this.plugin = plugin;
        this.jobService = jobService;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ComponentUtil.smartParse("§6§lJobs & Professions"));

        com.wiredid.skytree.util.GuiUtil.applyPremiumBorder(gui,
                Material.CYAN_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE);

        JobData data = jobService.getJobData(player.getUniqueId());

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 22};
        int idx = 0;
        for (String jobId : JobService.ALL_JOBS) {
            if (idx >= slots.length) break;
            int slot = slots[idx++];

            JobData.JobProgress prog = data.getOrCreate(jobId);
            int level = prog.level;
            double xp = prog.xp;
            double threshold = jobService.getXpThreshold(level);
            double earned = prog.totalEarned;
            int progressPct = threshold > 0 ? (int) ((xp / threshold) * 100) : 0;

            Material icon = JobService.JOB_ICONS.getOrDefault(jobId, Material.BOOK);
            String displayName = JobService.JOB_DISPLAY.getOrDefault(jobId, jobId);

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(ComponentUtil.smartParse("§e§l" + displayName));
            List<Component> lore = new ArrayList<>();
            lore.add(ComponentUtil.smartParse("§7Level: §f" + level));
            lore.add(ComponentUtil.smartParse("§7XP: §b" + String.format("%.0f", xp)
                    + " §7/ §b" + String.format("%.0f", threshold)
                    + " §8(" + progressPct + "%)"));
            lore.add(Component.empty());
            lore.add(ComponentUtil.smartParse("§7Total Earned: §a\u20AE "
                    + String.format("%,.2f", earned)));
            lore.add(Component.empty());
            lore.add(ComponentUtil.smartParse("§eClick for details"));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "job_id"),
                    PersistentDataType.STRING, jobId);
            item.setItemMeta(meta);

            gui.setItem(slot, item);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = close.getItemMeta();
        cMeta.displayName(ComponentUtil.smartParse("§cClose"));
        close.setItemMeta(cMeta);
        gui.setItem(49, close);

        player.openInventory(gui);
    }

    public void openDetail(Player player, String jobId) {
        JobData data = jobService.getJobData(player.getUniqueId());
        JobData.JobProgress prog = data.getOrCreate(jobId);

        int level = prog.level;
        double xp = prog.xp;
        double threshold = jobService.getXpThreshold(level);
        double earned = prog.totalEarned;
        int progressPct = threshold > 0 ? (int) ((xp / threshold) * 100) : 0;
        double bonus = (jobService.getLevelBonus(level) - 1.0) * 100;

        String displayName = JobService.JOB_DISPLAY.getOrDefault(jobId, jobId);
        Material icon = JobService.JOB_ICONS.getOrDefault(jobId, Material.BOOK);

        Inventory gui = Bukkit.createInventory(null, 27,
                ComponentUtil.smartParse("§6§l" + displayName + " §8» §7Details"));

        ItemStack info = new ItemStack(icon);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(ComponentUtil.smartParse("§e§l" + displayName));
        List<Component> lore = new ArrayList<>();
        lore.add(ComponentUtil.smartParse("§7Level: §f" + level));
        lore.add(ComponentUtil.smartParse("§7XP: §b" + String.format("%.0f", xp)
                + " §7/ §b" + String.format("%.0f", threshold)
                + " §8(" + progressPct + "%)"));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§7Payout Formula:"));
        lore.add(ComponentUtil.smartParse(" §8» §f" + String.format("%.2f", jobService.getBasePayout())
                + " §7+ worth §f× " + String.format("%.0f%%", jobService.getWorthPercent() * 100)));
        lore.add(ComponentUtil.smartParse(" §8» §f× " + String.format("%.0f%%", 100 + bonus) + " §7(level bonus)"));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§7Total Earned: §a\u20AE " + String.format("%,.2f", earned)));
        lore.add(ComponentUtil.smartParse("§7Actions Done: §e" + prog.actions));
        lore.add(Component.empty());
        lore.add(ComponentUtil.smartParse("§8Continue working to level up!"));
        meta.lore(lore);
        info.setItemMeta(meta);
        gui.setItem(13, info);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.displayName(ComponentUtil.smartParse("§cBack to Jobs"));
        back.setItemMeta(bMeta);
        gui.setItem(18, back);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = ComponentUtil.toLegacy(event.getView().title());

        if (title.contains("Jobs & Professions")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            if (event.getSlot() == 49) {
                event.getWhoClicked().closeInventory();
                return;
            }
            String jobId = event.getCurrentItem().getItemMeta()
                    .getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "job_id"), PersistentDataType.STRING);
            if (jobId != null) {
                openDetail((Player) event.getWhoClicked(), jobId);
            }
            return;
        }

        if (title.contains("» Details")) {
            event.setCancelled(true);
            if (event.getSlot() == 18) {
                open((Player) event.getWhoClicked());
            }
        }
    }
}
