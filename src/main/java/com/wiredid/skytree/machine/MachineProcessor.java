package com.wiredid.skytree.machine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.api.RecipeService;

/**
 * Handles automatic machine processing
 */
public class MachineProcessor {

    public enum MachineType {
        SIEVE, BARREL, CRUCIBLE, COBBLE_GEN, ADVANCED_FURNACE, COMPRESSOR, JUICER, PRIMORDIAL_SEAL,
        POWERED_SPAWNER, COBBLE_MINION, AUTO_CRAFTER
    }

    private final SkytreePlugin plugin;
    private final RecipeService recipeService;
    // Thresholds for each machine in server ticks (1 tick = 50ms). Filled from
    // config.
    private final Map<MachineType, Double> thresholdsTicks = new EnumMap<>(MachineType.class);

    // Active machines: Location -> MachineData (Thread-safe)
    private final Map<Location, MachineData> activeMachines = new ConcurrentHashMap<>();

    public MachineProcessor(SkytreePlugin plugin, RecipeService recipeService) {
        this.plugin = plugin;
        this.recipeService = recipeService;
        initDurationsFromConfig();
        loadTierConfigs(); // New
        loadMachines();
        startProcessingTask();
    }

    public void reload() {
        initDurationsFromConfig();
        loadTierConfigs();
    }

    private final Map<Integer, TierConfig> tierConfigs = new HashMap<>();

    public static class TierConfig {
        public double speed;
        public double efficiency;
        public double capacity;

        public TierConfig(double speed, double efficiency, double capacity) {
            this.speed = speed;
            this.efficiency = efficiency;
            this.capacity = capacity;
        }
    }

    private void loadTierConfigs() {
        File file = new File(plugin.getDataFolder(), "machine_upgrades.yml");
        if (!file.exists()) {
            plugin.saveResource("machine_upgrades.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection tiersSec = config.getConfigurationSection("tiers");
        if (tiersSec != null) {
            for (String key : tiersSec.getKeys(false)) {
                try {
                    int tier = Integer.parseInt(key);
                    ConfigurationSection sec = tiersSec.getConfigurationSection(key);
                    tierConfigs.put(tier, new TierConfig(
                            sec.getDouble("speed", 1.0),
                            sec.getDouble("efficiency", 1.0),
                            sec.getDouble("capacity", 1.0)));
                } catch (Exception e) {
                }
            }
        }
    }

    public TierConfig getTierConfig(int tier) {
        return tierConfigs.getOrDefault(tier, new TierConfig(1.0, 1.0, 1.0));
    }

    private void initDurationsFromConfig() {
        // Helper to read seconds from config and convert to ticks (1s = 20 ticks)
        thresholdsTicks.put(MachineType.POWERED_SPAWNER,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.powered_spawner_seconds", 5.0)));
        thresholdsTicks.put(MachineType.SIEVE,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.sieve_seconds", 10.0)));
        thresholdsTicks.put(MachineType.BARREL,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.barrel_seconds", 20.0)));
        thresholdsTicks.put(MachineType.CRUCIBLE,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.crucible_seconds", 33.333)));
        thresholdsTicks.put(MachineType.COBBLE_GEN,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.cobble_gen_seconds", 5.0)));
        thresholdsTicks.put(MachineType.COBBLE_MINION,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.cobble_minion_seconds", 5.0)));
        thresholdsTicks.put(MachineType.COMPRESSOR,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.compressor_seconds", 10.0)));
        thresholdsTicks.put(MachineType.JUICER,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.juicer_seconds", 10.0)));
        thresholdsTicks.put(MachineType.ADVANCED_FURNACE,
                secondsToTicks(plugin.getConfig().getDouble("machines.durations.advanced_furnace_seconds", 0.5)));
    }

    private static double secondsToTicks(double seconds) {
        return seconds * 20.0;
    }

    public void registerMachine(Location location, MachineType type) {
        activeMachines.put(location, new MachineData(type));
    }

    public void unregisterMachine(Location location) {
        activeMachines.remove(location);
    }

    public boolean isMachineType(Location location, MachineType type) {
        MachineData data = activeMachines.get(location);
        return data != null && data.type == type;
    }

    private void startProcessingTask() {
        // Make the machine processing interval configurable via plugin config (in
        // ticks).
        // Default: 20 ticks (1 second)
        long interval = 20L;
        try {
            // Preferred new key
            if (plugin.getConfig().contains("machines.tick-interval-ticks")) {
                interval = plugin.getConfig().getLong("machines.tick-interval-ticks", 20L);
            }
            // Fallback to legacy key `machines.tick_interval` for older configs
            else if (plugin.getConfig().contains("machines.tick_interval")) {
                interval = plugin.getConfig().getLong("machines.tick_interval", 20L);
            } else {
                interval = plugin.getConfig().getLong("machines.tick-interval-ticks", 20L);
            }

            if (interval < 1) // ensure sensible minimum
                interval = 1L;
        } catch (Exception ignored) {
            interval = 20L;
        }

        long finalInterval = interval;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, MachineData> entry : activeMachines.entrySet()) {
                    Location loc = entry.getKey();
                    // lag check: skip if chunk is not loaded
                    if (loc.getWorld() != null
                            && !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        continue;
                    }
                    processMachine(loc, entry.getValue());
                }
            }
        }.runTaskTimer(plugin, finalInterval, finalInterval);
    }

    private void processMachine(Location loc, MachineData data) {

        // Accumulate elapsed time in server 'ticks' (1 tick = 50ms) so thresholds
        // (which historically were written in ticks) remain valid regardless of
        // the scheduler interval. We add fractional ticks to data.progress.
        long now = System.currentTimeMillis();
        double elapsedTicks = (now - data.lastUpdateMs) / 50.0; // ticks
        data.lastUpdateMs = now;

        TierConfig tc = getTierConfig(data.tier);
        data.progress += (elapsedTicks * tc.speed);

        switch (data.type) {
            case SIEVE -> processSieve(loc, data);
            case BARREL -> processBarrel(loc, data);
            case CRUCIBLE -> processCrucible(loc, data);
            case COBBLE_GEN -> processCobbleGen(loc, data);
            case ADVANCED_FURNACE -> processAdvancedFurnace(loc, data);
            case COMPRESSOR -> processCompressor(loc, data);
            case JUICER -> processJuicer(loc, data);
            case POWERED_SPAWNER -> processPoweredSpawner(loc, data);
            case COBBLE_MINION -> processCobbleMinion(loc, data);
            case AUTO_CRAFTER -> processAutoCrafter(loc, data);
            case PRIMORDIAL_SEAL -> {
                /* Passive effect handled in listener */ }
        }
    }

    private void processPoweredSpawner(Location loc, MachineData data) {
        // Spawns mobs every 5 seconds (100 ticks)
        double threshold = thresholdsTicks.getOrDefault(MachineType.POWERED_SPAWNER, 100.0);
        if (data.progress < threshold)
            return;
        data.progress -= threshold;

        // Check for nearby players (activation range)
        if (loc.getWorld().getNearbyEntities(loc, 16, 16, 16).stream()
                .noneMatch(e -> e instanceof org.bukkit.entity.Player)) {
            return;
        }

        // Spawn logic
        String mobType = (data.meta != null && !data.meta.isEmpty()) ? data.meta : "ZOMBIE";
        try {
            org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(mobType);

            // Random point in radius 4
            double x = loc.getX() + (Math.random() * 8) - 4;
            double z = loc.getZ() + (Math.random() * 8) - 4;
            double y = loc.getY(); // Same level

            // Should verify valid spawn location, but simplistic for now
            Location spawnLoc = new Location(loc.getWorld(), x, y, z);

            // Limit mob count?
            // Simple check: don't spawn if too many nearby
            if (loc.getWorld().getNearbyEntities(spawnLoc, 5, 5, 5).stream().filter(e -> e.getType() == type)
                    .count() < 6) {
                loc.getWorld().spawnEntity(spawnLoc, type);
                loc.getWorld().playEffect(loc, org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
            }
        } catch (IllegalArgumentException e) {
            // Invalid mob type stored
        }
    }

    private void processJuicer(Location loc, MachineData data) {
        double threshold = thresholdsTicks.getOrDefault(MachineType.JUICER, 200.0);
        if (data.progress < threshold)
            return;
        data.progress -= threshold;

        // Juicer Logic: Chest/Hopper above with Fruit -> Output Juice below?
        // Or specific block interaction.
        // Assuming Input Block Above (Container) -> Output Block Below (Container)

        Block inputBlock = loc.clone().add(0, 1, 0).getBlock();
        if (!(inputBlock.getState() instanceof org.bukkit.inventory.InventoryHolder))
            return;

        org.bukkit.inventory.Inventory inputInv = ((org.bukkit.inventory.InventoryHolder) inputBlock.getState())
                .getInventory();

        for (int i = 0; i < inputInv.getSize(); i++) {
            ItemStack stack = inputInv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR)
                continue;

            ItemStack result = null;
            if (stack.getType() == Material.APPLE)
                result = plugin.getItemRegistry().getItem("juice_apple");
            else if (stack.getType() == Material.MELON_SLICE)
                result = plugin.getItemRegistry().getItem("juice_melon");
            else if (stack.getType() == Material.SWEET_BERRIES)
                result = plugin.getItemRegistry().getItem("juice_sweet_berry");
            else if (stack.getType() == Material.GLOW_BERRIES)
                result = plugin.getItemRegistry().getItem("juice_glow_berry");

            if (result != null) {
                // Check for Glass Bottle requirement?
                // Simplification for now: Just convert.

                // Verify space in output FIRST logic? or Drop?
                // Logic says: if output block has inventory, add. Else drop.

                // Remove input safely
                int newAmount = stack.getAmount() - 1;
                if (newAmount > 0) {
                    stack.setAmount(newAmount);
                    inputInv.setItem(i, stack); // Explicitly set it
                } else {
                    inputInv.setItem(i, null);
                }

                Block outputBlock = loc.clone().add(0, -1, 0).getBlock();
                if (outputBlock.getState() instanceof org.bukkit.inventory.InventoryHolder) {
                    ((org.bukkit.inventory.InventoryHolder) outputBlock.getState()).getInventory().addItem(result);
                } else {
                    loc.getWorld().dropItemNaturally(loc.clone().add(0.5, -0.5, 0.5), result);
                }
                return; // One per tick
            }
        }
    }

    private void processAdvancedFurnace(Location loc, MachineData data) {
        Block block = loc.getBlock();
        if (block.getType() != Material.BLAST_FURNACE) {
            unregisterMachine(loc);
            return;
        }

        if (!(block.getState() instanceof org.bukkit.block.Furnace)) {
            return;
        }

        org.bukkit.block.Furnace furnace = (org.bukkit.block.Furnace) block.getState();

        if (furnace.getInventory().getSmelting() == null
                || furnace.getInventory().getSmelting().getType() == Material.AIR) {
            furnace.setCookTime((short) 0);
            furnace.update();
            return;
        }

        double threshold = thresholdsTicks.getOrDefault(MachineType.ADVANCED_FURNACE, 10.0);
        if (data.progress < threshold)
            return;
        data.progress -= threshold;

        if (furnace.getBurnTime() > 0) {
            int current = furnace.getCookTime();
            int total = furnace.getCookTimeTotal();
            if (total <= 0)
                total = 100; // Default for blast furnace

            // Advance by a lot (10x speed effectively)
            int newTime = current + 100;
            if (newTime >= total) {
                newTime = total - 1;
            }

            furnace.setCookTime((short) newTime);
            furnace.update();
        }
    }

    private void processSieve(Location loc, MachineData data) {
        Block input = loc.clone().add(0, 1, 0).getBlock();

        if (canSieve(input.getType())) {
            double threshold = thresholdsTicks.getOrDefault(MachineType.SIEVE, 100.0);
            if (data.progress >= threshold) {
                List<ItemStack> drops = recipeService.getSieveDrops(input.getType(), "mesh_string");

                if (!drops.isEmpty()) {
                    TierConfig tc = getTierConfig(data.tier);
                    for (ItemStack drop : drops) {
                        ItemStack finalDrop = drop.clone();
                        if (tc.efficiency > 1.0) {
                            if (new Random().nextDouble() < (tc.efficiency - 1.0)) {
                                finalDrop.setAmount(finalDrop.getAmount() * 2);
                            }
                        }
                        loc.getWorld().dropItemNaturally(loc, finalDrop);
                    }
                    input.setType(Material.AIR);
                }
                data.progress -= threshold;
            }
        } else {
            data.progress = 0;
        }
    }

    private void processBarrel(Location loc, MachineData data) {
        Block input = loc.clone().add(0, 1, 0).getBlock();

        if (canCompost(input.getType())) {
            double threshold = thresholdsTicks.getOrDefault(MachineType.BARREL, 100.0);
            if (data.progress >= threshold) {
                ItemStack result = recipeService.getBarrelResult(input.getType());

                if (result != null) {
                    TierConfig tc = getTierConfig(data.tier);
                    ItemStack finalResult = result.clone();
                    // Efficiency bonus
                    if (tc.efficiency > 1.0) {
                        if (new Random().nextDouble() < (tc.efficiency - 1.0)) {
                            finalResult.setAmount(finalResult.getAmount() * 2);
                        }
                    }
                    loc.getWorld().dropItemNaturally(loc, finalResult);
                    input.setType(Material.AIR);
                }
                data.progress -= threshold;
            }
        } else {
            data.progress = 0;
        }
    }

    private void processCrucible(Location loc, MachineData data) {
        Block input = loc.clone().add(0, 1, 0).getBlock();

        if (canMelt(input.getType())) {
            double threshold = thresholdsTicks.getOrDefault(MachineType.CRUCIBLE, 100.0);
            if (data.progress >= threshold) {
                Material result = recipeService.getCrucibleResult(input.getType());

                if (result != null) {
                    Block output = loc.clone().add(0, -1, 0).getBlock();
                    if (output.getType() == Material.AIR) {
                        output.setType(result);
                    }
                    input.setType(Material.AIR);
                }
                data.progress -= threshold;
            }
        } else {
            data.progress = 0;
        }
    }

    private void processCobbleGen(Location loc, MachineData data) {
        Block block = loc.getBlock();
        if (block.getType() != Material.DISPENSER)
            return;

        double threshold = thresholdsTicks.getOrDefault(MachineType.COBBLE_GEN, 200.0);

        // Apply Island Generator Multiplier
        Optional<com.wiredid.skytree.model.Island> optIsland = plugin.getIslandService().getIslandAtLocation(loc);
        if (optIsland.isPresent()) {
            threshold /= optIsland.get().getGeneratorMultiplier();
        }

        if (data.progress >= threshold) {
            data.progress -= threshold;
            // Store internally in the machine data buffer
            data.addDescItem(new ItemStack(Material.COBBLESTONE));
        }
    }

    private void processCompressor(Location loc, MachineData data) {
        double threshold = thresholdsTicks.getOrDefault(MachineType.COMPRESSOR, 200.0);
        if (data.progress < threshold)
            return;
        data.progress -= threshold;

        // Compressor structure: Piston (top, registered) on Dispenser (bottom)
        // Input items are in the DISPENSER below the piston

        Block inputBlock = loc.clone().add(0, -1, 0).getBlock();
        if (!(inputBlock.getState() instanceof org.bukkit.inventory.InventoryHolder)) {
            return;
        }

        org.bukkit.inventory.Inventory inv = ((org.bukkit.inventory.InventoryHolder) inputBlock.getState())
                .getInventory();

        // Scan for compressible items (stacks >= 9)
        // Simplification: Check first slot or iterate?
        // Iterate all slots.

        Map<String, ItemStack> recipes = recipeService.getCompressionRecipes();
        // Since we don't have easy lookup from item -> recipe key, we might iterate
        // recipes or items.
        // Better: iterate Inventory, check if item is in valid inputs.
        // But getCompressionRecipes returns Map<StringKey, Output>.
        // We need Map<InputKey, Output>.

        // We can't easily know input from just output map.
        // We need internal knowledge of recipes. Simple hardcoded check for now based
        // on known keys.
        // "pebble_stone_9" -> 9 pebble_stone -> 1 COBBLESTONE

        // Let's iterate slots
        for (ItemStack stack : inv.getContents()) {
            if (stack == null || stack.getAmount() < 9)
                continue;

            // Generate key for this item?
            // "pebble_stone"
            // "piece_iron"
            // "cobblestone" -> "compressed_cobblestone" (not implemented yet maybe)

            String itemId = plugin.getItemRegistry().getItemId(stack);
            if (itemId == null) {
                itemId = stack.getType().getKey().toString();
                if (itemId.startsWith("minecraft:")) {
                    itemId = itemId.substring(10);
                }
            }

            String recipeKey = itemId + "_9";
            if (recipes.containsKey(recipeKey)) {
                // Compress!
                ItemStack result = recipes.get(recipeKey).clone();

                // Consume 9
                stack.setAmount(stack.getAmount() - 9);

                // Apply worth lore before dropping so pickup stacking works
                com.wiredid.skytree.api.WorthService worthSvc = plugin.getWorthService();
                if (worthSvc != null) {
                    worthSvc.updateItemLore(result);
                }
                // Drop result naturally at machine location (output goes to environment)
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), result);

                // One operation per tick cycle
                return;
            }
        }
    }

    private boolean canSieve(Material material) {
        return recipeService.canSieve(material);
    }

    private boolean canCompost(Material material) {
        return recipeService.canCompost(material);
    }

    private boolean canMelt(Material material) {
        return recipeService.canMelt(material);
    }

    private void processAutoCrafter(Location loc, MachineData data) {
        // Auto-crafter logic: Check chest above for inputs
        Block inputBlock = loc.clone().add(0, 1, 0).getBlock();
        if (!(inputBlock.getState() instanceof org.bukkit.inventory.InventoryHolder))
            return;

        org.bukkit.inventory.Inventory inputInv = ((org.bukkit.inventory.InventoryHolder) inputBlock.getState())
                .getInventory();

        // Get recipes
        Map<String, RecipeService.AutoCrafterRecipe> recipes = recipeService.getAutoCrafterRecipes();

        // If meta has a recipe ID, only try that one. Else, try all?
        // Trying all is expensive. Let's assume we use meta to select.
        if (data.meta == null || data.meta.isEmpty())
            return;

        RecipeService.AutoCrafterRecipe recipe = recipes.get(data.meta);
        if (recipe == null)
            return;

        double threshold = recipe.getProcessTime();
        if (data.progress < threshold)
            return;

        // Check if inputInv has all ingredients
        boolean hasIngredients = true;
        for (ItemStack input : recipe.getInputs()) {
            if (!inputInv.containsAtLeast(input, input.getAmount())) {
                hasIngredients = false;
                break;
            }
        }

        if (!hasIngredients) {
            data.progress = 0; // Reset progress if inputs are missing?
            // Or keep it? Usually machines keep progress but wait.
            return;
        }

        // Check output space (below)
        Block outputBlock = loc.clone().add(0, -1, 0).getBlock();
        org.bukkit.inventory.Inventory outputInv = null;
        if (outputBlock.getState() instanceof org.bukkit.inventory.InventoryHolder) {
            outputInv = ((org.bukkit.inventory.InventoryHolder) outputBlock.getState()).getInventory();
        }

        // Process!
        data.progress -= threshold;

        // Consume ingredients
        for (ItemStack input : recipe.getInputs()) {
            removeIngredients(inputInv, input);
        }

        // Award result
        ItemStack result = recipe.getOutput().clone();

        // Efficiency bonus: Tier 2 and Tier 3 might give extra output
        TierConfig tc = getTierConfig(data.tier);
        if (tc.efficiency > 1.0) {
            // Simple logic: chance for double output based on efficiency?
            // e.g. 1.2 efficiency = 20% chance for double.
            if (new Random().nextDouble() < (tc.efficiency - 1.0)) {
                result.setAmount(result.getAmount() * 2);
            }
        }

        if (outputInv != null) {
            HashMap<Integer, ItemStack> leftover = outputInv.addItem(result);
            for (ItemStack left : leftover.values()) {
                loc.getWorld().dropItemNaturally(loc, left);
            }
        } else {
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), result);
        }
    }

    private void removeIngredients(org.bukkit.inventory.Inventory inv, ItemStack target) {
        int toRemove = target.getAmount();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR)
                continue;

            if (stack.isSimilar(target)) {
                if (stack.getAmount() > toRemove) {
                    stack.setAmount(stack.getAmount() - toRemove);
                    return;
                } else {
                    toRemove -= stack.getAmount();
                    inv.setItem(i, null);
                    if (toRemove <= 0)
                        return;
                }
            }
        }
    }

    public void saveMachines() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "machines.json");
        try (java.io.Writer writer = new java.io.FileWriter(file)) {
            MachineStorage storage = new MachineStorage();
            for (Map.Entry<Location, MachineData> entry : activeMachines.entrySet()) {
                storage.machines.add(new MachineEntry(entry.getKey(), entry.getValue()));
            }
            new com.google.gson.Gson().toJson(storage, writer);
            plugin.getLogger().info("Saved " + activeMachines.size() + " active machines.");
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Failed to save machines.json: " + e.getMessage());
        }
    }

    private void loadMachines() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "machines.json");
        if (!file.exists())
            return;

        try (java.io.Reader reader = new java.io.FileReader(file)) {
            MachineStorage storage = new com.google.gson.Gson().fromJson(reader, MachineStorage.class);

            if (storage != null && storage.machines != null) {
                for (MachineEntry entry : storage.machines) {
                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(entry.world);
                    if (world != null) {
                        Location loc = new Location(world, entry.x, entry.y, entry.z);
                        activeMachines.put(loc, entry.data);
                        // Ensure loaded machines have a sensible last-update timestamp
                        if (entry.data.lastUpdateMs <= 0) {
                            entry.data.lastUpdateMs = System.currentTimeMillis();
                        }
                    }
                }
                plugin.getLogger().info("Loaded " + activeMachines.size() + " machines.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load machines.json: " + e.getMessage());
        }
    }

    // Changing storage to List for simpler serialization
    // Actually, simple way: load/save using custom format or wrapper.
    // Let's replace save/load with a robust simple approach.

    // We will define a Wrapper class for JSON.
    private static class MachineStorage {
        List<MachineEntry> machines = new ArrayList<>();
    }

    // Duplicate method removed

    private static class MachineEntry {
        String world;
        int x, y, z;
        MachineData data;

        MachineEntry(Location loc, MachineData data) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.data = data;
        }
    }

    // MachineData moved to end of file for visibility
    /*
     * private static class MachineData { ... }
     */

    /**
     * Process Cobble Minion - Auto-mines 5x5 platform below
     * Mines 1 block every 5 seconds (100 ticks)
     */
    private void processCobbleMinion(Location loc, MachineData data) {
        // Find platform blocks (5x5, 1 block below minion)
        Location platformStart = loc.clone().add(-2, -1, -2);
        List<Block> mineableBlocks = new ArrayList<>();

        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                Block b = platformStart.clone().add(x, 0, z).getBlock();
                if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                    mineableBlocks.add(b);
                }
            }
        }

        if (mineableBlocks.isEmpty())
            return;

        // Mine 1 block every configured seconds (default 100 ticks / 5s)
        double threshold = thresholdsTicks.getOrDefault(MachineType.COBBLE_MINION, 100.0);

        // Apply Island Generator Multiplier
        Optional<com.wiredid.skytree.model.Island> optIsland = plugin.getIslandService().getIslandAtLocation(loc);
        if (optIsland.isPresent()) {
            threshold /= optIsland.get().getGeneratorMultiplier();
        }

        if (data.progress < threshold)
            return;
        data.progress -= threshold;
        // Pick random block to mine
        Block toMine = mineableBlocks.get(new Random().nextInt(mineableBlocks.size()));
        Material originalType = toMine.getType();

        // Get drops and add to INTERNAL STORAGE (Persistent)
        // Fix: "Storage bug where take all causes items to disappear" -> We must store
        // them in data.inventory
        for (ItemStack drop : toMine.getDrops()) {
            data.addDescItem(drop);
        }

        // Remove block
        toMine.setType(Material.AIR);

        // Schedule regeneration in 30 seconds (600 ticks)
        final Location blockLoc = toMine.getLocation();
        final Material regenMat = originalType;

        // Fix: "Prevent ghost blocks" -> Capture machine location and verify existence
        final Location machineLoc = loc.clone();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Verify Minion still exists
            if (!isMachineType(machineLoc, MachineType.COBBLE_MINION)) {
                return; // Do not regen if minion is gone
            }

            if (blockLoc.getBlock().getType() == Material.AIR) {
                blockLoc.getBlock().setType(regenMat);
            }
        }, 600L);
    }

    // Helper to access inventory from GUI
    public List<ItemStack> getMachineInventory(Location loc) {
        MachineData data = activeMachines.get(loc);
        if (data != null) {
            return data.deserializeInventory();
        }
        return new ArrayList<>();
    }

    public void collectMachineInventory(Location loc, org.bukkit.entity.Player player) {
        MachineData data = activeMachines.get(loc);
        if (data != null) {
            List<ItemStack> items = data.deserializeInventory();
            com.wiredid.skytree.api.WorthService worthService = plugin.getWorthService();
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR)
                    continue;

                if (worthService != null) {
                    worthService.updateItemLore(item);
                }

                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack l : leftover.values()) {
                    if (l != null && l.getAmount() > 0) {
                        player.getWorld().dropItemNaturally(player.getLocation(), l);
                    }
                }
            }
            data.inventory.clear(); // Clear internal ONLY after successful (or dropped) collection
            data.serializeInventory(new ArrayList<>());
            player.sendMessage("§aMinion items collected!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        }
    }

    public java.util.Set<Location> getActiveMachinesLocations() {
        return activeMachines.keySet();
    }

    public MachineData getMachineData(Location loc) {
        return activeMachines.get(loc);
    }

    public void upgradeMachine(Location loc) {
        MachineData data = activeMachines.get(loc);
        if (data != null && data.tier < 3) {
            data.tier++;
        }
    }

    public void updateMachineMeta(Location loc, String meta) {
        MachineData data = activeMachines.get(loc);
        if (data != null) {
            data.setMeta(meta);
        }
    }

    public static class MachineData {
        private final MachineType type;
        private int tier = 1;
        private double progress = 0.0;
        private long lastUpdateMs = System.currentTimeMillis();
        private String meta;
        private List<Map<String, Object>> inventory = new ArrayList<>();

        public MachineData(MachineType type) {
            this.type = type;
        }

        public MachineType getType() {
            return type;
        }

        public int getTier() {
            return tier;
        }

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public void setTier(int tier) {
            this.tier = tier;
        }

        synchronized void addDescItem(ItemStack item) {
            List<ItemStack> current = deserializeInventory();

            // Try to merge with existing stacks
            for (ItemStack existing : current) {
                if (existing.isSimilar(item)) {
                    int space = existing.getMaxStackSize() - existing.getAmount();
                    if (space > 0) {
                        int toAdd = Math.min(space, item.getAmount());
                        existing.setAmount(existing.getAmount() + toAdd);
                        item.setAmount(item.getAmount() - toAdd);

                        if (item.getAmount() <= 0) {
                            break;
                        }
                    }
                }
            }

            // If leftovers, adding new stack logic
            if (item.getAmount() > 0) {
                while (item.getAmount() > 0) {
                    int max = Math.min(item.getMaxStackSize(), 99);
                    int toTake = Math.min(item.getAmount(), max);

                    ItemStack newStack = item.clone();
                    newStack.setAmount(toTake);
                    current.add(newStack);

                    item.setAmount(item.getAmount() - toTake);
                }
            }

            serializeInventory(current);
        }

        synchronized List<ItemStack> deserializeInventory() {
            List<ItemStack> list = new ArrayList<>();
            if (inventory == null) {
                inventory = new ArrayList<>();
                return list;
            }
            for (Map<String, Object> map : inventory) {
                try {
                    list.add(ItemStack.deserialize(map));
                } catch (Exception e) {
                }
            }
            return list;
        }

        synchronized void serializeInventory(List<ItemStack> items) {
            inventory.clear();
            for (ItemStack item : items) {
                if (item != null)
                    inventory.add(item.serialize());
            }
        }
    }
}
