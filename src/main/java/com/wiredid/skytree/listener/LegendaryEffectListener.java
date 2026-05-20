package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.CustomEnchant;
import com.wiredid.skytree.api.event.CustomEnchantTriggerEvent;
import com.wiredid.skytree.model.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class LegendaryEffectListener implements Listener {

    private final SkytreePlugin plugin;

    public LegendaryEffectListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantTrigger(CustomEnchantTriggerEvent event) {
        CustomEnchant enchant = event.getEnchant();
        CustomEnchant.Rarity rarity = enchant.getRarity();

        if (rarity == CustomEnchant.Rarity.LEGENDARY || rarity == CustomEnchant.Rarity.FABLED) {
            Player player = event.getPlayer();

            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 20, 0.5,
                    0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3,
                    0.3, 0.05);

            if (rarity == CustomEnchant.Rarity.FABLED) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
            }

            PlayerData data = plugin.getPersistenceService().loadPlayerData(player.getUniqueId());
            if (data.getSettings().getOrDefault("actionbar", true)) {
                String prefix = rarity.getPrefix();
                Component message = com.wiredid.skytree.util.ComponentUtil
                        .parse(prefix + " §l" + enchant.getDisplayName() + " §r§7Activated!");
                player.sendActionBar(message);
            }
        }
    }
}
