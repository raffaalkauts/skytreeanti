package com.wiredid.skytree.impl;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.IslandService;
import com.wiredid.skytree.api.PersistenceService;
import com.wiredid.skytree.model.Island;
import com.wiredid.skytree.model.IslandMember;
import com.wiredid.skytree.model.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Island Service
 */
public class SkytreeIslandService implements IslandService {

    private final SkytreePlugin plugin;
    private final PersistenceService persistence;
    private final Map<UUID, Island> islandCache;
    private final Map<String, Island> gridIdCache;
    // New cache: Maps member UUID to Island Owner UUID for fast lookup
    private final Map<UUID, UUID> memberToOwnerCache;
    // Maps Invitee UUID -> Island ID (Pending Invites)
    private final Map<UUID, IslandInvite> pendingInvites;
    private final Map<UUID, Integer> islandLevelCache = new ConcurrentHashMap<>();

    private record IslandInvite(UUID islandId, long timestamp) {
    }

    private int getGridSpacing() {
        return plugin.getConfig().getInt("island.distance", 500);
    }

    private long getInviteExpiry() {
        return plugin.getConfig().getLong("island.invite_expiry_ms", 60000);
    }

    public SkytreeIslandService(SkytreePlugin plugin, PersistenceService persistence) {
        this.plugin = plugin;
        this.persistence = persistence;
        this.islandCache = new ConcurrentHashMap<>();
        this.gridIdCache = new ConcurrentHashMap<>();
        this.memberToOwnerCache = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();

        startLevelScanningTask();
        startInviteCleanupTask();
    }

    private void startInviteCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            pendingInvites.entrySet().removeIf(entry -> now - entry.getValue().timestamp() > getInviteExpiry());
        }, 200L, 200L); // Every 10 seconds
    }

    private void startLevelScanningTask() {
        long interval = plugin.getConfig().getLong("performance.island-level-interval", 12000L); // 10 min
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Island island : islandCache.values()) {
                calculateLevelAsync(island);
            }
        }, 100L, interval);
    }

    private void calculateLevelAsync(Island island) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location center = island.getCenter();
            int radius = island.getSize() / 2;
            World world = center.getWorld();
            if (world == null || !world.isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4))
                return;

            // Run on main thread - world access must be synchronous
            long totalPoints = 0;
            int minX = center.getBlockX() - radius;
            int maxX = center.getBlockX() + radius;
            int minZ = center.getBlockZ() - radius;
            int maxZ = center.getBlockZ() + radius;
            int minHeight = world.getMinHeight();
            int maxHeight = world.getMaxHeight();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minHeight; y < maxHeight; y++) {
                        org.bukkit.Material type = world.getBlockAt(x, y, z).getType();
                        if (type != org.bukkit.Material.AIR) {
                            totalPoints += com.wiredid.skytree.util.BlockValueCalculator.getBlockValue(type);
                        }
                    }
                }
            }

            // Final calculation: (blocks / 100) + (members * 10) + (upgrades * 50)
            int calculatedLevel = (int) (totalPoints / 100) + (island.getMembers().size() * 10);

            // Add upgrade bonuses if any
            for (int upgradeLevel : island.getUpgrades().values()) {
                calculatedLevel += (upgradeLevel * 50);
            }

            islandLevelCache.put(island.getOwnerUUID(), calculatedLevel);
            island.setLevel(calculatedLevel);
        });
    }

    @Override
    public int getIslandLevel(UUID ownerId) {
        return islandLevelCache.getOrDefault(ownerId, 0);
    }

    @Override
    public Map<UUID, Integer> getAllIslandLevels() {
        return Collections.unmodifiableMap(islandLevelCache);
    }

    @Override
    public int getNextIslandId() {
        return persistence.getNextIslandId();
    }

    @Override
    public boolean hasIsland(UUID playerUUID) {
        return getIsland(playerUUID).isPresent();
    }

    @Override
    public boolean canModify(Player player, org.bukkit.Location location) {
        Optional<Island> optIsland = getIslandAtLocation(location);
        if (optIsland.isEmpty())
            return true; // Wilderness/Spawn protection handled elsewhere or non-island world
        return optIsland.get().isMember(player.getUniqueId());
    }

    @Override
    public Optional<Island> getIsland(UUID playerUUID) {
        // 1. Check if owner in cache
        if (islandCache.containsKey(playerUUID)) {
            return Optional.of(islandCache.get(playerUUID));
        }

        // 2. Check if member in cache
        if (memberToOwnerCache.containsKey(playerUUID)) {
            UUID ownerId = memberToOwnerCache.get(playerUUID);
            if (islandCache.containsKey(ownerId)) {
                Island island = islandCache.get(ownerId);
                // VERIFY membership (prevents stale cache issues after kick)
                if (island.isMember(playerUUID)) {
                    return Optional.of(island);
                } else {
                    // Stale cache detected, remove it
                    memberToOwnerCache.remove(playerUUID);
                }
            }
        }

        // 3. Try load from persistence (as owner)
        Optional<Island> loaded = persistence.loadIsland(playerUUID);
        if (loaded.isPresent()) {
            cacheIsland(loaded.get());
            return loaded;
        }

        // 4. Fallback search in cache for members
        for (Island is : islandCache.values()) {
            if (is.isMember(playerUUID)) {
                memberToOwnerCache.put(playerUUID, is.getOwnerUUID());
                return Optional.of(is);
            }
        }

        return Optional.empty();
    }

    @Override
    public void inviteMember(Player inviter, Player target) {
        Optional<Island> optIsland = getIsland(inviter.getUniqueId());
        if (optIsland.isEmpty()) {
            inviter.sendMessage("§c§l[Skytree] §cYou must have an island to invite others!");
            return;
        }

        Island island = optIsland.get();
        if (!island.getOwnerUUID().equals(inviter.getUniqueId())) {
            // Check co-owner logic elsewhere, or assume only owners/co-owners trigger this
            // We trust the command logic to verify permissions, but strict check here
            // prevents weirdness
            if (island.getRole(inviter.getUniqueId()) != IslandRole.CO_OWNER) {
                inviter.sendMessage("§c§l[Skytree] §cOnly owners and co-owners can invite!");
                return;
            }
        }

        if (island.isMember(target.getUniqueId())) {
            inviter.sendMessage("§c" + target.getName() + " is already a member!");
            return;
        }

        // Add to pending
        pendingInvites.put(target.getUniqueId(), new IslandInvite(island.getIslandId(), System.currentTimeMillis()));

        // Notify
        inviter.sendMessage("§a§l[Skytree] §aInvite sent to " + target.getName() + "!");
        target.sendMessage("§a§l[Skytree] §aYou have been invited to join " + inviter.getName() + "'s island!");
        target.sendMessage("§eType §6/is join §eto accept!");
        target.sendMessage("§7(This invite expires in 60 seconds)");
    }

    @Override
    public void acceptInvite(Player player) {
        if (!pendingInvites.containsKey(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cYou have no pending invites or it has expired!");
            return;
        }

        IslandInvite invite = pendingInvites.get(player.getUniqueId());
        Optional<Island> optIsland = getIslandById(invite.islandId());

        if (optIsland.isEmpty()) {
            player.sendMessage("§c§l[Skytree] §cThe island no longer exists!");
            pendingInvites.remove(player.getUniqueId());
            return;
        }

        Island island = optIsland.get();

        // Check member limit
        int rankBonus = plugin.getRankService().getMemberBonus(island.getOwnerUUID());
        int totalLimit = island.getMemberLimit() + rankBonus;
        if (island.getMembers().size() + 1 >= totalLimit) {
            player.sendMessage(
                    "§c§l[Skytree] §cThis island has reached its maximum member limit! (§e" + totalLimit + "§c)");
            player.sendMessage("§7Upgrade your island or your Rank to invite more members.");
            pendingInvites.remove(player.getUniqueId());
            return;
        }

        // Check if player has their own island
        if (hasIsland(player.getUniqueId())) {
            Optional<Island> myIsland = getIsland(player.getUniqueId());
            if (myIsland.isPresent() && myIsland.get().getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage("§c§l[Skytree] §cYou cannot join another island while you own one!");
                player.sendMessage("§cType §e/is delete §cto remove your current island first.");
                return;
            }
            // If they are just a member of another island, we should remove them from
            // there?
            // Or deny? Usually one island per player.
            // Let's remove them from old island if they accept.
            Island oldIsland = myIsland.get();
            oldIsland.removeMember(player.getUniqueId());
            persistence.saveIsland(oldIsland);
            // Clear cache
            memberToOwnerCache.remove(player.getUniqueId());
            player.sendMessage("§e§l[Skytree] §eLeft your previous island.");
        }

        // Add to new
        island.addMember(player.getUniqueId(), IslandRole.MEMBER);
        persistence.saveIsland(island);
        cacheIsland(island); // Updates member cache

        pendingInvites.remove(player.getUniqueId());

        player.sendMessage("§a§l[Skytree] §aYou have joined the island!");
        player.teleport(island.getCenter().clone().add(0, 2, 0)); // Teleport to island

        Player owner = Bukkit.getPlayer(island.getOwnerUUID());
        if (owner != null) {
            owner.sendMessage("§a§l[Skytree] §a" + player.getName() + " has joined your island!");
        }
    }

    @Override
    public void denyInvite(Player player) {
        if (!pendingInvites.containsKey(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cYou have no pending invites!");
            return;
        }
        pendingInvites.remove(player.getUniqueId());
        player.sendMessage("§c§l[Skytree] §cInvite declined.");
    }

    // Helper to cache an island and its members
    public void cacheIsland(Island island) {
        islandCache.put(island.getOwnerUUID(), island);
        gridIdCache.put(island.getGridId(), island);
        for (IslandMember m : island.getMembers()) {
            memberToOwnerCache.put(m.getUuid(), island.getOwnerUUID());
        }
    }

    @Override
    public Optional<Island> getIslandAtLocation(Location location) {
        if (location == null)
            return Optional.empty();

        // Calculate grid ID from location
        int spacing = getGridSpacing();
        int x = (int) Math.round((double) location.getBlockX() / spacing);
        int z = (int) Math.round((double) location.getBlockZ() / spacing);
        String gridId = x + ":" + z;

        if (gridIdCache.containsKey(gridId)) {
            return Optional.of(gridIdCache.get(gridId));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Island> getIslandById(UUID islandId) {
        for (Island island : islandCache.values()) {
            if (island.getIslandId().equals(islandId)) {
                return Optional.of(island);
            }
        }
        // Fallback: Check all? Persistence doesn't have loadById implies strict
        // owner-based lookup
        return Optional.empty();
    }

    @Override
    public void createIsland(Player player) {
        if (hasIsland(player.getUniqueId())) {
            player.sendMessage("§c§l[Skytree] §cYou already have an island!");
            return;
        }

        // Spiral search for empty spot
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;
        int maxTries = plugin.getConfig().getInt("island.max_spiral_search_tries", 5000);

        String gridId = "0:0";
        for (int i = 0; i < maxTries; i++) {
            gridId = x + ":" + z;
            if (!gridIdCache.containsKey(gridId)) {
                break;
            }

            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }
            x += dx;
            z += dz;
        }

        // Create
        World world = Bukkit.getWorld(plugin.getConfig().getString("world.name", "skytree_world"));
        if (world == null) {
            // Fallback to default world or try to load
            world = Bukkit.getWorlds().get(0);
        }

        int spacing = getGridSpacing();
        Location center = new Location(world, x * spacing, 100, z * spacing);

        Island island = new Island(player.getUniqueId(), UUID.randomUUID(), center.clone().add(0.5, 4, 0.5), center,
                gridId);
        island.addMember(player.getUniqueId(), IslandRole.OWNER); // Ensure owner is member

        // Ensure chunk is loaded before generating and teleporting
        world.getChunkAt(center).load();

        // Generate Template (Sky Factory)
        generateSkyFactoryTemplate(center);

        // Give Starter items
        Block chestBlock = center.clone().add(0, 4, 1).getBlock();
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            chest.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET));
            chest.getInventory().addItem(new ItemStack(Material.ICE, 2));
            chest.getInventory().addItem(new ItemStack(Material.SUGAR_CANE));
            chest.getInventory().addItem(new ItemStack(Material.BONE_MEAL, 5));
        }

        // Save
        persistence.saveIsland(island);
        cacheIsland(island);

        // Teleport - Above tree with a short delay to ensure block sync
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(center.clone().add(0.5, 12, 0.5));
                player.sendMessage("§a§l[Skytree] §aIsland created!");
                player.sendMessage("§7Type §e/is help §7for commands.");
            }
        }, 5L);

        // Quest Progress
        plugin.getQuestSystem().addProgress(player, com.wiredid.skytree.system.QuestSystem.QuestType.ISLAND_CREATE, 1);
    }

    @Override
    public void deleteIsland(Player player) {
        Optional<Island> opt = getIsland(player.getUniqueId());
        if (opt.isPresent()) {
            if (!opt.get().getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage("§cOnly the owner can delete the island!");
                return;
            }
            // Remove from cache
            Island island = opt.get();
            islandCache.remove(island.getOwnerUUID());
            gridIdCache.remove(island.getGridId());
            for (IslandMember m : island.getMembers()) {
                memberToOwnerCache.remove(m.getUuid());
            }
            memberToOwnerCache.remove(island.getOwnerUUID());

            // Clear area
            clearIslandArea(island);

            // Remove from persistence
            persistence.deleteIsland(island.getOwnerUUID());

            player.sendMessage("§c§l[Skytree] §cIsland deleted!");
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        } else {
            player.sendMessage("§cYou don't have an island!");
        }
    }

    private void clearIslandArea(Island island) {
        Location center = island.getCenter();
        int size = island.getSize();
        int radius = size / 2;
        World world = center.getWorld();
        if (world == null)
            return;

        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        // Perform chunked deletion to prevent lag
        int blocksPerTick = 10000; // Adjust as needed

        new BukkitRunnable() {
            int currentX = minX;
            int currentZ = minZ;
            int currentY = minY;

            @Override
            public void run() {
                int processed = 0;
                while (processed < blocksPerTick) {
                    Block block = world.getBlockAt(currentX, currentY, currentZ);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR, false);
                    }
                    processed++;

                    currentY++;
                    if (currentY >= maxY) {
                        currentY = minY;
                        currentZ++;
                        if (currentZ > maxZ) {
                            currentZ = minZ;
                            currentX++;
                            if (currentX > maxX) {
                                cancel();
                                return;
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void teleportHome(Player player) {
        Optional<Island> opt = getIsland(player.getUniqueId());
        if (opt.isPresent()) {
            Location loc = opt.get().getSpawnLocation();
            if (loc == null)
                loc = opt.get().getCenter().clone().add(0.5, 4, 0.5); // Top of dirt
            player.teleport(loc);
            player.sendMessage("§a§l[Skytree] §aTeleported home!");
        } else {
            player.sendMessage("§a§l[Skytree] §7Creating your island...");
            createIsland(player);
        }
    }

    /**
     * Generates SkyFactory 3 template:
     * - 1 Bedrock at Y
     * - 3 Dirt above
     * - Oak Tree on top
     */
    private void generateSkyFactoryTemplate(Location center) {
        World world = center.getWorld();
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();

        // 1. Base Column
        world.getBlockAt(x, y, z).setType(Material.BEDROCK);
        world.getBlockAt(x, y + 1, z).setType(Material.DIRT);
        world.getBlockAt(x, y + 2, z).setType(Material.DIRT);
        world.getBlockAt(x, y + 3, z).setType(Material.DIRT);

        // 2. Generate Oak Tree
        // Use Bukkit's tree generator for simplicity, or manual structure
        Location treeLoc = new Location(world, x, y + 4, z);
        boolean grown = world.generateTree(treeLoc, org.bukkit.TreeType.TREE);
        if (!grown) {
            // Fallback manual tree
            world.getBlockAt(x, y + 4, z).setType(Material.OAK_LOG);
            world.getBlockAt(x, y + 5, z).setType(Material.OAK_LOG);
            world.getBlockAt(x, y + 6, z).setType(Material.OAK_LOG);
            // Leaves...
            world.getBlockAt(x, y + 7, z).setType(Material.OAK_LEAVES);
        }
    }

    @Override
    public Collection<Island> getLoadedIslands() {
        return Collections.unmodifiableCollection(islandCache.values());
    }

    @Override
    public void saveIsland(Island island) {
        persistence.saveIsland(island);
    }

    @Override
    public void trustPlayer(Island island, UUID playerUUID, com.wiredid.skytree.model.TrustLevel level) {
        island.trustPlayer(playerUUID, level);
        saveIsland(island);
    }

    @Override
    public void untrustPlayer(Island island, UUID playerUUID) {
        island.untrustPlayer(playerUUID);
        saveIsland(island);
    }

    @Override
    public com.wiredid.skytree.model.TrustLevel getTrustLevel(Island island, UUID playerUUID) {
        return island.getTrustLevel(playerUUID);
    }
}
