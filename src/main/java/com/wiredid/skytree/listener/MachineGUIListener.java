package com.wiredid.skytree.listener;

import com.wiredid.skytree.api.ItemRegistry;
import com.wiredid.skytree.api.RecipeService;
import com.wiredid.skytree.util.ComponentUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Listener for machine GUI interactions
 * Handles processing logic for all machines
 */
public class MachineGUIListener implements Listener {

    private final RecipeService recipeService;
    private final ItemRegistry itemRegistry;
    private final com.wiredid.skytree.machine.MachineProcessor machineProcessor;

    public MachineGUIListener(com.wiredid.skytree.api.MachineService machineService, RecipeService recipeService,
            ItemRegistry itemRegistry, com.wiredid.skytree.machine.MachineProcessor machineProcessor) {

        this.recipeService = recipeService;
        this.itemRegistry = itemRegistry;
        this.machineProcessor = machineProcessor;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        String title = ComponentUtil.toLegacy(event.getView().title());

        // Only handle actual machine GUIs, not every "§6§l" GUI
        boolean isMachine = title.equals("§6§lSieve")
                || title.contains("Crushing Tub") || title.contains("Barrel") || title.contains("Crucible")
                || title.contains("Compressor") || title.contains("Pulverizer")
                || title.contains("Electric Furnace") || title.contains("Generator")
                || title.contains("Cobblestone Generator");
        if (!isMachine)
            return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName())
            return;

        // Allow clicking in player inventory
        if (event.getClickedInventory() == player.getInventory())
            return;

        event.setCancelled(true); // Prevent taking GUI items by default

        // Handle Process Buttons
        String displayName = ComponentUtil.toLegacy(clicked.getItemMeta().displayName());

        if (title.equals("§6§lSieve")) {
            handleSieve(player, event.getInventory(), displayName);
        } else if (title.contains("Crushing Tub") || title.contains("Barrel")) {
            handleBarrel(player, event.getInventory(), displayName);
        } else if (title.contains("Crucible")) {
            handleCrucible(player, event.getInventory(), displayName);
        } else if (title.contains("Compressor")) {
            handleCompressor(player, event.getInventory(), displayName);
        } else if (title.contains("Pulverizer")) {
            handlePulverizer(player, event.getInventory(), displayName);
        } else if (title.contains("Electric Furnace")) {
            handleFurnace(player, event.getInventory(), displayName);
        } else if (title.contains("Generator")) {
            handleGenerator(player, event.getInventory(), displayName);
        } else if (title.contains("Cobblestone Generator")) {
            handleCobbleGen(player, event.getInventory(), displayName);
        }
    }

    private void handleSieve(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Process")) {
            ItemStack input = inv.getItem(10); // Input Slot
            ItemStack mesh = inv.getItem(12); // Mesh Slot

            if (input == null || mesh == null) {
                player.sendMessage("§c§l[Sieve] §cMissing input or mesh!");
                return;
            }

            String meshId = itemRegistry.getItemId(mesh);
            if (meshId == null || !meshId.startsWith("mesh_")) {
                player.sendMessage("§c§l[Sieve] §cInvalid mesh!");
                return;
            }

            if (!recipeService.canSieve(input.getType())) {
                player.sendMessage("§c§l[Sieve] §cThis item cannot be sieved!");
                return;
            }

            // Process
            List<ItemStack> drops = recipeService.getSieveDrops(input.getType(), meshId);
            if (drops.isEmpty()) {
                player.sendMessage("§c§l[Sieve] §cNo drops found for this combination.");
            } else {
                for (ItemStack drop : drops) {
                    player.getInventory().addItem(drop);
                }
                player.sendMessage("§a§l[Sieve] §aSieved " + input.getType().name() + "!");

                // Consume input
                input.setAmount(input.getAmount() - 1);
                inv.setItem(10, input.getAmount() > 0 ? input : null);
            }
        }
    }

    private void handleBarrel(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Process")) {
            ItemStack input = inv.getItem(10); // Input slot

            if (input == null || input.getType() == Material.AIR) {
                player.sendMessage("§c§l[Crushing Tub] §cNo input item!");
                return;
            }

            if (!recipeService.canCompost(input.getType())) {
                player.sendMessage("§c§l[Crushing Tub] §cThis item cannot be crushed!");
                return;
            }

            // Process
            ItemStack result = recipeService.getBarrelResult(input.getType());
            if (result == null) {
                player.sendMessage("§c§l[Crushing Tub] §cNo recipe found!");
                return;
            }

            player.getInventory().addItem(result);
            player.sendMessage("§b§l[Crushing Tub] §bExtracted water from " + input.getType().name() + "!");

            // Consume input
            input.setAmount(input.getAmount() - 1);
            inv.setItem(10, input.getAmount() > 0 ? input : null);
        }
    }

    private void handleCrucible(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Melt")) {
            ItemStack input = inv.getItem(10); // Input slot

            if (input == null || input.getType() == Material.AIR) {
                player.sendMessage("§c§l[Crucible] §cNo input item!");
                return;
            }

            if (!recipeService.canMelt(input.getType())) {
                player.sendMessage("§c§l[Crucible] §cThis item cannot be melted!");
                return;
            }

            // Process
            Material result = recipeService.getCrucibleResult(input.getType());
            if (result == null) {
                player.sendMessage("§c§l[Crucible] §cNo recipe found!");
                return;
            }

            // Give output based on result type
            ItemStack output = result == Material.LAVA ? new ItemStack(Material.LAVA_BUCKET)
                    : result == Material.WATER ? new ItemStack(Material.WATER_BUCKET)
                            : new ItemStack(result);

            player.getInventory().addItem(output);
            player.sendMessage("§c§l[Crucible] §eMelted " + input.getType().name() + "!");

            // Consume input
            input.setAmount(input.getAmount() - 1);
            inv.setItem(10, input.getAmount() > 0 ? input : null);
        }
    }

    private void handleCompressor(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Compress")) {
            ItemStack input = inv.getItem(10); // Input slot

            if (input == null || input.getType() == Material.AIR) {
                player.sendMessage("§c§l[Compressor] §cNo input item!");
                return;
            }

            // Check if we have at least 9 items
            if (input.getAmount() < 9) {
                player.sendMessage("§c§l[Compressor] §cNeed 9 items to compress! " +
                        "§7You have: " + input.getAmount());
                return;
            }

            // Check recipes based on item type or custom item ID
            String itemId = itemRegistry.getItemId(input);
            ItemStack result = null;

            // Check custom item recipes
            if (itemId != null) {
                java.util.Map<String, ItemStack> recipes = recipeService.getCompressionRecipes();
                String recipeKey = itemId + "_9";
                result = recipes.get(recipeKey);
            }

            // Check vanilla compression
            if (result == null) {
                result = switch (input.getType()) {
                    case COBBLESTONE -> itemRegistry.getItem("compressed_cobble_1x");
                    case DIRT -> itemRegistry.getItem("compressed_dirt_1x");
                    case GRAVEL -> itemRegistry.getItem("compressed_gravel_1x");
                    case SAND -> itemRegistry.getItem("compressed_sand_1x");
                    default -> null;
                };
            }

            if (result == null) {
                player.sendMessage("§c§l[Compressor] §cNo compression recipe for this item!");
                return;
            }

            // Success - give result and consume 9 items
            player.getInventory().addItem(result.clone());
            player.sendMessage("§6§l[Compressor] §eCompressed 9x " + input.getType().name() + "!");

            // Consume 9 items
            input.setAmount(input.getAmount() - 9);
            inv.setItem(10, input.getAmount() > 0 ? input : null);
        }
    }

    private void handlePulverizer(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Pulverize")) {
            ItemStack input = inv.getItem(10); // Input slot

            if (input == null || input.getType() == Material.AIR) {
                player.sendMessage("§c§l[Pulverizer] §cNo input item!");
                return;
            }

            // Check pulverizer recipes
            ItemStack result = null;
            int outputAmount = 1;

            switch (input.getType()) {
                case IRON_ORE, DEEPSLATE_IRON_ORE -> {
                    result = itemRegistry.getItem("dust_iron");
                    outputAmount = 2;
                }
                case GOLD_ORE, DEEPSLATE_GOLD_ORE -> {
                    result = itemRegistry.getItem("dust_gold");
                    outputAmount = 2;
                }
                case COPPER_ORE, DEEPSLATE_COPPER_ORE -> {
                    result = itemRegistry.getItem("dust_copper");
                    outputAmount = 2;
                }
                case COBBLESTONE -> result = new ItemStack(Material.GRAVEL);
                case GRAVEL -> result = new ItemStack(Material.SAND);
                case SAND -> result = itemRegistry.getItem("dust_iron");
                case NETHERRACK -> result = new ItemStack(Material.GRAVEL);
                default -> {
                    player.sendMessage("§c§l[Pulverizer] §cCannot pulverize this item!");
                    return;
                }
            }

            if (result != null) {
                result.setAmount(outputAmount);
                player.getInventory().addItem(result);
                player.sendMessage("§7§l[Pulverizer] §7Crushed " + input.getType().name() + "!");

                // Consume input
                input.setAmount(input.getAmount() - 1);
                inv.setItem(10, input.getAmount() > 0 ? input : null);
            }
        }
    }

    private void handleFurnace(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Smelt")) {
            ItemStack input = inv.getItem(10); // Input slot

            if (input == null || input.getType() == Material.AIR) {
                player.sendMessage("§c§l[Furnace] §cNo input item!");
                return;
            }

            // Check smelting recipes
            ItemStack result = null;
            String itemId = itemRegistry.getItemId(input);

            // Custom item smelting
            if (itemId != null) {
                result = switch (itemId) {
                    case "dust_iron" -> new ItemStack(Material.IRON_INGOT);
                    case "dust_gold" -> new ItemStack(Material.GOLD_INGOT);
                    case "dust_copper" -> new ItemStack(Material.COPPER_INGOT);
                    default -> null;
                };
            }

            // Vanilla smelting
            if (result == null) {
                result = switch (input.getType()) {
                    case IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT);
                    case GOLD_ORE, DEEPSLATE_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
                    case COPPER_ORE, DEEPSLATE_COPPER_ORE -> new ItemStack(Material.COPPER_INGOT);
                    case COBBLESTONE -> new ItemStack(Material.STONE);
                    case SAND -> new ItemStack(Material.GLASS);
                    case CLAY -> new ItemStack(Material.TERRACOTTA);
                    default -> null;
                };
            }

            if (result == null) {
                player.sendMessage("§c§l[Furnace] §cCannot smelt this item!");
                return;
            }

            player.getInventory().addItem(result);
            player.sendMessage("§6§l[Furnace] §eSmelted " + input.getType().name() + "!");

            // Consume input
            input.setAmount(input.getAmount() - 1);
            inv.setItem(10, input.getAmount() > 0 ? input : null);
        }
    }

    private void handleGenerator(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Status")) {
            player.sendMessage("§e§l[Generator] §eEnergy generation active!");
        }
    }

    private void handleCobbleGen(Player player, Inventory inv, String clickedName) {
        if (clickedName.contains("Generate")) {
            // Get machine location from nearest dispenser player is looking at
            org.bukkit.block.Block target = player.getTargetBlockExact(5);
            if (target != null && target.getType() == Material.DISPENSER) {
                machineProcessor.collectMachineInventory(target.getLocation(), player);
            } else {
                player.sendMessage("§c§l[Cobble Gen] §cYou must be looking at the generator!");
            }
        }
    }
}
