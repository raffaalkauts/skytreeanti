package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Random;

public class BowEnchants {

    private static final Random random = new Random();

    public static class DoubleShotEnchant extends CustomEnchant {
        public DoubleShotEnchant() {
            super("double_shot", "Double Shot", Rarity.ELITE, 3, Arrays.asList(Material.BOW, Material.CROSSBOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityShootBowEvent) {
                EntityShootBowEvent e = (EntityShootBowEvent) event;
                if (random.nextInt(100) < (level * 10)) {
                    Arrow extra = e.getEntity().launchProjectile(Arrow.class,
                            e.getProjectile().getVelocity().rotateAroundY(Math.toRadians(5)));
                    extra.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                    extra.setDamage(e.getProjectile() instanceof Arrow ? ((Arrow) e.getProjectile()).getDamage() : 2.0);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to shoot an extra arrow.";
        }
    }

    public static class ExplosiveEnchant extends CustomEnchant {
        public ExplosiveEnchant() {
            super("explosive", "Explosive", Rarity.ULTIMATE, 3, Arrays.asList(Material.BOW, Material.CROSSBOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                e.getEntity().getWorld().createExplosion(e.getEntity().getLocation(), (float) (1.0 + level), false,
                        false);
            }
        }

        @Override
        public String getDescription() {
            return "§7Causes arrows to explode on impact.";
        }
    }

    public static class LightningEnchant extends CustomEnchant {
        public LightningEnchant() {
            super("lightning", "Lightning", Rarity.ELITE, 3, Arrays.asList(Material.BOW, Material.CROSSBOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                if (e.getHitEntity() != null) {
                    if (random.nextInt(100) < (level * 15)) {
                        e.getHitEntity().getWorld().strikeLightning(e.getHitEntity().getLocation());
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to strike lightning on impact.";
        }
    }

    public static class PullEnchant extends CustomEnchant {
        public PullEnchant() {
            super("pull", "Pull", Rarity.UNIQUE, 3, Arrays.asList(Material.BOW, Material.CROSSBOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                if (e.getHitEntity() instanceof LivingEntity && e.getEntity().getShooter() instanceof Player) {
                    Player shooter = (Player) e.getEntity().getShooter();
                    LivingEntity victim = (LivingEntity) e.getHitEntity();
                    Vector dir = shooter.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize();
                    victim.setVelocity(dir.multiply(0.5 * level));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Pulls hit entities towards you.";
        }
    }

    public static class VenomEnchant extends CustomEnchant {
        public VenomEnchant() {
            super("venom", "Venom", Rarity.UNIQUE, 3, Arrays.asList(Material.BOW, Material.CROSSBOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                if (e.getHitEntity() instanceof LivingEntity) {
                    if (random.nextInt(100) < (level * 20)) {
                        ((LivingEntity) e.getHitEntity())
                                .addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, level - 1));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to poison hit entities.";
        }
    }

    public static class TeleportEnchant extends CustomEnchant {
        public TeleportEnchant() {
            super("teleport_bow", "Teleport", Rarity.ULTIMATE, 1, Arrays.asList(Material.BOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                if (e.getEntity().getShooter() instanceof Player) {
                    Player p = (Player) e.getEntity().getShooter();
                    p.teleport(e.getEntity().getLocation());
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Teleports you to where the arrow lands.";
        }
    }

    public static class WebEnchant extends CustomEnchant {
        public WebEnchant() {
            super("web", "Web", Rarity.ELITE, 3, Arrays.asList(Material.BOW, Material.CROSSBOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                if (random.nextInt(100) < (level * 20)) {
                    e.getEntity().getLocation().getBlock().setType(Material.COBWEB);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to create a cobweb on impact.";
        }
    }

    public static class SniperEnchant extends CustomEnchant {
        public SniperEnchant() {
            super("sniper", "Sniper", Rarity.LEGENDARY, 3, Arrays.asList(Material.BOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                if (e.getHitEntity() instanceof LivingEntity && e.getEntity().getShooter() instanceof Player) {
                    Player p = (Player) e.getEntity().getShooter();
                    double dist = p.getLocation().distance(e.getEntity().getLocation());
                    if (dist > 30) {
                        ((LivingEntity) e.getHitEntity()).damage(dist * 0.1 * level);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Deals more damage to far away enemies.";
        }
    }

    public static class MultishotEnchant extends CustomEnchant {
        public MultishotEnchant() {
            super("multishot_ench", "Multishot", Rarity.ULTIMATE, 1, Arrays.asList(Material.BOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityShootBowEvent) {
                EntityShootBowEvent e = (EntityShootBowEvent) event;
                Arrow a1 = e.getEntity().launchProjectile(Arrow.class,
                        e.getProjectile().getVelocity().clone().rotateAroundY(Math.toRadians(10)));
                Arrow a2 = e.getEntity().launchProjectile(Arrow.class,
                        e.getProjectile().getVelocity().clone().rotateAroundY(Math.toRadians(-10)));
                a1.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                a2.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            }
        }

        @Override
        public String getDescription() {
            return "§7Shoots multiple arrows at once.";
        }
    }

    public static class FlareEnchant extends CustomEnchant {
        public FlareEnchant() {
            super("flare", "Flare", Rarity.UNIQUE, 3, Arrays.asList(Material.BOW, Material.CROSSBOW));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof ProjectileHitEvent) {
                ProjectileHitEvent e = (ProjectileHitEvent) event;
                e.getEntity().getWorld().spawnParticle(org.bukkit.Particle.FLAME, e.getEntity().getLocation(), 50, 1, 1,
                        1, 0.1);
                for (org.bukkit.entity.Entity ent : e.getEntity().getNearbyEntities(3, 3, 3)) {
                    if (ent instanceof LivingEntity) {
                        ent.setFireTicks(40 * level);
                        ((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Spawns a flare that sets nearby enemies on fire.";
        }
    }
}
