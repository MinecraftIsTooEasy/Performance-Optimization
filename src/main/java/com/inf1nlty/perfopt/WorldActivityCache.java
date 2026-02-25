package com.inf1nlty.perfopt;

import net.minecraft.EntityPlayer;
import net.minecraft.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class WorldActivityCache
{
    private static final Map<World, Set<Long>> ACTIVE_CHUNKS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void rebuildForWorld(World world, int activeRangeBlocks)
    {
        if (world == null) return;

        int chunkRadius = Math.max(0, (activeRangeBlocks + 15) / 16);

        Set<Long> set = Collections.synchronizedSet(new HashSet<>());

        for (Object object : world.playerEntities)
        {
            EntityPlayer player = (EntityPlayer) object;

            int pcx = ((int) Math.floor(player.posX)) >> 4;
            int pcz = ((int) Math.floor(player.posZ)) >> 4;

            for (int dx = -chunkRadius; dx <= chunkRadius; dx++)
            {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++)
                {
                    long key = ((long) (pcx + dx) << 32) | ((long) (pcz + dz) & 0xffffffffL);
                    set.add(key);
                }
            }
        }

        if (set.isEmpty())
        {
            ACTIVE_CHUNKS.remove(world);
        }
        else
        {
            ACTIVE_CHUNKS.put(world, set);
        }
    }

    public static boolean isChunkActive(World world, int chunkX, int chunkZ)
    {
        Set<Long> set = ACTIVE_CHUNKS.get(world);

        if (set == null) return false;

        long key = ((long) chunkX << 32) | ((long) chunkZ & 0xffffffffL);

        return set.contains(key);
    }
}