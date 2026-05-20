package com.wiredid.skytree.api;

import com.wiredid.skytree.SkytreePlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Cocoa;

import java.util.*;

public class CustomCropService implements Listener {

    private final ItemRegistry itemRegistry;
    private final NamespacedKey cropIdKey;

    public CustomCropService(SkytreePlugin plugin, ItemRegistry itemRegistry) {

        this.itemRegistry = itemRegistry;
        this.cropIdKey = new NamespacedKey(plugin, "custom_crop_id");
    }

    // Mapping seed item ID -> Display Material & Produce
    private static class CropInfo {
        Material displayMaterial;
        Material produce;

        CropInfo(Material display, Material produce) {
            this.displayMaterial = display;
            this.produce = produce;
        }
    }

    private final Map<String, CropInfo> cropDefinitions = new HashMap<>() {
        {
            put("ancient_spores", new CropInfo(Material.GRASS_BLOCK, Material.GRASS_BLOCK));
            put("seed_nether_wart", new CropInfo(Material.NETHER_WART, Material.NETHER_WART));
            put("seed_carrot", new CropInfo(Material.CARROT, Material.CARROT));
            put("seed_potato", new CropInfo(Material.POTATO, Material.POTATO));
            put("seed_beetroot", new CropInfo(Material.BEETROOT, Material.BEETROOT));
            put("seed_melon", new CropInfo(Material.MELON, Material.MELON));
            put("seed_pumpkin", new CropInfo(Material.PUMPKIN, Material.PUMPKIN));
            put("seed_cocoa", new CropInfo(Material.COCOA, Material.COCOA_BEANS));
        }
    };

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String itemId = itemRegistry.getItemId(item);
        if (itemId == null || !cropDefinitions.containsKey(itemId))
            return;

        CropInfo info = cropDefinitions.get(itemId);

        spawnDisplay(event.getBlock().getLocation(), info.displayMaterial, itemId);
    }

    @EventHandler
    public void onInteractPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null) return;

        String itemId = itemRegistry.getItemId(item);
        if (itemId == null || !cropDefinitions.containsKey(itemId))
            return;

        CropInfo info = cropDefinitions.get(itemId);

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // Cocoa: place on jungle log
        if (itemId.equals("seed_cocoa")) {
            if (clicked.getType() != Material.JUNGLE_LOG && clicked.getType() != Material.JUNGLE_WOOD)
                return;

            Block target = clicked.getRelative(event.getBlockFace());
            if (target.getType() != Material.AIR)
                return;

            // Place cocoa
            target.setType(Material.COCOA);
            Cocoa cocoa = (Cocoa) target.getBlockData();
            cocoa.setAge(0);
            cocoa.setFacing(getOppositeFace(event.getBlockFace()));
            target.setBlockData(cocoa);

            // Spawn display
            spawnDisplay(target.getLocation(), info.displayMaterial, itemId);

            // Consume item
            item.setAmount(item.getAmount() - 1);
            event.setCancelled(true);
        }
    }

    private BlockFace getOppositeFace(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.NORTH;
            case EAST -> BlockFace.WEST;
            case WEST -> BlockFace.EAST;
            default -> BlockFace.NORTH;
        };
    }

    @EventHandler
    public void onGrow(BlockGrowEvent event) {
        // Find display at this location
        ItemDisplay display = findDisplay(event.getBlock().getLocation());
        if (display != null) {
            // Scale up based on growth
            if (event.getNewState().getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) event.getNewState().getBlockData();
                updateDisplayScale(display, ageable.getAge(), ageable.getMaximumAge());
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        ItemDisplay display = findDisplay(event.getBlock().getLocation());
        if (display != null) {
            String id = display.getPersistentDataContainer().get(cropIdKey, PersistentDataType.STRING);

            // Drop custom loot if fully grown (logic depends on crop)
            if (event.getBlock().getBlockData() instanceof Ageable) {
                Ageable age = (Ageable) event.getBlock().getBlockData();
                if (age.getAge() >= age.getMaximumAge()) {
                    // Drop resource + seed
                    if (id != null) {
                        // For simplicity dropping the seed itself as the "produce" for now, or look up
                        // better logic
                        // Actually user wants "Blaze Powder Seed" -> Grows "Blaze Powder".
                        // So we should drop BLAZE_POWDER + Seed.
                        dropCustomLoot(event.getBlock().getLocation(), id);
                    }
                } else {
                    // Drop seed only
                    if (id != null) {
                        ItemStack seed = itemRegistry.getItem(id);
                        if (seed != null)
                            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), seed);
                    }
                }
            }

            event.setDropItems(false); // Cancel vanilla drops
            display.remove(); // Kill entity
        }
    }

    // Helper to spawn display
    private void spawnDisplay(Location loc, Material mat, String id) {
        Location center = loc.clone().add(0.5, 0.0, 0.5); // Center of block
        ItemDisplay display = loc.getWorld().spawn(center, ItemDisplay.class);
        display.setItemStack(new ItemStack(mat));
        display.setBillboard(ItemDisplay.Billboard.VERTICAL); // Always face player
        display.getPersistentDataContainer().set(cropIdKey, PersistentDataType.STRING, id);

        // Initial scale small
        Transformation t = display.getTransformation();
        t.getScale().set(0.2f, 0.2f, 0.2f);
        t.getTranslation().set(0.0f, 0.2f, 0.0f); // Slight offset up
        display.setTransformation(t);
    }

    private ItemDisplay findDisplay(Location blockLoc) {
        // Search for entity at block center
        Location center = blockLoc.clone().add(0.5, 0.5, 0.5);
        Collection<Entity> entities = center.getWorld().getNearbyEntities(center, 0.5, 0.5, 0.5);
        for (Entity e : entities) {
            if (e instanceof ItemDisplay && e.getPersistentDataContainer().has(cropIdKey, PersistentDataType.STRING)) {
                return (ItemDisplay) e;
            }
        }
        return null;
    }

    private void updateDisplayScale(ItemDisplay display, int age, int maxAge) {
        float scale = 0.2f + ((float) age / maxAge) * 0.5f; // 0.2 to 0.7
        Transformation t = display.getTransformation();
        t.getScale().set(scale, scale, scale);
        display.setTransformation(t);
    }

    private void dropCustomLoot(Location loc, String cropId) {
        CropInfo info = cropDefinitions.get(cropId);
        ItemStack seed = itemRegistry.getItem(cropId);

        // Guaranteed seed back
        if (seed != null) {
            loc.getWorld().dropItemNaturally(loc, seed);
        }

        // Drop produce
        if (info != null && info.produce != null) {
            ItemStack produce = new ItemStack(info.produce);
            // Randomize yield 1-3
            int amount = 1 + new Random().nextInt(3);
            produce.setAmount(amount);
            loc.getWorld().dropItemNaturally(loc, produce);
        }
    }
}
