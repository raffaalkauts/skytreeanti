package com.wiredid.skytree.listener;

import com.wiredid.skytree.SkytreePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.Optional;

public class SpawnerListener implements Listener {

    private final NamespacedKey stackKey;
    private final NamespacedKey mobStackKey;
    private final NamespacedKey itemAmountKey;
    private final SkytreePlugin plugin;
    private static final ThreadLocal<Boolean> processingDeath = ThreadLocal.withInitial(() -> false);

    public SpawnerListener(SkytreePlugin plugin) {
        this.plugin = plugin;
        this.stackKey = new NamespacedKey(plugin, "spawner_stack");
        this.mobStackKey = new NamespacedKey(plugin, "mob_stack");
        this.itemAmountKey = new NamespacedKey(plugin, "item_amount");
    }

    // Spawner Stacking Logic
    @EventHandler(ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        // Check for adjacent spawners to stack
        Block placed = event.getBlock();
        if (placed.getType() != Material.SPAWNER)
            return;

        // Spawner Limit Check
        Optional<com.wiredid.skytree.model.Island> optIsland = plugin.getIslandService()
                .getIslandAtLocation(placed.getLocation());
        if (optIsland.isPresent()) {
            com.wiredid.skytree.model.Island island = optIsland.get();
            int count = getOrCalculateSpawnerCount(island);
            int rankBonus = plugin.getRankService().getSpawnerBonus(island.getOwnerUUID());
            int totalLimit = island.getSpawnerLimit() + rankBonus;

            if (count >= totalLimit) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c§l[Skytree] §cYour island has reached its spawner limit! (§e" + count
                        + "/" + totalLimit + "§c)");
                event.getPlayer().sendMessage("§7Upgrade your spawner limit or your Rank in §e/is upgrade§7!");
                return;
            }
            // Temporarily increment - will be re-synced if needed
            island.setSpawnerCount(count + 1);
        }

        CreatureSpawner placedState = (CreatureSpawner) placed.getState();

        // Handle stack size from item (if it was broken with stack)
        ItemStack hand = event.getItemInHand();
        int itemStackSize = 1;
        org.bukkit.entity.EntityType placedType = null;
        if (hand.hasItemMeta() && hand.getItemMeta() instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof CreatureSpawner metaSpawner) {
                placedType = metaSpawner.getSpawnedType();
                itemStackSize = metaSpawner.getPersistentDataContainer().getOrDefault(stackKey,
                        PersistentDataType.INTEGER, 1);
            }
        }

        if (placedType == null) {
            placedType = placedState.getSpawnedType();
        }

        if (placedType == null)
            return;

        // Bulk Placement Logic (Shift + Right Click)
        if (event.getPlayer().isSneaking()) {
            int totalToStack = 0;
            org.bukkit.inventory.PlayerInventory inv = event.getPlayer().getInventory();
            ItemStack[] contents = inv.getContents();

            for (int i = 0; i < contents.length; i++) {
                ItemStack is = contents[i];
                if (is == null || is.getType() != Material.SPAWNER)
                    continue;

                // Check if it's the same type
                if (is.getItemMeta() instanceof BlockStateMeta bsm &&
                        bsm.getBlockState() instanceof CreatureSpawner invSpawner) {

                    if (invSpawner.getSpawnedType() == placedType) {
                        int stackSize = invSpawner.getPersistentDataContainer().getOrDefault(stackKey,
                                PersistentDataType.INTEGER, 1);
                        totalToStack += (is.getAmount() * stackSize);
                        inv.setItem(i, null); // Consume all
                    }
                } else if (!plugin.getItemRegistry().isCustomItem(is)) {
                    // Vanilla spawner with same type (fallback)
                    if (placedType == org.bukkit.entity.EntityType.PIG) { // Default vanilla is pig
                        totalToStack += is.getAmount();
                        inv.setItem(i, null);
                    }
                }
            }

            if (totalToStack > 0) {
                // If we merged with a neighbor, add to it. Otherwise add to placed.
                // But neighbors check already happened above (if we returned).
                // Wait, if we are here, no neighbor merged. So we are placing a NEW block.
                itemStackSize = totalToStack;
            }
        }

        // Check faces (Up, Down, North, South, East, West)
        Block[] neighbors = new Block[] {
                placed.getRelative(org.bukkit.block.BlockFace.UP),
                placed.getRelative(org.bukkit.block.BlockFace.DOWN),
                placed.getRelative(org.bukkit.block.BlockFace.NORTH),
                placed.getRelative(org.bukkit.block.BlockFace.SOUTH),
                placed.getRelative(org.bukkit.block.BlockFace.EAST),
                placed.getRelative(org.bukkit.block.BlockFace.WEST)
        };

        for (Block neighbor : neighbors) {
            if (neighbor.getType() == Material.SPAWNER) {
                if (neighbor.getState() instanceof CreatureSpawner neighborSpawner) {
                    if (neighborSpawner.getSpawnedType() == placedType) {
                        // Merge!
                        // Check setting
                        Optional<com.wiredid.skytree.model.Island> optIslandInner = plugin.getIslandService()
                                .getIslandAtLocation(placed.getLocation());
                        if (optIslandInner.isPresent() && !plugin.getIslandSettingsService()
                                .getSetting(optIslandInner.get(), "spawner_stacking")) {
                            // Stacking disabled, do not merge. Treat as separate.
                            return; // Skip this neighbor, maybe check others (but logic implies only one check per
                                    // face?)
                            // Actually if one fails, we might just stop or continue.
                            // But standard behavior is: if stacking disabled, don't stack.
                        }

                        // Proceed with merge
                        int currentStack = neighborSpawner.getPersistentDataContainer().getOrDefault(stackKey,
                                PersistentDataType.INTEGER, 1);
                        int newStack = currentStack + itemStackSize;

                        neighborSpawner.getPersistentDataContainer().set(stackKey, PersistentDataType.INTEGER,
                                newStack);
                        neighborSpawner.setSpawnedType(placedType); // Re-enforce type
                        updateSpawnerStats(neighborSpawner, newStack);

                        // Remove placed block logic
                        event.setCancelled(true);
                        hand.setAmount(hand.getAmount() - 1);
                        event.getPlayer().getInventory().setItemInMainHand(hand);

                        event.getPlayer().sendMessage("§aStacked spawner! (x" + newStack + ")");
                        updateSpawnerNametag(neighbor);
                        return; // Only merge with one
                    }
                }
            }
        }

        // No merge found - set initial stack from item and re-enforce type
        placedState.setSpawnedType(placedType);
        placedState.getPersistentDataContainer().set(stackKey, PersistentDataType.INTEGER, itemStackSize);
        updateSpawnerStats(placedState, itemStackSize);
        updateSpawnerNametag(placed);
    }

    // Drop logic
    @EventHandler(ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER)
            return;

        if (!(block.getState() instanceof CreatureSpawner spawner))
            return;

        event.setExpToDrop(0);
        event.setDropItems(false); // Cancel vanilla drops (xp/nothing)

        int stackSize = 1;
        PersistentDataContainer pdc = spawner.getPersistentDataContainer();
        if (pdc.has(stackKey, PersistentDataType.INTEGER)) {
            stackSize = pdc.get(stackKey, PersistentDataType.INTEGER);
        }

        // Drop Loop (Split into max 64 stacks)
        int remaining = stackSize;
        while (remaining > 0) {
            int batchSize = Math.min(remaining, 64);

            ItemStack drop = new ItemStack(Material.SPAWNER, batchSize);
            BlockStateMeta meta = (BlockStateMeta) drop.getItemMeta();
            CreatureSpawner metaSpawner = (CreatureSpawner) meta.getBlockState();

            metaSpawner.setSpawnedType(spawner.getSpawnedType());
            // Important: Each PHYSICAL item represents 1 spawner stack value
            metaSpawner.getPersistentDataContainer().set(stackKey, PersistentDataType.INTEGER, 1);

            meta.setBlockState(metaSpawner);

            // Cosmetic name
            String mobName = spawner.getSpawnedType().name().charAt(0)
                    + spawner.getSpawnedType().name().substring(1).toLowerCase().replace("_", " ");
            meta.displayName(Component.text("§e" + mobName + " Spawner"));
            meta.lore(new java.util.ArrayList<>());
            drop.setItemMeta(meta);

            block.getWorld().dropItemNaturally(block.getLocation(), drop);

            remaining -= batchSize;
        }

        // Remove nametag
        removeSpawnerNametag(block);

        // Update count
        plugin.getIslandService().getIslandAtLocation(block.getLocation()).ifPresent(island -> {
            int current = island.getSpawnerCount();
            if (current > 0)
                island.setSpawnerCount(current - 1);
        });
    }

    // Mob Stacking
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER)
            return;

        // Check nearby entities to stack
        LivingEntity spawned = event.getEntity();

        Optional<com.wiredid.skytree.model.Island> optIsland = plugin.getIslandService()
                .getIslandAtLocation(spawned.getLocation());
        if (optIsland.isPresent() && !plugin.getIslandSettingsService().getSetting(optIsland.get(), "mob_stacking")) {
            return; // Stacking disabled
        }

        double range = 5.0; // Stack range

        Optional<LivingEntity> nearbyStack = spawned.getNearbyEntities(range, range, range).stream()
                .filter(e -> e.getType() == spawned.getType())
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(e -> !e.isDead())
                .findFirst();

        if (nearbyStack.isPresent()) {
            LivingEntity stackBase = nearbyStack.get();
            int currentStack = getMobStackSize(stackBase);
            // Limit? "Unlimited"

            // Add to stack
            setMobStackSize(stackBase, currentStack + 1);
            updateMobName(stackBase);

            event.setCancelled(true); // Cancel new spawn, integrated into existing
        } else {
            // New stack of 1
            setMobStackSize(spawned, 1);
            updateMobName(spawned);
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (processingDeath.get())
            return;

        LivingEntity entity = event.getEntity();
        int stack = getMobStackSize(entity);

        if (stack > 1) {
            processingDeath.set(true);
            try {
                org.bukkit.event.entity.EntityDamageEvent lastDamage = entity.getLastDamageCause();
                boolean dieToFire = lastDamage != null
                        && (lastDamage.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE ||
                                lastDamage.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK
                                ||
                                lastDamage.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.LAVA ||
                                lastDamage
                                        .getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.HOT_FLOOR);

                if (dieToFire) {
                    // Kill the entire stack and multiply drops
                    for (ItemStack drop : event.getDrops()) {
                        int originalAmount = drop.getAmount();
                        long totalAmount = (long) originalAmount * stack;

                        // Store the true amount in PDC so ItemStackListener can pick it up
                        org.bukkit.inventory.meta.ItemMeta meta = drop.getItemMeta();
                        if (meta != null) {
                            meta.getPersistentDataContainer().set(itemAmountKey, PersistentDataType.LONG, totalAmount);
                            drop.setItemMeta(meta);
                        }

                        // Cap visual amount to 64 to avoid internal server errors during spawn
                        drop.setAmount((int) Math.min(64, totalAmount));
                    }
                    // No replacement spawn
                    return;
                }

                // Normal death (sword etc) -> spawn replacement with stack - 1
                int newSize = stack - 1;
                LivingEntity replacement = (LivingEntity) entity.getWorld().spawnEntity(entity.getLocation(),
                        entity.getType());
                setMobStackSize(replacement, newSize);
                updateMobName(replacement);

                if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    replacement.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                            .setBaseValue(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                }
                replacement.setHealth(replacement.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            } finally {
                processingDeath.set(false);
            }
        }
    }

    private void updateSpawnerStats(CreatureSpawner spawner, int stackSize) {
        // Base delay: 200 ticks (10s), Max delay: 800 ticks (40s)
        // Scale with stack size: more spawners = faster & more mobs
        int minDelay = Math.max(20, 200 / stackSize);
        int maxDelay = Math.max(40, 800 / stackSize);
        int spawnCount = stackSize; // User Request: spawn equal to stack size
        int maxNearby = Math.min(1024, 16 + (stackSize * 2));

        spawner.setMinSpawnDelay(minDelay);
        spawner.setMaxSpawnDelay(maxDelay);
        spawner.setSpawnCount(spawnCount);
        spawner.setMaxNearbyEntities(maxNearby);
        spawner.setRequiredPlayerRange(16);
        spawner.update(true); // Force update
    }

    private void updateSpawnerNametag(Block block) {
        if (!(block.getState() instanceof CreatureSpawner spawner))
            return;
        int stackSize = spawner.getPersistentDataContainer().getOrDefault(stackKey, PersistentDataType.INTEGER, 1);
        if (stackSize <= 1) {
            removeSpawnerNametag(block);
            return;
        }

        String mobName = spawner.getSpawnedType() != null ? spawner.getSpawnedType().name().replace("_", " ")
                : "Unknown";
        String name = "§6" + stackSize + "x §e" + mobName;

        // Use ArmorStand for nametag
        org.bukkit.entity.ArmorStand as = getOrCreateNametag(block);
        as.customName(Component.text(name));
        as.setCustomNameVisible(true);
    }

    private org.bukkit.entity.ArmorStand getOrCreateNametag(Block block) {
        org.bukkit.Location loc = block.getLocation().add(0.5, 1.2, 0.5);
        // Find existing
        for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(loc, 0.2, 0.5, 0.2)) {
            if (entity instanceof org.bukkit.entity.ArmorStand as && !as.isVisible() && as.isMarker()) {
                return as;
            }
        }
        // Create new
        org.bukkit.entity.ArmorStand as = block.getWorld().spawn(loc, org.bukkit.entity.ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.setMarker(true);
        as.setSmall(true);
        return as;
    }

    private void removeSpawnerNametag(Block block) {
        org.bukkit.Location loc = block.getLocation().add(0.5, 1.2, 0.5);
        for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(loc, 0.2, 0.5, 0.2)) {
            if (entity instanceof org.bukkit.entity.ArmorStand as && !as.isVisible() && as.isMarker()) {
                as.remove();
            }
        }
    }

    private int getMobStackSize(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.getOrDefault(mobStackKey, PersistentDataType.INTEGER, 1);
    }

    private void setMobStackSize(LivingEntity entity, int size) {
        entity.getPersistentDataContainer().set(mobStackKey, PersistentDataType.INTEGER, size);
    }

    private void updateMobName(LivingEntity entity) {
        int size = getMobStackSize(entity);
        if (size > 1) {
            entity.customName(Component.text("§6" + size + "x §e" + entity.getType().name()));
            entity.setCustomNameVisible(true);
        } else {
            entity.setCustomNameVisible(false); // Hide if 1? Or keep name?
        }
    }

    private int getOrCalculateSpawnerCount(com.wiredid.skytree.model.Island island) {
        if (island.getSpawnerCount() != -1) {
            return island.getSpawnerCount();
        }

        // Calculate
        int count = 0;
        org.bukkit.Location min = island.getMin();
        org.bukkit.Location max = island.getMax();
        org.bukkit.World world = island.getCenter().getWorld();
        if (world == null)
            return 0;

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                // Check all Y levels? This is slow.
                // In Skyblock, players usually place spawners in a specific range.
                // For now, let's scan a reasonable Y range (0 to 255) to be safe but
                // potentially slow once.
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 16) {
                    // Optimized check: check if chunk has spawners? Bukkit doesn't have easy way.
                    // We'll just check at 16 block intervals for chunks and then scan? No.
                }
                // Actually, let's just scan all Y.
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.SPAWNER) {
                        count++;
                    }
                }
            }
        }
        island.setSpawnerCount(count);
        return count;
    }
}
