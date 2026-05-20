package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.RecipeService;
import com.wiredid.skytree.api.WorthService;
import com.wiredid.skytree.economy.JobService;
import com.wiredid.skytree.machine.MachineProcessor;
import com.wiredid.skytree.api.MultiblockService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.block.Chest;
import com.wiredid.skytree.SkytreePlugin;
import com.wiredid.skytree.util.ComponentUtil;
import com.wiredid.skytree.machine.MachineProcessor.MachineType;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.block.data.Levelled;

public class MechanicsListener implements Listener {

    private final SkytreePlugin plugin;
    private final ItemRegistry itemRegistry;
    private final RecipeService recipeService;
    private final MultiblockService multiblockService;
    private final MachineProcessor machineProcessor;
    private final WorthService worthService;
    private final JobService jobService;
    private final com.wiredid.skytree.impl.ResourceDropManager dropManager;
    private final java.util.Map<String, org.bukkit.inventory.ItemStack> dustSmeltingMap;
    private final java.util.Map<String, org.bukkit.inventory.ItemStack> foodSmeltingMap;
    private final java.util.Random random = new java.util.Random();
    private final java.util.Set<Material> treeLeavesMaterials = new java.util.HashSet<>();
    private boolean treeLeavesInitialized = false;

    // Hammer block conversion map
    private static final java.util.Map<Material, Material> HAMMER_RECIPES = new java.util.LinkedHashMap<>();
    static {
        HAMMER_RECIPES.put(Material.COBBLESTONE, Material.GRAVEL);
        HAMMER_RECIPES.put(Material.STONE, Material.COBBLESTONE);
        HAMMER_RECIPES.put(Material.GRAVEL, Material.SAND);
        HAMMER_RECIPES.put(Material.SAND, Material.GUNPOWDER); // Dust (generic)
        HAMMER_RECIPES.put(Material.NETHERRACK, Material.GRAVEL);
        HAMMER_RECIPES.put(Material.ANDESITE, Material.COBBLESTONE);
        HAMMER_RECIPES.put(Material.DIORITE, Material.COBBLESTONE);
        HAMMER_RECIPES.put(Material.GRANITE, Material.COBBLESTONE);
    }

    public MechanicsListener(SkytreePlugin plugin, ItemRegistry itemRegistry, RecipeService recipeService,
            MultiblockService multiblockService, MachineProcessor machineProcessor, WorthService worthService, JobService jobService) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.recipeService = recipeService;
        this.multiblockService = multiblockService;
        this.machineProcessor = machineProcessor;
        this.worthService = worthService;
        this.jobService = jobService;
        this.dropManager = new com.wiredid.skytree.impl.ResourceDropManager(plugin);
        this.dustSmeltingMap = new java.util.HashMap<>();
        this.foodSmeltingMap = new java.util.HashMap<>();
        initDustSmeltingMap();
        initFoodSmeltingMap();
        initTreeLeavesMaterials();
    }

    private void initTreeLeavesMaterials() {
        treeLeavesMaterials.add(Material.AZALEA_LEAVES);
        treeLeavesMaterials.add(Material.FLOWERING_AZALEA_LEAVES);
        for (com.wiredid.skytree.impl.ResourceDropManager.TreeDropTable table : dropManager.getAllTreeDrops().values()) {
            if (table.leaves != null) {
                treeLeavesMaterials.add(table.leaves);
            }
        }
        treeLeavesInitialized = true;
    }

    private void initFoodSmeltingMap() {
        foodSmeltingMap.put("silkworm", itemRegistry.getItem("silkworm_cooked"));
    }

    private void initDustSmeltingMap() {
        dustSmeltingMap.put("dust_iron", new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT));
        dustSmeltingMap.put("dust_copper", new org.bukkit.inventory.ItemStack(org.bukkit.Material.COPPER_INGOT));
        dustSmeltingMap.put("dust_lead", itemRegistry.getItem("ingot_lead"));
        dustSmeltingMap.put("dust_tin", itemRegistry.getItem("ingot_tin"));
        dustSmeltingMap.put("dust_aluminum", itemRegistry.getItem("ingot_aluminum"));
        dustSmeltingMap.put("dust_silver", itemRegistry.getItem("ingot_silver"));
        dustSmeltingMap.put("dust_nickel", itemRegistry.getItem("ingot_nickel"));
        dustSmeltingMap.put("dust_zinc", itemRegistry.getItem("ingot_zinc"));
        dustSmeltingMap.put("dust_brass", itemRegistry.getItem("ingot_brass"));
        dustSmeltingMap.put("dust_bronze", itemRegistry.getItem("ingot_bronze"));
        dustSmeltingMap.put("dust_steel", itemRegistry.getItem("ingot_steel"));
        dustSmeltingMap.put("dust_uranium", itemRegistry.getItem("ingot_uranium"));
        dustSmeltingMap.put("dust_obsidian", new org.bukkit.inventory.ItemStack(org.bukkit.Material.OBSIDIAN));
        dustSmeltingMap.put("dust_ender", new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL));
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Material type = event.getBlockPlaced().getType();

        // Illegal items check
        if (type == Material.BARRIER || type == Material.BEDROCK || type == Material.COMMAND_BLOCK
                || type == Material.CHAIN_COMMAND_BLOCK || type == Material.REPEATING_COMMAND_BLOCK
                || type == Material.STRUCTURE_BLOCK || type == Material.JIGSAW || type == Material.DRAGON_EGG
                || type.name().contains("SPAWN_EGG")) {
            if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c§l[Skytree] §cYou cannot place this illegal item!");
                return;
            }
        }

        if (itemRegistry.isCustomItem(item)) {
            // Handle custom block placement data if needed
        }
    }

    // Sneak + right-click on dirt/grass to find pebbles
    @EventHandler
    public void onDirtScavenge(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (type != Material.DIRT && type != Material.GRASS_BLOCK && type != Material.COARSE_DIRT
                && type != Material.ROOTED_DIRT && type != Material.PODZOL && type != Material.MYCELIUM
                && type != Material.GRASS_PATH && type != Material.FARMLAND) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!hand.getType().isAir()) return;

        if (random.nextDouble() < 0.4) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                    itemRegistry.getItem("pebble_stone"));
            player.sendMessage("§7You found a pebble!");
        } else {
            player.sendMessage("§7You scavenge but find nothing.");
        }

        event.setCancelled(true);
    }

    // Silkworm: right-click leaves with empty hand
    @EventHandler
    public void onLeafInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        boolean isLeaf = type.name().endsWith("_LEAVES") || treeLeavesMaterials.contains(type);
        if (!isLeaf) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Crook: right-click leaves with crook
        String handId = itemRegistry.getItemId(hand);
        if (handId != null && dropManager.isCrook(handId)) {
            handleCrookInteract(player, block, hand, handId);
            event.setCancelled(true);
            return;
        }

        // Empty hand silkworm
        if (hand.getType() != Material.AIR) return;

        if (Math.random() < 0.6) {
            player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                    itemRegistry.getItem("silkworm"));
            player.sendMessage("§aYou found a silkworm!");
        }

        // 20% chance to break the leaf
        if (Math.random() < 0.2) {
            block.breakNaturally();
        }

        event.setCancelled(true);
    }

    private void handleCrookInteract(Player player, Block block, ItemStack crook, String crookId) {
        Material leafType = block.getType();
        // Determine trunk type by looking 1-2 blocks below
        Material trunkType = Material.AIR;
        for (int y = 1; y <= 3; y++) {
            Block below = block.getRelative(0, -y, 0);
            if (below.getType().name().endsWith("_LOG") || below.getType().name().endsWith("_WOOD")
                    || below.getType() == Material.STONE) {
                trunkType = below.getType();
                break;
            }
        }
        if (trunkType == Material.AIR) {
            trunkType = Material.OAK_LOG; // Default fallback
        }

        // Identify tree type
        String treeKey = dropManager.identifyTreeType(leafType, trunkType);
        if (treeKey == null) {
            // Try to match by material only for vanilla trees
            if (leafType.name().endsWith("_LEAVES")) {
                // Use generic oak drops for unrecognized trees
                treeKey = "dirt_tree";
            } else {
                player.sendMessage("§cNo resources found on these leaves.");
                return;
            }
        }

        com.wiredid.skytree.impl.ResourceDropManager.TreeDropTable table = dropManager.getTreeDrop(treeKey);
        if (table == null || table.drops.isEmpty()) {
            player.sendMessage("§cThis tree has no harvestable resources.");
            return;
        }

        double bonus = dropManager.getCrookBonus(crookId);
        boolean droppedAny = false;

        for (com.wiredid.skytree.impl.ResourceDropManager.DropEntry entry : table.drops) {
            double effectiveChance = entry.chance * (1.0 + bonus);
            if (random.nextDouble() * 100 < effectiveChance) {
                int amount = entry.min + random.nextInt(entry.max - entry.min + 1);
                ItemStack drop = resolveDropItem(entry.itemId, amount);
                if (drop != null) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.7, 0.5), drop);
                    droppedAny = true;
                }
            }
        }

        if (droppedAny) {
            player.sendMessage("§a§l[Crook] §7Harvested " + treeKey.replace("_", " ") + "!");
        } else {
            player.sendMessage("§7No drops this time.");
        }

        // Damage crook
        if (crook.getType() != Material.AIR) {
            crook.damage(1, player);
            if (crook.getType() == Material.AIR) {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    private ItemStack resolveDropItem(String itemId, int amount) {
        if (itemId == null) return null;

        // Check custom items first
        ItemStack custom = itemRegistry.getItem(itemId);
        if (custom != null) {
            custom.setAmount(amount);
            return custom;
        }

        // Check vanilla materials
        Material mat = Material.matchMaterial(itemId.toUpperCase());
        if (mat != null && mat != Material.AIR) {
            return new ItemStack(mat, amount);
        }

        return null;
    }

    // Composter override: harvest gives dirt instead of bone meal
    @EventHandler
    public void onComposterInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COMPOSTER)
            return;

        // Check if composter is full (level 7 = ready)
        if (block.getBlockData() instanceof Levelled) {
            Levelled levelled = (Levelled) block.getBlockData();
            if (levelled.getLevel() >= levelled.getMaximumLevel()) {
                event.setCancelled(true);
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5),
                        new ItemStack(Material.DIRT));
                levelled.setLevel(0);
                block.setBlockData(levelled);
                event.getPlayer().sendMessage("§6Composter produced Dirt!");
            }
        }
    }

    // Glass water bucket: right-click to place water
    @EventHandler
    public void onGlassBucketUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        String id = itemRegistry.getItemId(hand);
        if (id == null || !id.equals("glass_water_bucket"))
            return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Block target = clicked.getRelative(event.getBlockFace());
        if (target.getType() != Material.AIR)
            return;

        // Place water
        target.setType(Material.WATER);

        // Consume item (breaks)
        hand.setAmount(hand.getAmount() - 1);
        player.sendMessage("§bThe glass bucket shatters as it places the water!");
        event.setCancelled(true);
    }

    @EventHandler
    public void onSieveInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.OAK_FENCE)
            return;

        // Check if it's a sieve multiblock
        if (!multiblockService.isMultiblock(block.getLocation(), "sieve"))
            return;

        Player player = event.getPlayer();
        ItemStack mesh = player.getInventory().getItemInMainHand();

        if (!itemRegistry.isCustomItem(mesh) || !itemRegistry.getItemId(mesh).contains("_mesh")) {
            player.sendMessage("§cYou must hold a Mesh to use the Sieve!");
            return;
        }

        String meshType = itemRegistry.getItemId(mesh).replace("_mesh", "");
        String pureMeshType = meshType.replace("compressed_", "");

        Block chestBlock = block.getRelative(0, -1, 0);
        if (chestBlock.getType() != Material.CHEST && chestBlock.getType() != Material.TRAPPED_CHEST) {
            player.sendMessage("§cPlace a Chest under the Sieve to hold materials!");
            return;
        }

        BlockState state = chestBlock.getState();
        Inventory chestInv = ((Chest) state).getInventory();

        boolean shiftClick = player.isSneaking();
        int totalProcessed = 0;

        // Find first sievable item in chest
        for (int i = 0; i < chestInv.getSize(); i++) {
            ItemStack item = chestInv.getItem(i);
            if (item == null || item.getType() == Material.AIR)
                continue;

            if (recipeService.canSieve(item.getType())) {
                String itemId = itemRegistry.getItemId(item);
                boolean isCompressed = itemId != null && itemId.startsWith("compressed_");
                int multiplier = isCompressed ? 9 : 1;

                int amountToProcess = shiftClick ? item.getAmount() : 1;

                for (int count = 0; count < amountToProcess; count++) {
                    java.util.List<ItemStack> drops = recipeService.getSieveDrops(item.getType(), pureMeshType);
                    for (ItemStack drop : drops) {
                        ItemStack finalDrop = drop.clone();
                        finalDrop.setAmount(finalDrop.getAmount() * multiplier);
                        giveOrDropItem(player, finalDrop);
                    }
                }

                totalProcessed += amountToProcess;
                if (shiftClick) {
                    chestInv.setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                break;
            }
        }

        if (totalProcessed > 0) {
            if (jobService != null) {
                jobService.handleJobAction(player, "farmer", 2.0 + totalProcessed);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_SAND_BREAK, 1.0f, 1.0f);
            if (!shiftClick) {
                player.sendMessage("§aProcessed 1x material.");
            } else {
                player.sendMessage("§aProcessed " + totalProcessed + "x materials.");
            }
        } else {
            player.sendMessage("§cNo sievable materials found in the chest below!");
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onMachineInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Block clicked = event.getClickedBlock();
        if (clicked == null)
            return;
        Player player = event.getPlayer();
        if (player.isSneaking())
            return;

        Material type = clicked.getType();

        // Barrel (Trapdoor on Cauldron)
        if (type.name().contains("TRAPDOOR") && multiblockService.isBarrel(clicked)) {
            event.setCancelled(true);
            machineProcessor.registerMachine(clicked.getRelative(BlockFace.DOWN).getLocation(), MachineType.BARREL);
            openBarrelGUI(player);
            return;
        }

        // Electric Furnace (Iron Trapdoor on Blast Furnace)
        if (type == Material.IRON_TRAPDOOR && multiblockService.isElectricFurnace(clicked.getRelative(BlockFace.DOWN))) {
            event.setCancelled(true);
            machineProcessor.registerMachine(clicked.getRelative(BlockFace.DOWN).getLocation(), MachineType.ADVANCED_FURNACE);
            openElectricFurnaceGUI(player);
            return;
        }

        // Crucible (Cauldron on Heat Source) - only if no trapdoor above
        if (type == Material.CAULDRON) {
            if (clicked.getRelative(BlockFace.UP).getType().name().contains("TRAPDOOR")) {
                event.setCancelled(true);
                openBarrelGUI(player);
                return;
            }
            if (multiblockService.isCrucible(clicked)) {
                event.setCancelled(true);
                machineProcessor.registerMachine(clicked.getLocation(), MachineType.CRUCIBLE);
                openCrucibleGUI(player);
                return;
            }
        }

        // Compressor (Piston on Dispenser)
        if (type == Material.PISTON && multiblockService.isCompressor(clicked)) {
            event.setCancelled(true);
            machineProcessor.registerMachine(clicked.getLocation(), MachineType.COMPRESSOR);
            openCompressorGUI(player);
            return;
        }

        // Pulverizer (Iron Block on Dispenser)
        if (type == Material.IRON_BLOCK && multiblockService.isPulverizer(clicked)) {
            event.setCancelled(true);
            openPulverizerGUI(player);
            return;
        }

        // Cobble Gen (Dispenser with pickaxe)
        if (type == Material.DISPENSER
                && player.getInventory().getItemInMainHand().getType().name().contains("PICKAXE")) {
            if (multiblockService.isCompressor(clicked.getRelative(BlockFace.UP)))
                return;
            if (multiblockService.isPulverizer(clicked.getRelative(BlockFace.UP)))
                return;
            event.setCancelled(true);
            machineProcessor.registerMachine(clicked.getLocation(), MachineType.COBBLE_GEN);
            openCobbleGenGUI(player);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        org.bukkit.inventory.ItemStack source = event.getSource();
        if (source == null || source.getType() == org.bukkit.Material.AIR) return;

        String itemId = itemRegistry.getItemId(source);
        if (itemId == null) return;

        org.bukkit.inventory.ItemStack result = dustSmeltingMap.get(itemId);
        if (result != null) {
            event.setResult(result.clone());
            if (jobService != null && event.getBlock() != null) {
                org.bukkit.block.Block block = event.getBlock();
                java.util.Collection<org.bukkit.entity.Player> players = block.getWorld().getNearbyPlayers(
                        block.getLocation().add(0.5, 0.5, 0.5), 5);
                for (org.bukkit.entity.Player p : players) {
                    if (!p.hasMetadata("NPC")) {
                        double smeltWorth = plugin.getWorthService().getItemSellPrice(result);
                        jobService.handleJobAction(p, "crafter", Math.max(smeltWorth, 5.0));
                        break;
                    }
                }
            }
            return;
        }

        result = foodSmeltingMap.get(itemId);
        if (result != null) {
            event.setResult(result.clone());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Crook instant break cobweb
        if (block.getType() == Material.COBWEB) {
            String itemId = plugin.getItemRegistry().getItemId(tool);
            if (itemId != null && itemId.startsWith("crook_")) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.COBWEB));
                if (tool.getType() != Material.AIR) {
                    tool.damage(1, player);
                    if (tool.getType() == Material.AIR) {
                        player.getInventory().setItemInMainHand(null);
                    }
                }
                player.giveExp(1);
                if (jobService != null && !player.hasMetadata("NPC")) {
                    jobService.handleJobAction(player, "lumberjack", 1.0);
                }
                return;
            }
        }

        // Hammer: break block override
        String handId = plugin.getItemRegistry().getItemId(tool);
        if (handId != null && handId.startsWith("hammer_")) {
            Material result = HAMMER_RECIPES.get(block.getType());
            if (result != null) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(result));
                if (tool.getType() != Material.AIR) {
                    tool.damage(1, player);
                    if (tool.getType() == Material.AIR) {
                        player.getInventory().setItemInMainHand(null);
                    }
                }
                player.sendMessage("§7§l[Hammer] §7Crushed " + formatBlockName(block.getType()) + "!");
                return;
            }
        }

        if (machineProcessor.getActiveMachinesLocations().contains(block.getLocation())) {
            machineProcessor.unregisterMachine(block.getLocation());
        }
    }

    private String formatBlockName(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }

    private void openBarrelGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lCrushing Tub"));
        setupMachineGUI(gui);
        gui.setItem(10, null);
        gui.setItem(14, createButton(Material.LIME_STAINED_GLASS_PANE, "§a§lProcess", "§7Crush organics into water"));
        player.openInventory(gui);
    }

    private void openCrucibleGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lCrucible"));
        setupMachineGUI(gui);
        gui.setItem(10, null);
        gui.setItem(14, createButton(Material.ORANGE_STAINED_GLASS_PANE, "§6§lMelt", "§7Click to Melt Stone -> Lava"));
        player.openInventory(gui);
    }

    private void openCompressorGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lCompressor"));
        setupMachineGUI(gui);
        gui.setItem(10, null);
        gui.setItem(14, createButton(Material.PISTON, "§6§lCompress", "§7Click to Compress 9x items"));
        player.openInventory(gui);
    }

    private void openPulverizerGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lPulverizer"));
        setupMachineGUI(gui);
        gui.setItem(10, null);
        gui.setItem(14, createButton(Material.IRON_BLOCK, "§7§lPulverize", "§7Click to Crush ores"));
        player.openInventory(gui);
    }

    private void openElectricFurnaceGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lElectric Furnace"));
        setupMachineGUI(gui);
        gui.setItem(10, null);
        gui.setItem(14, createButton(Material.BLAST_FURNACE, "§c§lSmelt", "§7Click to Smelt Dusts"));
        player.openInventory(gui);
    }

    private void openCobbleGenGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ComponentUtil.parse("§6§lCobblestone Generator"));
        setupMachineGUI(gui);
        gui.setItem(14, createButton(Material.COBBLESTONE, "§7§lGenerate", "§7Click to Collect Cobble"));
        player.openInventory(gui);
    }

    private void setupMachineGUI(Inventory gui) {
        ItemStack bg = createButton(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (i != 10 && i != 12) {
                gui.setItem(i, bg);
            }
        }
    }

    private ItemStack createButton(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(ComponentUtil.parse(name));
        meta.lore(ComponentUtil.parseList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private void giveOrDropItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return;

        // CRITICAL: Update worth lore before adding to inventory to fix stacking
        if (worthService != null) {
            worthService.updateItemLore(item);
        }

        java.util.HashMap<Integer, ItemStack> left = player.getInventory().addItem(item);
        if (!left.isEmpty()) {
            for (ItemStack drop : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage("§eInventory full! Item dropped at your feet.");
        }
    }
}
