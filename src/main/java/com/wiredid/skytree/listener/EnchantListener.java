package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.CustomEnchant;
import com.wiredid.skytree.api.event.CustomEnchantTriggerEvent;
import com.wiredid.skytree.system.EnchantRegistry;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import java.util.Map;
import java.util.Random;

public class EnchantListener implements Listener {

    private final EnchantRegistry registry;
    private final Random random = new Random();
    private static final ThreadLocal<Boolean> recursionGuard = ThreadLocal.withInitial(() -> false);

    public EnchantListener(SkytreePlugin plugin, EnchantRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            triggerEnchants(attacker.getInventory().getItemInMainHand(), event);
        }
        if (event.getEntity() instanceof Player) {
            Player defender = (Player) event.getEntity();
            for (ItemStack armor : defender.getInventory().getArmorContents()) {
                triggerEnchants(armor, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        triggerEnchants(event.getPlayer().getInventory().getItemInMainHand(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        triggerEnchants(event.getPlayer().getInventory().getItemInMainHand(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            triggerEnchants(event.getBow(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (proj.getShooter() instanceof Player) {
            Player shooter = (Player) proj.getShooter();
            // We need to store the bow used or just use the current main hand if it's a bow
            ItemStack bow = shooter.getInventory().getItemInMainHand();
            if (bow != null
                    && (bow.getType() == org.bukkit.Material.BOW || bow.getType() == org.bukkit.Material.CROSSBOW)) {
                triggerEnchants(bow, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        triggerEnchants(event.getItem(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            triggerEnchants(event.getEntity().getKiller().getInventory().getItemInMainHand(), event);
        }
    }

    @EventHandler
    public void onEnchantTable(EnchantItemEvent event) {
        // Integration with Enchantment Table
        // Logic: For each CE in registry, check if it applies to the item.
        // Roll based on weight.
        for (CustomEnchant enchant : registry.getAllEnchants()) {
            if (!enchant.getAppliesTo().contains(event.getItem().getType()))
                continue;

            // Basic weight-based roll
            if (random.nextInt(1000) < enchant.getRarity().getWeight()) {
                int level = random.nextInt(enchant.getMaxLevel()) + 1;
                registry.applyEnchant(event.getItem(), enchant, level);
            }
        }
    }

    private void triggerEnchants(ItemStack item, org.bukkit.event.Event event) {
        if (recursionGuard.get())
            return;
        if (item == null || item.getType().isAir())
            return;

        Map<CustomEnchant, Integer> enchants = registry.getEnchantsOnItem(item);
        if (enchants.isEmpty())
            return;

        recursionGuard.set(true);
        try {
            Player player = null;
            if (event instanceof BlockBreakEvent e)
                player = e.getPlayer();
            else if (event instanceof EntityDamageByEntityEvent e && e.getDamager() instanceof Player)
                player = (Player) e.getDamager();
            else if (event instanceof EntityDamageByEntityEvent e && e.getEntity() instanceof Player)
                player = (Player) e.getEntity();
            else if (event instanceof PlayerFishEvent e)
                player = e.getPlayer();
            else if (event instanceof EntityShootBowEvent e && e.getEntity() instanceof Player)
                player = (Player) e.getEntity();
            else if (event instanceof ProjectileHitEvent e && e.getEntity().getShooter() instanceof Player)
                player = (Player) e.getEntity().getShooter();
            else if (event instanceof PlayerInteractEvent e)
                player = e.getPlayer();
            else if (event instanceof EntityDeathEvent e && e.getEntity().getKiller() != null)
                player = e.getEntity().getKiller();

            for (Map.Entry<CustomEnchant, Integer> entry : enchants.entrySet()) {
                CustomEnchant enchant = entry.getKey();
                int level = entry.getValue();

                enchant.onTrigger(event, level, item);

                if (player != null) {
                    Bukkit.getPluginManager()
                            .callEvent(new CustomEnchantTriggerEvent(player, enchant, level, item, event));
                }
            }
        } finally {
            recursionGuard.set(false);
        }
    }
}
