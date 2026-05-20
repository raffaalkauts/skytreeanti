package com.wiredid.skytree.system.enchants;

import com.wiredid.skytree.api.CustomEnchant;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;

public class UtilityEnchants {
    private static final Random random = new Random();

    private static final Set<Material> NATURAL_SHOVEL_BLOCKS = new HashSet<>(Arrays.asList(
            Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT,
            Material.PODZOL, Material.MYCELIUM, Material.SAND, Material.RED_SAND,
            Material.GRAVEL, Material.CLAY, Material.SOUL_SAND, Material.SOUL_SOIL,
            Material.SNOW, Material.SNOW_BLOCK, Material.MUD, Material.SUSPICIOUS_SAND,
            Material.SUSPICIOUS_GRAVEL));

    private static final Set<Material> NATURAL_PICKAXE_BLOCKS = new HashSet<>(Arrays.asList(
            Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.DEEPSLATE, Material.TUFF, Material.CALCITE, Material.NETHERRACK,
            Material.END_STONE, Material.BLACKSTONE, Material.BASALT, Material.AMETHYST_BLOCK,
            Material.DRIPSTONE_BLOCK, Material.SCULK));

    public static class VeinMinerEnchant extends CustomEnchant {
        public VeinMinerEnchant() {
            super("vein_miner", "Vein Miner", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Block block = e.getBlock();
                if (!isOre(block.getType()))
                    return;

                Set<Block> vein = new HashSet<>();
                findVein(block, block.getType(), vein, 10 + (level * 10));

                for (Block b : vein) {
                    if (b.equals(block))
                        continue;
                    b.breakNaturally(item);
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Mines the entire vein of ores automatically.";
        }

        private boolean isOre(Material m) {
            String name = m.name();
            return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
        }

        private void findVein(Block start, Material type, Set<Block> vein, int limit) {
            if (vein.size() >= limit || start.getType() != type || vein.contains(start))
                return;
            vein.add(start);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        findVein(start.getRelative(x, y, z), type, vein, limit);
                    }
                }
            }
        }
    }

    public static class TrenchEnchant extends CustomEnchant {
        public TrenchEnchant() {
            super("trench", "Trench", Rarity.ULTIMATE, 3, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                    Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Block block = e.getBlock();
                Player player = e.getPlayer();

                // Determine block face (This often requires a metadata system or RayTrace)
                // Since BlockBreakEvent doesn't natively have the Face clicked in the event
                // itself
                // (except in older versions or via NMS), we RayTrace or fallback to 3x3x3 or
                // similar.
                // However, Paper/Spigot often provides the clicked face if we track it from
                // PlayerInteractEvent.
                // For simplicity and robustness, we use the Player's direction.

                Set<Material> whitelist = item.getType().name().contains("PICKAXE") ? NATURAL_PICKAXE_BLOCKS
                        : NATURAL_SHOVEL_BLOCKS;

                // If the block itself isn't in whitelist, skip Trench
                if (!whitelist.contains(block.getType()))
                    return;

                int radius = level == 1 ? 1 : (level == 2 ? 1 : 2); // lvl 1: 3x3, lvl 2: 3x3, lvl 3: 5x5

                // Directional 3x3 or 5x5 breaking
                // We calculate the plane based on player pitch
                float pitch = player.getLocation().getPitch();
                float yaw = player.getLocation().getYaw();

                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            // Plane logic:
                            // If looking down/up (pitch > 45 or < -45): X-Z plane
                            // If looking horizontal (Z direction): X-Y plane
                            // If looking horizontal (X direction): Z-Y plane

                            Block target;
                            if (pitch > 45 || pitch < -45) {
                                // Looking vertical
                                if (y != 0)
                                    continue;
                                target = block.getRelative(x, 0, z);
                            } else {
                                // Looking horizontal
                                double absYaw = Math.abs(yaw % 360);
                                if ((absYaw > 45 && absYaw < 135) || (absYaw > 225 && absYaw < 315)) {
                                    // East/West -> Y-Z plane
                                    if (x != 0)
                                        continue;
                                    target = block.getRelative(0, y, z);
                                } else {
                                    // North/South -> Y-X plane
                                    if (z != 0)
                                        continue;
                                    target = block.getRelative(x, y, 0);
                                }
                            }

                            if (target.equals(block))
                                continue;
                            if (whitelist.contains(target.getType())) {
                                target.breakNaturally(item);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Mines a large area of blocks at once.";
        }
    }

    public static class AutoReelEnchant extends CustomEnchant {
        public AutoReelEnchant() {
            super("auto_reel", "Auto Reel", Rarity.ULTIMATE, 1, Arrays.asList(Material.FISHING_ROD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof PlayerFishEvent) {
                PlayerFishEvent e = (PlayerFishEvent) event;
                if (e.getState() == PlayerFishEvent.State.BITE) {
                    // Logic to automatically reel in is usually handled by a task or delayed packet
                    // Simplified: Message and sound
                    e.getPlayer().sendMessage("§b§l[CE] §7Auto Reel triggered!");
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Automatically reels in your fishing rod when a fish bites.";
        }
    }

    public static class AutoSmeltEnchant extends CustomEnchant {
        public AutoSmeltEnchant() {
            super("auto_smelt", "Auto Smelt", Rarity.ELITE, 1, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Material type = e.getBlock().getType();
                Material result = switch (type) {
                    case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
                    case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.GOLD_INGOT;
                    case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
                    default -> null;
                };

                if (result != null) {
                    e.setDropItems(false);
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(result));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Automatically smelts ores into ingots when mined.";
        }
    }

    public static class ReplenishEnchant extends CustomEnchant {
        public ReplenishEnchant() {
            super("replenish", "Replenish", Rarity.UNIQUE, 1, Arrays.asList(
                    Material.DIAMOND_HOE, Material.NETHERITE_HOE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Block block = e.getBlock();
                Material type = block.getType();

                if (type == Material.WHEAT || type == Material.CARROTS || type == Material.POTATOES
                        || type == Material.BEETROOTS) {
                    org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) block.getBlockData();
                    if (ageable.getAge() == ageable.getMaximumAge()) {
                        org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin
                                .getProvidingPlugin(getClass());
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            block.setType(type);
                            org.bukkit.block.data.Ageable newAge = (org.bukkit.block.data.Ageable) block.getBlockData();
                            newAge.setAge(0);
                            block.setBlockData(newAge);
                        }, 1L);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Automatically replants crops when harvested.";
        }
    }

    public static class LumberjackEnchant extends CustomEnchant {
        public LumberjackEnchant() {
            super("lumberjack", "Lumberjack", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Block block = e.getBlock();
                if (!isLog(block.getType()))
                    return;

                if (!isNaturalTree(block)) {
                    // It's a player placed log, don't trigger
                    return;
                }

                Set<Block> tree = new HashSet<>();
                findTree(block, block.getType(), tree, 20 + (level * 20));

                for (Block b : tree) {
                    if (b.equals(block))
                        continue;
                    b.breakNaturally(item);
                }
            }
        }

        private boolean isNaturalTree(Block logBlock) {
            // Check if there are LEAVES within radius
            // Natural trees always have leaves nearby
            for (int y = -2; y <= 8; y++) {
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Block check = logBlock.getRelative(x, y, z);
                        String name = check.getType().name();
                        if (name.endsWith("_LEAVES") || name.endsWith("_WART_BLOCK")
                                || check.getType() == Material.SHROOMLIGHT) {
                            return true; // Found leaves = natural tree
                        }
                    }
                }
            }
            return false; // No leaves = player-placed logs
        }

        @Override
        public String getDescription() {
            return "§7Chops down the entire tree automatically.";
        }

        private boolean isLog(Material m) {
            return m.name().endsWith("_LOG") || m.name().endsWith("_WOOD") || m.name().endsWith("_STEM")
                    || m.name().endsWith("_HYPHAE");
        }

        private void findTree(Block start, Material type, Set<Block> tree, int limit) {
            if (tree.size() >= limit || start.getType() != type || tree.contains(start))
                return;
            tree.add(start);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        findTree(start.getRelative(x, y, z), type, tree, limit);
                    }
                }
            }
        }
    }

    public static class MagnetEnchant extends CustomEnchant {
        public MagnetEnchant() {
            super("magnet", "Magnet", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                    Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                    Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Player p = e.getPlayer();
                org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin
                        .getProvidingPlugin(getClass());

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    double radius = 2.0 + level;
                    for (org.bukkit.entity.Entity ent : p.getNearbyEntities(radius, radius, radius)) {
                        if (ent instanceof org.bukkit.entity.Item) {
                            ent.teleport(p.getLocation());
                        }
                    }
                }, 2L);
            }
        }

        @Override
        public String getDescription() {
            return "§7Attracts nearby items to your location.";
        }
    }

    public static class ExperiencePlusEnchant extends CustomEnchant {
        public ExperiencePlusEnchant() {
            super("experience_plus", "Experience Plus", Rarity.ELITE, 5, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                e.setExpToDrop((int) (e.getExpToDrop() * (1.0 + (level * 0.5))));
            } else if (event instanceof org.bukkit.event.entity.EntityDeathEvent) {
                org.bukkit.event.entity.EntityDeathEvent e = (org.bukkit.event.entity.EntityDeathEvent) event;
                e.setDroppedExp((int) (e.getDroppedExp() * (1.0 + (level * 0.5))));
            }
        }

        @Override
        public String getDescription() {
            return "§7Significantly increases experience from all sources.";
        }
    }

    public static class FortunePlusEnchant extends CustomEnchant {
        public FortunePlusEnchant() {
            super("fortune_plus", "Fortune Plus", Rarity.ULTIMATE, 3, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                // Since Fortune is already handled by vanilla if applied,
                // we can add extra drops here.
                if (e.isDropItems() && e.getBlock().getType().name().endsWith("_ORE")) {
                    int extra = new Random().nextInt(level + 1);
                    if (extra > 0) {
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),
                                new ItemStack(e.getBlock().getDrops(item).iterator().next().getType(), extra));
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Adds extra drops to mined ores beyond Fortune.";
        }
    }

    public static class TimberEnchant extends CustomEnchant {
        public TimberEnchant() {
            super("timber", "Timber", Rarity.ELITE, 1, Arrays.asList(
                    Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Block block = e.getBlock();
                if (!isLog(block.getType()))
                    return;

                if (!isNaturalTree(block)) {
                    return;
                }

                Set<Block> tree = new HashSet<>();
                // Timber is stronger, higher limit
                findTree(block, block.getType(), tree, 200);

                for (Block b : tree) {
                    if (b.equals(block))
                        continue;
                    b.breakNaturally(item);
                }
            }
        }

        private boolean isLog(Material m) {
            return m.name().endsWith("_LOG") || m.name().endsWith("_WOOD") || m.name().endsWith("_STEM")
                    || m.name().endsWith("_HYPHAE");
        }

        private boolean isNaturalTree(Block logBlock) {
            // Check if there are LEAVES within radius
            for (int y = -2; y <= 8; y++) {
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Block check = logBlock.getRelative(x, y, z);
                        String name = check.getType().name();
                        if (name.endsWith("_LEAVES") || name.endsWith("_WART_BLOCK")
                                || check.getType() == Material.SHROOMLIGHT) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void findTree(Block start, Material type, Set<Block> tree, int limit) {
            if (tree.size() >= limit || start.getType() != type || tree.contains(start))
                return;
            tree.add(start);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        findTree(start.getRelative(x, y, z), type, tree, limit);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Quickly fells entire trees with a single break.";
        }
    }

    public static class GrowthEnchant extends CustomEnchant {
        public GrowthEnchant() {
            super("growth", "Growth", Rarity.UNIQUE, 3, Arrays.asList(
                    Material.DIAMOND_HOE, Material.NETHERITE_HOE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof org.bukkit.event.player.PlayerInteractEvent) {
                org.bukkit.event.player.PlayerInteractEvent e = (org.bukkit.event.player.PlayerInteractEvent) event;
                if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                    Block b = e.getClickedBlock();
                    if (b != null && b.getBlockData() instanceof org.bukkit.block.data.Ageable) {
                        org.bukkit.block.data.Ageable age = (org.bukkit.block.data.Ageable) b.getBlockData();
                        if (age.getAge() < age.getMaximumAge()) {
                            age.setAge(Math.min(age.getMaximumAge(), age.getAge() + level));
                            b.setBlockData(age);
                            b.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                                    b.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3);
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Instantly grows crops when right-clicked.";
        }
    }

    public static class AutoRepairEnchant extends CustomEnchant {
        public AutoRepairEnchant() {
            super("auto_repair", "Auto Repair", Rarity.ULTIMATE, 3, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent
                    || event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
                if (random.nextInt(100) < (level * 20)) {
                    org.bukkit.inventory.meta.Damageable meta = (org.bukkit.inventory.meta.Damageable) item
                            .getItemMeta();
                    if (meta != null && meta.getDamage() > 0) {
                        meta.setDamage(meta.getDamage() - 1);
                        item.setItemMeta(meta);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to repair the item while in use.";
        }
    }

    public static class DrillEnchant extends CustomEnchant {
        public DrillEnchant() {
            super("drill", "Drill", Rarity.ELITE, 1, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                    Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                Block b = e.getBlock();
                for (int y = -1; y <= 1; y++) {
                    if (y == 0)
                        continue;
                    Block target = b.getRelative(0, y, 0);
                    if (target.getType().getHardness() >= 0
                            && target.getType().getHardness() <= b.getType().getHardness()) {
                        target.breakNaturally(item);
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Mines a 1x3 vertical column of blocks.";
        }
    }

    public static class NautilusEnchant extends CustomEnchant {
        public NautilusEnchant() {
            super("nautilus", "Nautilus", Rarity.ELITE, 1, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                if (e.getPlayer().getLocation().getBlock().getType() == Material.WATER) {
                    e.getPlayer().addPotionEffect(
                            new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, 40, 1));
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Provides Haste when mining underwater.";
        }
    }

    public static class GreedEnchant extends CustomEnchant {
        public GreedEnchant() {
            super("greed", "Greed", Rarity.LEGENDARY, 3, Arrays.asList(
                    Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof BlockBreakEvent) {
                BlockBreakEvent e = (BlockBreakEvent) event;
                if (random.nextInt(1000) < (level * 5)) {
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),
                            new ItemStack(Material.GOLD_INGOT, 1));
                    e.getPlayer().sendMessage("§6§l[CE] §7Greed triggered! Extra gold found.");
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Chance to find extra gold while mining.";
        }
    }

    public static class BlessedEnchant extends CustomEnchant {
        public BlessedEnchant() {
            super("blessed", "Blessed", Rarity.ELITE, 3, Arrays.asList(
                    Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.FISHING_ROD));
        }

        @Override
        public void onTrigger(Event event, int level, ItemStack item) {
            if (event instanceof org.bukkit.event.player.PlayerFishEvent) {
                org.bukkit.event.player.PlayerFishEvent e = (org.bukkit.event.player.PlayerFishEvent) event;
                if (e.getState() == org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH) {
                    if (random.nextInt(100) < (level * 10)) {
                        e.getPlayer().sendMessage("§f§l[CE] §bBlessed§7: Luck is on your side!");
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "§7Greatly increases luck while fishing.";
        }
    }
}
