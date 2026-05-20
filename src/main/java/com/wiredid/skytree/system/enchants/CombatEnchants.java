package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.event.Event;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.Random;

public class CombatEnchants {

    private static final Random random = new Random();

    public static class LifestealEnchant extends CustomEnchant {
        public LifestealEnchant() {
            super("lifesteal", "Lifesteal", Rarity.LEGENDARY, 5, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (!(e.getDamager() instanceof Player))
                    return;
                Player player = (Player) e.getDamager();

                if (random.nextInt(100) < (level * 5)) { // 5% per level
                    double heal = 1.0; // Half heart
                    var maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        player.setHealth(Math.min(maxHealthAttr.getValue(), player.getHealth() + heal));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to heal half a heart when hitting enemies.";
        }
    }

    public static class ArmoredEnchant extends CustomEnchant {
        public ArmoredEnchant() {
            super("armored", "Armored", Rarity.LEGENDARY, 4, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
                    Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                // Decreases damage from enemy swords by 2% per level
                double reduction = 1.0 - (level * 0.02);
                e.setDamage(e.getDamage() * reduction);
            }
        }

        @Override
        public String getDescription() {
            return "§7Reduces incoming damage from enemy swords.";
        }
    }

    public static class MoltenEnchant extends CustomEnchant {
        public MoltenEnchant() {
            super("molten", "Molten", Rarity.UNIQUE, 4, Arrays.asList(
                    Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof Player) {
                    if (random.nextInt(100) < (level * 10)) { // 10% per level
                        e.getDamager().setFireTicks(40); // 2 seconds
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to set attackers on fire.";
        }
    }

    public static class BlindnessEnchant extends CustomEnchant {
        public BlindnessEnchant() {
            super("blindness", "Blindness", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 8)) { // 8% per level
                        ((org.bukkit.entity.LivingEntity) e.getEntity()).addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60,
                                        0));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to blind enemies.";
        }
    }

    public static class PoisonEnchant extends CustomEnchant {
        public PoisonEnchant() {
            super("poison", "Poison", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 10)) { // 10% per level
                        ((org.bukkit.entity.LivingEntity) e.getEntity()).addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 100,
                                        level - 1));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to poison enemies.";
        }
    }

    public static class WitherEnchant extends CustomEnchant {
        public WitherEnchant() {
            super("wither", "Wither", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 5)) { // 5% per level
                        ((org.bukkit.entity.LivingEntity) e.getEntity()).addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WITHER, 60,
                                        level - 1));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to wither enemies.";
        }
    }

    public static class VampireEnchant extends CustomEnchant {
        public VampireEnchant() {
            super("vampire", "Vampire", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof Player) {
                    if (random.nextInt(100) < (level * 5)) {
                        Player player = (Player) e.getDamager();
                        double amount = e.getFinalDamage() * 0.1 * level;
                        var maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            player.setHealth(Math.min(maxHealthAttr.getValue(), player.getHealth() + amount));
                        }
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5f,
                                1.5f);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Heals you for a portion of damage dealt.";
        }
    }

    public static class ExecuteEnchant extends CustomEnchant {
        public ExecuteEnchant() {
            super("execute", "Execute", Rarity.ELITE, 5, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) e.getEntity();
                    var maxHealthAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null && victim.getHealth() / maxHealthAttr.getValue() < 0.25) { // Below 25%
                                                                                                         // health
                        e.setDamage(e.getDamage() * (1.1 + (level * 0.05)));
                        victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT, victim.getLocation().add(0, 1, 0),
                                10);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Deals more damage to low health targets.";
        }
    }

    public static class BleedEnchant extends CustomEnchant {
        public BleedEnchant() {
            super("bleed", "Bleed", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 10)) {
                        org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) e.getEntity();
                        org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin
                                .getProvidingPlugin(getClass());
                        new org.bukkit.scheduler.BukkitRunnable() {
                            int count = 0;

                            @Override
                            public void run() {
                                if (count >= 5 || victim.isDead()) {
                                    cancel();
                                    return;
                                }
                                victim.damage(1.0);
                                victim.getWorld().spawnParticle(org.bukkit.Particle.BLOCK,
                                        victim.getLocation().add(0, 1, 0), 5,
                                        org.bukkit.Bukkit.createBlockData(Material.REDSTONE_BLOCK));
                                count++;
                            }
                        }.runTaskTimer(plugin, 20L, 20L);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Causes enemies to bleed over time.";
        }
    }

    public static class CriticalEnchant extends CustomEnchant {
        public CriticalEnchant() {
            super("critical", "Critical", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (random.nextInt(100) < (level * 5)) {
                    e.setDamage(e.getDamage() * 1.5);
                    e.getEntity().getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT,
                            e.getEntity().getLocation().add(0, 1, 0), 10);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to deal critical damage.";
        }
    }

    public static class DoubleStrikeEnchant extends CustomEnchant {
        public DoubleStrikeEnchant() {
            super("double_strike", "Double Strike", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 8)) {
                        org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) e.getEntity();
                        org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin
                                .getProvidingPlugin(getClass());
                        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (!victim.isDead())
                                victim.damage(e.getDamage() * 0.5);
                        }, 5L);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to strike twice.";
        }
    }

    public static class DisarmEnchant extends CustomEnchant {
        public DisarmEnchant() {
            super("disarm", "Disarm", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof Player) {
                    if (random.nextInt(100) < (level * 5)) {
                        Player victim = (Player) e.getEntity();
                        victim.addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 40, 4));
                        victim.sendMessage("§c§l[CE] §7You have been disarmed!");
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to weaken enemies, making them 'disarmed'.";
        }
    }

    public static class IceColdEnchant extends CustomEnchant {
        public IceColdEnchant() {
            super("ice_cold", "Ice Cold", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 10)) {
                        org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) e.getEntity();
                        victim.addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 3));
                        victim.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE,
                                victim.getLocation().add(0, 1, 0), 15);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Slows down enemies upon impact.";
        }
    }

    public static class FearEnchant extends CustomEnchant {
        public FearEnchant() {
            super("fear", "Fear", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 5)) {
                        org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) e.getEntity();
                        victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 0));
                        victim.addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 2));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Inflicts blindness and slowness on enemies.";
        }
    }

    public static class LevitationEnchant extends CustomEnchant {
        public LevitationEnchant() {
            super("levitation", "Levitation", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 5)) {
                        ((org.bukkit.entity.LivingEntity) e.getEntity()).addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION,
                                        20 * level, 0));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Causes enemies to float in the air.";
        }
    }

    public static class RageEnchant extends CustomEnchant {
        public RageEnchant() {
            super("rage", "Rage", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getDamager() instanceof Player) {
                    Player p = (Player) e.getDamager();
                    var maxHealthAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        double hpPercent = p.getHealth() / maxHealthAttr.getValue();
                        if (hpPercent < 0.5) {
                            e.setDamage(e.getDamage() * (1.0 + (level * 0.1 * (1.0 - hpPercent))));
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Deals more damage when your health is low.";
        }
    }

    public static class CleaveEnchant extends CustomEnchant {
        public CleaveEnchant() {
            super("cleave", "Cleave", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (random.nextInt(100) < (level * 10)) {
                    double radius = 1.0 + level;
                    for (org.bukkit.entity.Entity ent : e.getEntity().getNearbyEntities(radius, radius, radius)) {
                        if (ent instanceof org.bukkit.entity.LivingEntity && ent != e.getDamager()
                                && ent != e.getEntity()) {
                            ((org.bukkit.entity.LivingEntity) ent).damage(e.getDamage() * 0.3);
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Deals AOE damage to nearby entities.";
        }
    }

    public static class StunEnchant extends CustomEnchant {
        public StunEnchant() {
            super("stun", "Stun", Rarity.ULTIMATE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 5)) {
                        org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) e.getEntity();
                        victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.SLOWNESS, 20 * level, 10));
                        victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT, victim.getLocation().add(0, 2, 0),
                                20);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Stuns enemies, preventing movement.";
        }
    }

    public static class ConfuseEnchant extends CustomEnchant {
        public ConfuseEnchant() {
            super("confuse", "Confuse", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (e.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                    if (random.nextInt(100) < (level * 10)) {
                        ((org.bukkit.entity.LivingEntity) e.getEntity()).addPotionEffect(
                                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA, 100, 0));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Confuses enemies with nausea.";
        }
    }

    public static class BountyHunterEnchant extends CustomEnchant {
        public BountyHunterEnchant() {
            super("bounty_hunter", "Bounty Hunter", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof org.bukkit.event.entity.EntityDeathEvent) {
                org.bukkit.event.entity.EntityDeathEvent e = (org.bukkit.event.entity.EntityDeathEvent) event;
                Player killer = e.getEntity().getKiller();
                if (killer != null && random.nextInt(100) < (level * 5)) {
                    double reward = 1.0 + (random.nextDouble() * 4 * level);
                    org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Skytree");
                    if (plugin instanceof com.wiredid.skytree.SkytreePlugin) {
                        ((com.wiredid.skytree.SkytreePlugin) plugin).getEconomyService()
                                .addBalance(killer.getUniqueId(), reward);
                        killer.sendMessage("§a§l[CE] §7Bounty Hunter: §e+" + String.format("%.2f", reward) + " USDT");
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to earn USDT when killing mobs.";
        }
    }

    public static class DragonBreathEnchant extends CustomEnchant {
        public DragonBreathEnchant() {
            super("dragon_breath", "Dragon Breath", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                if (random.nextInt(100) < (level * 5)) {
                    e.getEntity().getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH,
                            e.getEntity().getLocation(), 50, 1, 1, 1, 0.1);
                    for (org.bukkit.entity.Entity ent : e.getEntity().getNearbyEntities(4, 4, 4)) {
                        if (ent instanceof org.bukkit.entity.LivingEntity && ent != e.getDamager()) {
                            ent.setFireTicks(60);
                            ((org.bukkit.entity.LivingEntity) ent).damage(2.0 * level);
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7AOE fire damage surrounding the target.";
        }
    }
}
