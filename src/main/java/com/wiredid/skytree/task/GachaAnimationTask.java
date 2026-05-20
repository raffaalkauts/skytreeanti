package com.wiredid.skytree.task;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.impl.MythicItemManager.GachaCrateDef;
import com.wiredid.skytree.impl.MythicItemManager.GachaGlobalConfig;
import com.wiredid.skytree.impl.SkytreeGachaService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;

public class GachaAnimationTask extends BukkitRunnable {

    private final Player player;
    private final SkytreeGachaService gachaService;
    private final String crateId;

    private ItemStack pendingReward;
    private GachaGlobalConfig config;
    private Inventory gui;

    private int tick = 0;
    private int step = 0; // 0=Spinning, 1=SlowDown, 2=Claim

    public GachaAnimationTask(SkytreePlugin plugin, Player player, GachaCrateDef crate,
            SkytreeGachaService gachaService, String crateId) {
        this.player = player;
        this.gachaService = gachaService;
        this.crateId = crateId;
        // plugin and crate are not stored as they are not used in logic
    }

    public void prepare(ItemStack reward, GachaGlobalConfig config) {
        this.pendingReward = reward;
        this.config = config;

        // Open GUI
        this.gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lGacha: " + crateId));

        // Fill glass with Premium Border visual
        for (int i = 0; i < 27; i++) {
            if (i == 13)
                continue;
            // Oscillating or static premium border
            Material border = (i % 2 == 0) ? Material.ORANGE_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;
            gui.setItem(i, new ItemStack(border));
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
    }

    @Override
    public void run() {
        if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(gui)) {
            // Safer to give reward to avoid scam
            giveReward();
            this.cancel();
            return;
        }

        tick++;

        if (step == 0) { // FAST SPIN
            if (tick % 2 == 0) {
                ItemStack visual = gachaService.getRandomVisualItem(config);
                gui.setItem(13, visual);
                // Pitch increases with speed
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f + (tick / 60f));
            }
            if (tick > 35)
                step = 1;
        } else if (step == 1) { // SLOW SPIN
            int interval = 2 + (tick - 35) / 4; // Gradually slower
            if (tick % Math.max(2, interval) == 0) {
                ItemStack visual = gachaService.getRandomVisualItem(config);
                gui.setItem(13, visual);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f - (tick / 100f));
            }
            if (tick > 65) {
                step = 2;
                // Reveal
                gui.setItem(13, pendingReward);
            }
        } else if (step == 2) { // CLAIM
            if (tick == 66) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            if (tick > 90) {
                player.closeInventory();
                giveReward();
                this.cancel();
            }
        }
    }

    private void giveReward() {
        if (pendingReward == null)
            return;

        java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(pendingReward);
        if (!left.isEmpty()) {
            for (ItemStack item : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage("§c§l[Gacha] §cInventory full! Item dropped at your feet.");
        }
        gachaService.playWinEffects(player, pendingReward);
        pendingReward = null; // Mark as given
    }
}
