package org.by1337.bairdrop.worldGuardHook;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SavedBlocksData {
    private final Map<Location, BlockData> savedBlocks = new HashMap<>();
    private final World world;

    public SavedBlocksData(World world) {
        this.world = world;
    }

    public void saveBlock(Location loc, BlockData data) {
        savedBlocks.put(loc.clone(), data.clone());
    }

    public void setBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    public void restore() {
        Set<Chunk> affectedChunks = new HashSet<>();
        
        for (Map.Entry<Location, BlockData> entry : savedBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();
            Block block = loc.getBlock();
            block.setBlockData(data, false);
            affectedChunks.add(block.getChunk());
        }
        
        for (Chunk chunk : affectedChunks) {
            if (chunk.isLoaded()) {
                world.refreshChunk(chunk.getX(), chunk.getZ());
            }
        }
        
        savedBlocks.clear();
    }

    public boolean isEmpty() {
        return savedBlocks.isEmpty();
    }

    public World getWorld() {
        return world;
    }
}
