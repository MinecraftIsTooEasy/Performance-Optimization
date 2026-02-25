package com.inf1nlty.perfopt;

import net.minecraft.World;
import net.minecraft.NextTickListEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingBlockUpdateIndex
{
    private static final Map<World, Map<Long, List<NextTickListEntry>>> INDEX = Collections.synchronizedMap(new WeakHashMap<>());

    private PendingBlockUpdateIndex() {}

    private static long chunkKey(int chunkX, int chunkZ)
    {
        return (((long)chunkX) << 32) | ((long)chunkZ & 0xffffffffL);
    }

    public static void addToBucket(World world, int chunkX, int chunkZ, NextTickListEntry entry)
    {
        if (world == null || entry == null) return;

        long key = chunkKey(chunkX, chunkZ);

        synchronized (INDEX)
        {
            Map<Long, List<NextTickListEntry>> map = INDEX.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
            List<NextTickListEntry> list = map.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));
            list.add(entry);
        }
    }

    public static void removeFromBucket(World world, int chunkX, int chunkZ, NextTickListEntry entry)
    {
        if (world == null || entry == null) return;

        long key = chunkKey(chunkX, chunkZ);

        synchronized (INDEX)
        {
            Map<Long, List<NextTickListEntry>> map = INDEX.get(world);

            if (map == null) return;

            List<NextTickListEntry> list = map.get(key);

            if (list == null) return;

            list.remove(entry);

            if (list.isEmpty()) map.remove(key);
        }
    }

    public static List<NextTickListEntry> getBucketSnapshot(World world, int chunkX, int chunkZ)
    {
        if (world == null) return null;

        long key = chunkKey(chunkX, chunkZ);

        synchronized (INDEX)
        {
            Map<Long, List<NextTickListEntry>> map = INDEX.get(world);

            if (map == null) return null;

            List<NextTickListEntry> list = map.get(key);

            if (list == null) return null;

            return new ArrayList<>(list);
        }
    }

    public static void clearBucket(World world, int chunkX, int chunkZ)
    {
        if (world == null) return;

        long key = chunkKey(chunkX, chunkZ);

        synchronized (INDEX)
        {
            Map<Long, List<NextTickListEntry>> map = INDEX.get(world);

            if (map == null) return;

            map.remove(key);

            if (map.isEmpty()) INDEX.remove(world);
        }
    }
}