package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import java.util.Arrays;
import java.util.Random;

public class ArmorEnchants {

    private static final Random random = new Random();

    public static class ReflectEnchant extends CustomEnchant {
        public ReflectEnchant() {
            super("reflect", "Reflect", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 10)) {
                        double reflected = e.getDamage() * 0.1 * level;
                        ((org.bukkit.entity.LivingEntity) e.getDamager()).damage(reflected);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to reflect damage back to the attacker.";
        }
    }

    public static class GearsEnchant extends CustomEnchant {
        public GearsEnchant() {
            super("gears", "Gears", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS,
                    Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            // Usually applied via a task or on equip, but we can use damage/interact as a
            // refresh pulse
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, level - 1));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Provides a Speed boost when taking damage.";
        }
    }

    public static class SpringsEnchant extends CustomEnchant {
        public SpringsEnchant() {
            super("springs", "Springs", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, level - 1));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Provides a Jump Boost when taking damage.";
        }
    }

    public static class OxygenEnchant extends CustomEnchant {
        public OxygenEnchant() {
            super("oxygen", "Oxygen", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_HELMET, Material.NETHERITE_HELMET));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 0));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Allows you to breathe underwater.";
        }
    }

    public static class NightVisionEnchant extends CustomEnchant {
        public NightVisionEnchant() {
            super("night_vision", "Night Vision", Rarity.UNIQUE, 1, Arrays.asList(
                    Material.DIAMOND_HELMET, Material.NETHERITE_HELMET));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Provides constant Night Vision.";
        }
    }

    public static class FlightEnchant extends CustomEnchant {
        public FlightEnchant() {
            super("flight", "Flight", Rarity.FABLED, 1, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.setAllowFlight(true);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Grants the ability to fly.";
        }
    }

    public static class DrillerEnchant extends CustomEnchant {
        public DrillerEnchant() {
            super("driller", "Driller", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS,
                    Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, level - 1));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Provides Haste when taking damage.";
        }
    }

    public static class SaturationEnchant extends CustomEnchant {
        public SaturationEnchant() {
            super("saturation", "Saturation", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_HELMET, Material.NETHERITE_HELMET));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.setFoodLevel(Math.min(20, p.getFoodLevel() + level));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Restores hunger when taking damage.";
        }
    }

    public static class HealthBoostEnchant extends CustomEnchant {
        public HealthBoostEnchant() {
            super("health_boost", "Health Boost", Rarity.LEGENDARY, 5, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 100, level - 1));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Provides extra health hearts.";
        }
    }

    public static class ResistanceEnchant extends CustomEnchant {
        public ResistanceEnchant() {
            super("resistance_ench", "Resistance", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, level - 1));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Provides a Resistance boost when taking damage.";
        }
    }

    public static class CactusEnchant extends CustomEnchant {
        public CactusEnchant() {
            super("cactus", "Cactus", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.entity.LivingEntity attacker = (org.bukkit.entity.LivingEntity) e.getDamager();
                    attacker.damage(1.0 * level);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Deals damage back to attackers.";
        }
    }

    public static class BurnEnchant extends CustomEnchant {
        public BurnEnchant() {
            super("burn", "Burn", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
                    Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.entity.LivingEntity attacker = (org.bukkit.entity.LivingEntity) e.getDamager();
                    attacker.setFireTicks(20 * 2 * level);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Sets attackers on fire.";
        }
    }

    public static class InvisibilityEnchant extends CustomEnchant {
        public InvisibilityEnchant() {
            super("invisibility", "Invisibility", Rarity.ULTIMATE, 1, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    if (random.nextInt(100) < 10) {
                        Player p = (Player) e.getEntity();
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to turn invisible when taking damage.";
        }
    }

    public static class ForceFieldEnchant extends CustomEnchant {
        public ForceFieldEnchant() {
            super("force_field", "Force Field", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player && random.nextInt(100) < (level * 10)) {
                    Player p = (Player) e.getEntity();
                    for (org.bukkit.entity.Entity ent : p.getNearbyEntities(2, 2, 2)) {
                        if (ent instanceof org.bukkit.entity.LivingEntity && ent != p) {
                            org.bukkit.util.Vector vec = ent.getLocation().toVector()
                                    .subtract(p.getLocation().toVector()).normalize();
                            ent.setVelocity(vec.multiply(1.5));
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to push away nearby enemies.";
        }
    }

    public static class SonarEnchant extends CustomEnchant {
        public SonarEnchant() {
            super("sonar", "Sonar", Rarity.ELITE, 1, Arrays.asList(
                    Material.DIAMOND_HELMET, Material.NETHERITE_HELMET));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    for (org.bukkit.entity.Entity ent : p.getNearbyEntities(15, 15, 15)) {
                        if (ent instanceof org.bukkit.entity.LivingEntity && ent != p) {
                            ((org.bukkit.entity.LivingEntity) ent)
                                    .addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0));
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Makes nearby enemies glow.";
        }
    }

    public static class AngelEnchant extends CustomEnchant {
        public AngelEnchant() {
            super("angel", "Angel", Rarity.FABLED, 1, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    if (p.getHealth() - e.getFinalDamage() <= 0) {
                        e.setDamage(0);
                        var maxHealthAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            p.setHealth(maxHealthAttr.getValue() * 0.5);
                        }
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                        p.sendMessage("§e§l[CE] §fAngel enchantment saved you!");
                        p.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, p.getLocation(), 30);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to be saved from fatal damage.";
        }
    }

    public static class BerserkEnchant extends CustomEnchant {
        public BerserkEnchant() {
            super("berserk", "Berserk", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    var maxHealthAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null && p.getHealth() / maxHealthAttr.getValue() < 0.25) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, level - 1));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, level - 1));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Gain Strength and Speed at low health.";
        }
    }

    public static class HeavyEnchant extends CustomEnchant {
        public HeavyEnchant() {
            super("heavy", "Heavy", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    org.bukkit.Bukkit.getScheduler()
                            .runTaskLater(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                                p.setVelocity(new org.bukkit.util.Vector(0, -0.1 * level, 0));
                            }, 1L);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Increases your fall speed, making you 'heavy'.";
        }
    }

    public static class GuardianEnchant extends CustomEnchant {
        public GuardianEnchant() {
            super("guardian", "Guardian", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof org.bukkit.entity.Projectile) {
                    e.setDamage(e.getDamage() * (1.0 - (level * 0.1)));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Reduces incoming projectile damage.";
        }
    }

    public static class ImmortalityEnchant extends CustomEnchant {
        public ImmortalityEnchant() {
            super("immortality", "Immortality", Rarity.FABLED, 1, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    Player p = (Player) e.getEntity();
                    var maxHealthAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null && p.getHealth() / maxHealthAttr.getValue() < 0.1) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
                        p.sendMessage("§d§l[CE] §5Immortality§7: You are briefly indestructible!");
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Grants near-indestructibility at extremely low health.";
        }
    }
}
