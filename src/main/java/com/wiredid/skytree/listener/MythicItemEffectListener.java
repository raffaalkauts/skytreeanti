package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.impl.SkytreeItemRegistry;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
// wind charge is AbstractWindCharge or WindCharge entity.

import org.bukkit.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import com.wiredid.skytree.model.MythicItemConfig;

public class MythicItemEffectListener implements Listener {

    private final SkytreePlugin plugin;
    private static final ThreadLocal<Boolean> processingAOE = ThreadLocal.withInitial(() -> false);
    private final MythicItemConfig configCache = new MythicItemConfig();
    private final Map<UUID, String> playerArmorSetCache = new HashMap<>();
    private final Map<UUID, Boolean> playerFlightCache = new HashMap<>();

    public MythicItemEffectListener(SkytreePlugin plugin, SkytreeItemRegistry registry) {
        this.plugin = plugin;
        loadConfigCache();

        // Start flight check and armor effect task
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    checkFlightNormalized(p);
                    checkArmorSetEffectsNormalized(p);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L); // Check every 0.25 seconds (5 ticks) for responsiveness
    }

    public void loadConfigCache() {
        configCache.clear();

        org.bukkit.configuration.ConfigurationSection armorSets = plugin.getConfig()
                .getConfigurationSection("mythic_logic.armor_sets");
        if (armorSets != null) {
            for (String prefix : armorSets.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection effectsSec = armorSets
                        .getConfigurationSection(prefix + ".effects");
                if (effectsSec != null) {
                    for (String effectKey : effectsSec.getKeys(false)) {
                        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(effectKey.toLowerCase());
                        PotionEffectType type = org.bukkit.Registry.POTION_EFFECT_TYPE.get(key);
                        if (type != null) {
                            configCache.addArmorSetEffect(prefix, type, effectsSec.getInt(effectKey, 0));
                        }
                    }
                }
            }
        }

        org.bukkit.configuration.ConfigurationSection chances = plugin.getConfig()
                .getConfigurationSection("mythic_logic.chances");
        if (chances != null) {
            for (String key : chances.getKeys(false)) {
                configCache.setChance(key, chances.getDouble(key));
            }
        }
    }

    private void checkArmorSetEffectsNormalized(Player p) {
        ItemStack helm = p.getInventory().getHelmet();
        ItemStack chest = p.getInventory().getChestplate();
        ItemStack legs = p.getInventory().getLeggings();
        ItemStack boots = p.getInventory().getBoots();

        if (helm == null || chest == null || legs == null || boots == null) {
            playerArmorSetCache.remove(p.getUniqueId());
            return;
        }

        String id = getSkytreeId(helm);
        if (id == null) {
            playerArmorSetCache.remove(p.getUniqueId());
            return;
        }

        String prefix = id.split("_")[0];

        // Check if full set is actually worn
        if (!checkSet(prefix, chest, legs, boots)) {
            playerArmorSetCache.remove(p.getUniqueId());
            return;
        }

        // Cache the set for this player
        playerArmorSetCache.put(p.getUniqueId(), prefix);

        // Apply effects from cache (O(1) lookup)
        Map<PotionEffectType, Integer> effects = configCache.getArmorSetEffects(prefix);
        if (effects != null) {
            for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                p.addPotionEffect(new PotionEffect(entry.getKey(), 40, entry.getValue(), false, false));
            }
        }

        // Special logic for Uranium (Radiation)
        if (prefix.equals("uranium")) {
            for (org.bukkit.entity.Entity e : p.getNearbyEntities(5, 5, 5)) {
                if (e instanceof LivingEntity && e != p) {
                    ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1));
                }
            }
        }

        // Special logic for Brass Night Vision
        if (prefix.equals("brass") && helm.getType() == Material.GOLDEN_HELMET) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, false, false));
        }
    }

    private boolean checkSet(String prefix, ItemStack... pieces) {
        for (ItemStack p : pieces) {
            String id = getSkytreeId(p);
            if (id == null || !id.startsWith(prefix + "_"))
                return false;
        }
        return true;
    }

    private String getSkytreeId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String id = data.get(new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING);
        if (id == null) {
            // Fallback to mythic_id
            id = data.get(new NamespacedKey(plugin, "mythic_id"), PersistentDataType.STRING);
        }
        return id;
    }

    // ==========================================
    // MINING EFFECTS
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (processingAOE.get())
            return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        String id = getSkytreeId(tool);

        if (id == null)
            return;

        // PRIME DRILL
        // Handled in Interact usually for "Instant Break", but here we handle
        // drops/logic if needed.

        // CUSTOM TOOL EFFECTS

        // Magnetism (Aluminum)
        if (id.startsWith("aluminum_")) {
            handleMagnetism(event.getBlock().getLocation(), 3);
        }

        // Auto-Smelt (Nickel)
        if (id.startsWith("nickel_")) {
            handleAutoSmelt(event, 0);
        }

        // Dual Harvest (Bronze)
        double dualHarvestChance = configCache.getChance("dual_harvest", 0.10);
        if (id.startsWith("bronze_") && Math.random() < dualHarvestChance) {
            // Re-drop items
            for (ItemStack drop : event.getBlock().getDrops(tool)) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
            }
        }

        // 3x3 Mining (Steel Pickaxe/Shovel)
        if (id.equals("steel_pickaxe") || id.equals("steel_shovel")) {
            handleAreaMining(event, 1);
        }
        // Vein Miner (Uranium Pickaxe/Axe)
        if (id.equals("uranium_pickaxe") || id.equals("uranium_axe")) {
            // Use existing vein logic but adapted for ores if pickaxe
            if (id.contains("pickaxe")) {
                // Needs Ore Vein logic
                handleOreVeinMining(event);
            } else {
                handleVeinMining(event); // Log vein logic
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // PRIME DRILL INSTANT BREAK
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            String id = getSkytreeId(item);
            if ("prime_drill".equals(id)) {
                // Instant break
                Block b = event.getClickedBlock();
                if (b != null && b.getType() != Material.BEDROCK && b.getType() != Material.BARRIER) {
                    // Check protection
                    if (plugin.getIslandService() != null
                            && !plugin.getIslandService().canModify(player, b.getLocation())) {
                        player.sendMessage("§cYou don't have permission to use this tool here!");
                        return;
                    }
                    b.breakNaturally(item);
                }
            }
        }
    }

    private void handleMagnetism(org.bukkit.Location loc, int radius) {
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof org.bukkit.entity.Item) {
                // Pull towards nearest player
                Player p = loc.getWorld().getPlayers().stream()
                        .min((p1, p2) -> Double.compare(p1.getLocation().distance(loc), p2.getLocation().distance(loc)))
                        .orElse(null);

                if (p != null && p.getLocation().distance(loc) < 10) {
                    e.setVelocity(
                            p.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.5));
                }
            }
        }
    }

    private void handleOreVeinMining(BlockBreakEvent event) {
        Block start = event.getBlock();
        String type = start.getType().name();
        if (!type.contains("ORE") && !type.contains("ANCIENT_DEBRIS"))
            return;

        Player player = event.getPlayer();
        if (player.hasMetadata("is_vein_mining"))
            return;

        player.setMetadata("is_vein_mining", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        processingAOE.set(true);
        try {
            breakRecursiveOre(player, start, player.getInventory().getItemInMainHand(), new HashSet<>(), 100);
        } finally {
            processingAOE.set(false);
        }
        player.removeMetadata("is_vein_mining", plugin);
    }

    private void breakRecursiveOre(Player player, Block block, ItemStack tool, Set<Block> visited, int limit) {
        if (visited.size() >= limit)
            return;

        // Protection check
        if (plugin.getIslandService() != null && !plugin.getIslandService().canModify(player, block.getLocation())) {
            return;
        }

        visited.add(block);
        block.breakNaturally(tool);

        Material mat = block.getType();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    Block next = block.getRelative(x, y, z);
                    if (next.getType() == mat && !visited.contains(next)) {
                        breakRecursiveOre(player, next, tool, visited, limit);
                    }
                }
            }
        }
    }

    // Reuse existing handleAutoSmelt, handleAreaMining etc from the file...
    // (They are effectively kept if I replaced carefully? No, replace_tool replaces
    // lines.
    // I need to be careful not to delete the private helper methods if I am
    // replacing the Constructor + Events)

    // ==========================================
    // COMBAT EFFECTS
    // ==========================================

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        if (!plugin.isMythicItemsEnabled())
            return;
        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        String id = getSkytreeId(weapon);

        if (id == null)
            return;

        // Tin Sword: Speed II on Hit
        if (id.equals("tin_sword")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
        }

        // Aluminum Sword: Lightning
        double aluminumLightningChance = configCache.getChance("aluminum_lightning", 0.10);
        if (id.equals("aluminum_sword") && Math.random() < aluminumLightningChance) {
            event.getEntity().getWorld().strikeLightningEffect(event.getEntity().getLocation());
            event.setDamage(event.getDamage() + 4);
        }

        // Silver Sword: Smite (+Dmg vs Undead) + Weakness
        if (id.equals("silver_sword")) {
            if (isUndead(event.getEntity())) {
                event.setDamage(event.getDamage() + 5); // +5 Dmg (~Smite V)
                if (event.getEntity() instanceof LivingEntity) {
                    ((LivingEntity) event.getEntity())
                            .addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
                }
            }
        }

        // Lead Sword: Poison + Slowness
        if (id.equals("lead_sword")) {
            if (event.getEntity() instanceof LivingEntity) {
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
            }
        }

        // Nickel Sword: Fire
        if (id.equals("nickel_sword")) {
            event.getEntity().setFireTicks(100); // 5s
        }

        // Zinc Sword: Rust Guard? (Handled in Block/Interact usually, or here if we
        // verify blocking?)
        // Plan says "Resist on Block/Hit". Let's give slight resist on hit too?
        if (id.equals("zinc_sword")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1));
        }

        // Brass Sword: Crit
        if (id.equals("brass_sword") && Math.random() < 0.15) {
            event.setDamage(event.getDamage() * 2);
            player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, event.getEntity().getLocation(), 10);
        }

        // Bronze Sword: Bleed
        if (id.equals("bronze_sword")) {
            // Simple bleed: Direct damage? Or Scheduler?
            // Let's just do extra true damage for simplicity or simple DoT via Wither
            // reflavored?
            // "Bleed" usually custom runnable. Let's use Wither as proxy for Bleed
            // visually? or just instant bonus
            // Plan says DOT. Let's use Wither 1 (Hearts decay)
            if (event.getEntity() instanceof LivingEntity) {
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            }
        }

        // Steel Sword: Armor Pierce
        if (id.equals("steel_sword")) {
            // Increase damage to simulate pierce (rough approximation)
            // Or use setDamage(DamageModifier.ARMOR, ...) if API allows.
            event.setDamage(event.getDamage() * 1.25);
        }

        // Uranium Sword: Wither III + Poison
        if (id.equals("uranium_sword")) {
            if (event.getEntity() instanceof LivingEntity) {
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
            }
        }

        // ===================================
        // MIGRATED MYTHIC LOGIC
        // ===================================

        // Mjolnir: Lightning strike (AXE weapon)
        if (id.equals("mjolnir") && Math.random() < 0.20) {
            org.bukkit.Location targetLoc = event.getEntity().getLocation();
            event.getEntity().getWorld().strikeLightningEffect(targetLoc);
            event.setDamage(event.getDamage() + 6.0);
            event.getEntity().setFireTicks(40);
        }

        // Divinebreaker: Lifesteal 10%
        if (id.equals("divinebreaker")) {
            double heal = event.getFinalDamage() * 0.10;
            double max = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            if (player.getHealth() + heal <= max)
                player.setHealth(player.getHealth() + heal);
            else
                player.setHealth(max);
        }

        // Vanguard Blade: Lifesteal 2%
        if (id.equals("vanguard_blade")) {
            double heal = event.getFinalDamage() * 0.02;
            double max = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            if (player.getHealth() + heal <= max)
                player.setHealth(player.getHealth() + heal);
            else
                player.setHealth(max);
        }
    }

    private boolean isUndead(org.bukkit.entity.Entity e) {
        if (!(e instanceof LivingEntity))
            return false;
        org.bukkit.entity.EntityType type = e.getType();
        return type == org.bukkit.entity.EntityType.ZOMBIE ||
                type == org.bukkit.entity.EntityType.SKELETON ||
                type == org.bukkit.entity.EntityType.WITHER_SKELETON ||
                type == org.bukkit.entity.EntityType.STRAY ||
                type == org.bukkit.entity.EntityType.HUSK ||
                type == org.bukkit.entity.EntityType.PHANTOM ||
                type == org.bukkit.entity.EntityType.DROWNED ||
                type == org.bukkit.entity.EntityType.ZOMBIFIED_PIGLIN;
    }

    // Keep helpers from original file...
    // IMPORTANT: I am replacing lines 37 to 506. I must include the helper methods
    // I didn't reimplement or want to keep.
    // The previous implementation had `handleAutoSmelt`, `handleFortune`,
    // `handleAreaMining`, `handleVeinMining`, `breakRecursive`.
    // My new `onBlockBreak` calls `handleAreaMining` and `handleVeinMining` and
    // `handleAutoSmelt`.
    // I NEED TO INCLUDE THEM IN THE REPLACEMENT CONTENT or they will be lost.

    private void handleAutoSmelt(BlockBreakEvent event, int fortuneLevel) {
        if (event.isDropItems()) {
            event.setDropItems(false);
            Block block = event.getBlock();
            ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

            ItemStack tempTool = tool.clone();
            if (fortuneLevel > 0) {
                ItemMeta tempMeta = tempTool.getItemMeta();
                if (tempMeta != null) {
                    tempMeta.addEnchant(org.bukkit.enchantments.Enchantment.FORTUNE, fortuneLevel, true);
                    tempTool.setItemMeta(tempMeta);
                }
            }

            for (ItemStack drop : block.getDrops(tempTool)) {
                ItemStack smelted = null;
                Material dropType = drop.getType();
                if (dropType == Material.IRON_ORE || dropType == Material.DEEPSLATE_IRON_ORE
                        || dropType == Material.RAW_IRON)
                    smelted = new ItemStack(Material.IRON_INGOT, drop.getAmount());
                else if (dropType == Material.GOLD_ORE || dropType == Material.DEEPSLATE_GOLD_ORE
                        || dropType == Material.RAW_GOLD)
                    smelted = new ItemStack(Material.GOLD_INGOT, drop.getAmount());
                else if (dropType == Material.COPPER_ORE || dropType == Material.DEEPSLATE_COPPER_ORE
                        || dropType == Material.RAW_COPPER)
                    smelted = new ItemStack(Material.COPPER_INGOT, drop.getAmount());
                else if (dropType == Material.ANCIENT_DEBRIS)
                    smelted = new ItemStack(Material.NETHERITE_SCRAP, drop.getAmount());
                else if (dropType == Material.SAND)
                    smelted = new ItemStack(Material.GLASS, drop.getAmount());
                else if (dropType == Material.CLAY)
                    smelted = new ItemStack(Material.TERRACOTTA, drop.getAmount());
                else if (dropType == Material.CACTUS)
                    smelted = new ItemStack(Material.GREEN_DYE, drop.getAmount());
                else if (dropType == Material.WET_SPONGE)
                    smelted = new ItemStack(Material.SPONGE, drop.getAmount());
                else if (dropType == Material.STONE)
                    smelted = new ItemStack(Material.SMOOTH_STONE, drop.getAmount());
                else if (dropType == Material.COBBLESTONE)
                    smelted = new ItemStack(Material.STONE, drop.getAmount());
                else if (dropType.name().contains("LOG"))
                    smelted = new ItemStack(Material.CHARCOAL, drop.getAmount()); // Added charcoal for Nickel logic

                if (smelted != null) {
                    // Apply Fortune to smelted count if acceptable? Usually AutoSmelt + Fortune
                    // works.
                    // The loop already used fortune tool for drops.
                    block.getWorld().dropItemNaturally(block.getLocation(), smelted);
                } else {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }
    }

    private void handleAreaMining(BlockBreakEvent event, int radius) {
        if (event.isCancelled())
            return;
        Block center = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (player.hasMetadata("is_area_mining"))
            return;
        player.setMetadata("is_area_mining", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        processingAOE.set(true);
        try {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x == 0 && y == 0 && z == 0)
                            continue;
                        Block target = center.getRelative(x, y, z);
                        if (target.getType() == Material.AIR || target.getType() == Material.BEDROCK)
                            continue;

                        // Protection check
                        if (plugin.getIslandService() != null
                                && !plugin.getIslandService().canModify(player, target.getLocation())) {
                            continue;
                        }

                        if (!target.getDrops(tool).isEmpty()) {
                            target.breakNaturally(tool);
                        }
                    }
                }
            }
        } finally {
            processingAOE.set(false);
        }
        player.removeMetadata("is_area_mining", plugin);
    }

    private void handleVeinMining(BlockBreakEvent event) { // For Logs
        Block start = event.getBlock();
        if (!isLog(start.getType()))
            return;
        Player player = event.getPlayer();
        if (player.hasMetadata("is_vein_mining"))
            return;
        player.setMetadata("is_vein_mining", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        processingAOE.set(true);
        try {
            breakRecursive(player, start, player.getInventory().getItemInMainHand(), new HashSet<>(), 500);
        } finally {
            processingAOE.set(false);
        }
        player.removeMetadata("is_vein_mining", plugin);
    }

    private void breakRecursive(Player player, Block block, ItemStack tool, Set<Block> visited, int limit) {
        if (visited.size() >= limit)
            return;
        if (visited.contains(block))
            return;

        // Protection check
        if (plugin.getIslandService() != null && !plugin.getIslandService().canModify(player, block.getLocation())) {
            return;
        }

        visited.add(block);
        if (block.getType() != Material.AIR) {
            block.breakNaturally(tool);
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    Block next = block.getRelative(x, y, z);
                    if (isLog(next.getType()) && !visited.contains(next)) {
                        breakRecursive(player, next, tool, visited, limit);
                    }
                }
            }
        }
    }

    private boolean isLog(Material mat) {
        return mat.name().endsWith("_LOG") || mat.name().endsWith("_WOOD") || mat.name().endsWith("_STEM")
                || mat.name().endsWith("_HYPHAE");
    }

    public void checkFlightNormalized(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR)
            return;

        ItemStack chest = p.getInventory().getChestplate();
        boolean hasWings = false;
        if (chest != null && chest.hasItemMeta()) {
            String id = getSkytreeId(chest);
            if ("god_wings".equals(id) || "GODWINGS".equals(id))
                hasWings = true;
        }

        // Grant flight globally while wearing god wings
        if (hasWings) {
            if (!p.getAllowFlight()) {
                p.setAllowFlight(true);
                p.setFlying(true); // Auto-fly when worn
                playerFlightCache.put(p.getUniqueId(), true);
            }
        } else {
            // Check metadata/staff mode before disabling
            if (p.hasMetadata("skytree_fly") || p.hasMetadata("staff_mode")) {
                return;
            }

            if (p.getAllowFlight()) {
                p.setAllowFlight(false);
                p.setFlying(false);
                playerFlightCache.remove(p.getUniqueId());
            }
        }
    }

    // Keep Flight Toggle
    @EventHandler
    public void onFlightToggle(PlayerToggleFlightEvent event) {
    }
}
