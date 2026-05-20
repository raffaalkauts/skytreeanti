package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrystalDamageListener implements Listener {

    private final SkytreePlugin plugin;
    // Map<CrystalUUID, PlayerUUID> - Tracks who detonated which crystal
    private final Map<UUID, UUID> crystalDetonators = new HashMap<>();

    public CrystalDamageListener(SkytreePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHitCrystal(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal))
            return;
        if (!(event.getDamager() instanceof Player))
            return;

        Player player = (Player) event.getDamager();
        Entity crystal = event.getEntity();

        // Register detonation
        crystalDetonators.put(crystal.getUniqueId(), player.getUniqueId());

        // Schedule cleanup (Crystal entities are removed quickly, but we want to ensure
        // no memory leaks)
        // 1 second (20 ticks) is more than enough for the explosion calculations to
        // finish
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            crystalDetonators.remove(crystal.getUniqueId());
        }, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplosionDamage(EntityDamageByEntityEvent event) {
        // Check if victim is a player
        if (!(event.getEntity() instanceof Player))
            return;
        Player victim = (Player) event.getEntity();

        // Check if damager is an Ender Crystal
        if (!(event.getDamager() instanceof EnderCrystal))
            return;
        Entity crystal = event.getDamager();

        // Check if the explosion cause is indeed an entity explosion (redundant but
        // safe)
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
            return;

        // Check if this crystal was detonated by THIS victim
        UUID detonatorId = crystalDetonators.get(crystal.getUniqueId());

        if (detonatorId != null && detonatorId.equals(victim.getUniqueId())) {
            // Self-Damage: Reduce significantly
            // User requested "smaller damage".
            // Standard reduction for efficient PvP flow is usually around 75-90% reduction
            // (taking 10-25%).
            // Full point-blank end crystal is ~120 damage (60 hearts).
            // 10% = 12 dmg (6 hearts).
            // 25% = 30 dmg (15 hearts, still lethal depending on armor).
            // With Blast Prot IV netherite, unmitigated damage is manageable, but naked is
            // death.
            // Let's go with 25% incoming damage (75% reduction) as a safe baseline.
            event.setDamage(event.getDamage() * 0.25);
        } else {
            // Enemy Damage: Full (default)
        }
    }
}
