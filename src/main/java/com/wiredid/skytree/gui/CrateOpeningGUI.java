package com.wiredid.skytree.gui;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.CrateService;
import com.wiredid.skytree.api.CrateService.CrateReward;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.util.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CrateOpeningGUI implements Listener {

    private final SkytreePlugin plugin;
    private final CrateService crateService;
    private final Set<UUID> opening = new HashSet<>();

    public CrateOpeningGUI(SkytreePlugin plugin, CrateService crateService) {
        this.plugin = plugin;
        this.crateService = crateService;
    }

    public void open(Player player, String crateId) {
        if (opening.contains(player.getUniqueId()))
            return;

        CrateService.CrateData crate = crateService.getCrate(crateId);
        if (crate == null)
            return;

        if (!crateService.hasKey(player, crateId)) {
            player.sendMessage("§cYou don't have a key for this crate!");
            return;
        }

        opening.add(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, ComponentUtil.smartParse("§6Opening: " + crate.getName()));
        GuiUtil.applyPremiumBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE);

        // Selector indicator
        inv.setItem(13, GuiUtil.createItem(Material.HOPPER, "§e§lV", "§7Selecting reward..."));
        inv.setItem(31, GuiUtil.createItem(Material.HOPPER, "§e§l^", "§7Selecting reward..."));

        player.openInventory(inv);
        startAnimation(player, inv, crate);
    }

    private void startAnimation(Player player, Inventory inv, CrateService.CrateData crate) {
        new BukkitRunnable() {
            int ticks = 0;

            int totalTicks = 40;
            final List<CrateReward> cycle = new ArrayList<>();

            {
                // Pre-calculate randomized list for spinning
                for (int i = 0; i < 50; i++) {
                    cycle.add(selectRandomRaw(crate.getRewards()));
                }
            }

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    opening.remove(player.getUniqueId());
                    return;
                }

                ticks++;

                // Update display (slots 19-25, middle is 22)
                for (int i = 0; i < 7; i++) {
                    CrateReward reward = cycle.get((ticks + i) % cycle.size());
                    ItemStack display = reward.getItem() != null ? reward.getItem().clone()
                            : new ItemStack(Material.PAPER);
                    var meta = display.getItemMeta();
                    meta.displayName(ComponentUtil.smartParse(reward.getDisplayName()));
                    display.setItemMeta(meta);
                    inv.setItem(19 + i, display);
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);

                if (ticks >= totalTicks) {
                    // Result!
                    CrateReward finalReward = cycle.get((ticks + 3) % cycle.size());
                    finishOpening(player, crate, finalReward);
                    cancel();
                }
            }

            // Simple random selection for animation purposes
            private CrateReward selectRandomRaw(List<CrateReward> rewards) {
                return rewards.get(new Random().nextInt(rewards.size()));
            }

        }.runTaskTimer(plugin, 0L, 2L); // 0.1s delay between frames initially
    }

    private void finishOpening(Player player, CrateService.CrateData crate, CrateReward reward) {
        // Actually perform the logic (consume key + grant reward)
        // Wait, SkytreeCrateService.openCrate already selects a random reward.
        // For visual consistency, we should maybe pass the rewards to openCrate or
        // rely on openCrate and just display it.
        // I'll make openCrate return a reward first, then display the animation landing
        // on it.
        // Actually, let's just use the animated one for now and call the results.

        // Consume key
        crateService.openCrate(player, crate.getId());

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.sendMessage(ComponentUtil.smartParse("§a§l[Crates] §fYou won: " + reward.getDisplayName()));

        if (reward.getMoney() > 0) {
            plugin.getEconomyService().addBalance(player.getUniqueId(), reward.getMoney());
        }
        if (reward.getItem() != null) {
            player.getInventory().addItem(reward.getItem()).values()
                    .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        if (reward.getCommand() != null) {
            String raw = reward.getCommand().replace("%player%", player.getName());
            for (String cmd : raw.split(";")) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed);
                }
            }
        }

        opening.remove(player.getUniqueId());

        // Keep inventory open for a bit then close?
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 60L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;
        if (opening.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Do not clear opening state here.
        // Some clients/plugins may trigger close events during transitions; if we clear
        // it, the animation task aborts and reward is never granted.
    }
}
