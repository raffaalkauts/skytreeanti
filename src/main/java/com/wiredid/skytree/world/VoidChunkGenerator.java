package com.wiredid.skytree.world;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

/**
 * Generates empty void chunks for the Skytree world
 * Simplified version compatible with Paper 1.21
 */
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunkData) {
        // Leave completely empty for void world
    }
}

