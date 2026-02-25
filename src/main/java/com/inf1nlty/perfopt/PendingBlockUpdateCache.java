package com.inf1nlty.perfopt;

import net.minecraft.World;
import net.minecraft.NextTickListEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存每个 world 的按 chunk 分组的 pending block updates。
 * 现在优先从写时索引取（PendingBlockUpdateIndex），fallback 到读时构建。
 */
public final class PendingBlockUpdateCache
{
    // world -> (chunkKey -> cachedList)
    private static final Map<World, Map<Long, List<NextTickListEntry>>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    // world -> set of invalid chunkKeys
    private static final Map<World, Set<Long>> INVALID = Collections.synchronizedMap(new WeakHashMap<>());

    private PendingBlockUpdateCache() {}

    private static long chunkKey(int chunkX, int chunkZ)
    {
        return (((long)chunkX) << 32) | ((long)chunkZ & 0xffffffffL);
    }

    public static void invalidateChunk(World world, int chunkX, int chunkZ)
    {
        if (world == null) return;

        long key = chunkKey(chunkX, chunkZ);

        synchronized (INVALID)
        {
            INVALID.computeIfAbsent(world, w -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(key);
        }

        synchronized (CACHE)
        {
            Map<Long, List<NextTickListEntry>> map = CACHE.get(world);

            if (map != null) map.remove(key);
        }

        PendingBlockUpdateIndex.clearBucket(world, chunkX, chunkZ);
    }

    public static List<NextTickListEntry> getCachedOrBuild(World world, int chunkX, int chunkZ,
                                                           Iterable<NextTickListEntry> treeSetIter,
                                                           Iterable<NextTickListEntry> thisTickListIter,
                                                           Collection<NextTickListEntry> hashSetRef,
                                                           boolean removeFlag)
    {
        if (world == null) return null;

        long key = chunkKey(chunkX, chunkZ);

        // 1) try write-time index first
        List<NextTickListEntry> indexSnapshot = PendingBlockUpdateIndex.getBucketSnapshot(world, chunkX, chunkZ);

        if (indexSnapshot != null && !indexSnapshot.isEmpty())
        {
            if (removeFlag)
            {
                for (NextTickListEntry e : indexSnapshot)
                {
                    if (hashSetRef != null) hashSetRef.remove(e);
                    // also remove from index
                    PendingBlockUpdateIndex.removeFromBucket(world, chunkX, chunkZ, e);
                }
            }

            return new ArrayList<>(indexSnapshot);
        }

        // 2) check read-time cache
        synchronized (CACHE)
        {
            Map<Long, List<NextTickListEntry>> map = CACHE.get(world);
            Set<Long> inval = INVALID.get(world);

            if (map != null && map.containsKey(key) && (inval == null || !inval.contains(key)))
            {
                List<NextTickListEntry> cached = map.get(key);
                return cached == null ? null : new ArrayList<>(cached);
            }
        }

        // 3) fallback: build by scanning the provided iterables (original logic)
        List<NextTickListEntry> collected = null;

        int xMin = (chunkX << 4) - 2;
        int xMax = xMin + 16 + 2;
        int zMin = (chunkZ << 4) - 2;
        int zMax = zMin + 16 + 2;

        for (int pass = 0; pass < 2; pass++)
        {
            Iterable<NextTickListEntry> itSrc = (pass == 0) ? treeSetIter : thisTickListIter;

            if (itSrc == null) continue;

            Iterator<NextTickListEntry> it = itSrc.iterator();

            while (it.hasNext())
            {
                NextTickListEntry entry = it.next();

                int xCoord = entry.xCoord;
                int zCoord = entry.zCoord;

                if (xCoord >= xMin && xCoord < xMax && zCoord >= zMin && zCoord < zMax)
                {
                    if (removeFlag)
                    {
                        it.remove();

                        if (hashSetRef != null)
                        {
                            hashSetRef.remove(entry);
                        }
                        // defensive: remove from write-time index if present

                        PendingBlockUpdateIndex.removeFromBucket(world, chunkX, chunkZ, entry);
                    }

                    if (collected == null) collected = new ArrayList<>();

                    collected.add(entry);
                }
            }
        }

        // cache
        synchronized (CACHE)
        {
            Map<Long, List<NextTickListEntry>> map = CACHE.computeIfAbsent(world, k -> new ConcurrentHashMap<>());
            map.put(key, collected == null ? Collections.emptyList() : new ArrayList<>(collected));
        }
        // clear invalid mark
        synchronized (INVALID)
        {
            Set<Long> s = INVALID.get(world);

            if (s != null) s.remove(key);
        }

        return collected == null ? null : new ArrayList<>(collected);
    }
}