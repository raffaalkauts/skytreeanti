package com.wiredid.skytree.api;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Service for detecting multiblock structures
 */
public class MultiblockService {

    /**
     * Checks if a block is part of a Barrel structure
     * Structure: Barrel with Trapdoor on top
     * 
     * @param clickedBlock The block clicked (should be the Barrel or Trapdoor)
     * @return true if valid Barrel
     */
    public boolean isBarrel(Block clickedBlock) {
        if (clickedBlock.getType() == Material.BARREL || clickedBlock.getType() == Material.CAULDRON) {
            Block above = clickedBlock.getRelative(BlockFace.UP);
            return above.getType().name().contains("TRAPDOOR");
        }
        if (clickedBlock.getType().name().contains("TRAPDOOR")) {
            Block below = clickedBlock.getRelative(BlockFace.DOWN);
            return below.getType() == Material.BARREL || below.getType() == Material.CAULDRON;
        }
        return false;
    }

    /**
     * Checks if a block is part of a Crucible structure
     * Structure: Cauldron with Heat Source below (Fire, Lava, Magma)
     * 
     * @param clickedBlock The block clicked (should be the Cauldron)
     * @return true if valid Crucible
     */
    public boolean isCrucible(Block clickedBlock) {
        if (clickedBlock.getType() != Material.CAULDRON)
            return false;

        Block below = clickedBlock.getRelative(BlockFace.DOWN);
        Material type = below.getType();
        return type == Material.FIRE || type == Material.LAVA || type == Material.MAGMA_BLOCK
                || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE;
    }

    /**
     * Checks if a block is part of a Compressor structure
     * Structure: Dispenser (Bottom) + Piston (Top)
     * Click: Piston
     */
    public boolean isCompressor(Block clickedBlock) {
        if (clickedBlock.getType() != Material.PISTON)
            return false;

        Block below = clickedBlock.getRelative(BlockFace.DOWN);
        return below.getType() == Material.DISPENSER;
    }

    /**
     * Checks if a block is part of a Pulverizer structure
     * Structure: Dispenser (Bottom) + Iron Block (Top)
     * Click: Iron Block
     */
    public boolean isPulverizer(Block clickedBlock) {
        if (clickedBlock.getType() != Material.IRON_BLOCK)
            return false;

        Block below = clickedBlock.getRelative(BlockFace.DOWN);
        return below.getType() == Material.DISPENSER;
    }

    /**
     * Checks if a block is part of an Electric Furnace structure
     * Structure: Blast Furnace (Bottom) + Iron Trapdoor (Top)
     * Click: Blast Furnace
     */
    public boolean isElectricFurnace(Block clickedBlock) {
        if (clickedBlock.getType() != Material.BLAST_FURNACE)
            return false;

        Block above = clickedBlock.getRelative(BlockFace.UP);
        return above.getType() == Material.IRON_TRAPDOOR;
    }

    /**
     * Checks if a block is part of a Coal Generator structure
     * Structure: Furnace (Bottom) + Stone Pressure Plate (Top)
     * Click: Furnace
     */
    public boolean isCoalGenerator(Block clickedBlock) {
        if (clickedBlock.getType() != Material.FURNACE)
            return false;

        Block above = clickedBlock.getRelative(BlockFace.UP);
        return above.getType() == Material.STONE_PRESSURE_PLATE;
    }

    /**
     * Checks if a block is part of a Lava Generator structure
     * Structure: Magma Block (Bottom) + Glass (Top)
     * Click: Glass
     */
    public boolean isLavaGenerator(Block clickedBlock) {
        if (clickedBlock.getType() != Material.GLASS)
            return false;

        Block below = clickedBlock.getRelative(BlockFace.DOWN);
        return below.getType() == Material.MAGMA_BLOCK;
    }

    /**
     * Checks if a block is part of a Solar Generator structure
     * Structure: Daylight Detector (Bottom) + Glass Pane (Top)
     * Click: Daylight Detector
     */
    public boolean isSolarGenerator(Block clickedBlock) {
        if (clickedBlock.getType() != Material.DAYLIGHT_DETECTOR)
            return false;

        Block above = clickedBlock.getRelative(BlockFace.UP);
        return above.getType().name().contains("GLASS_PANE");
    }

    /**
     * General multiblock check by type
     */
    public boolean isMultiblock(org.bukkit.Location loc, String type) {
        if (loc == null || type == null) return false;
        Block block = loc.getBlock();
        
        if (type.equalsIgnoreCase("sieve")) {
            return block.getType() == Material.OAK_FENCE
                    && (block.getRelative(BlockFace.DOWN).getType() == Material.CHEST
                    || block.getRelative(BlockFace.DOWN).getType() == Material.TRAPPED_CHEST);
        }
        
        return false;
    }
}
