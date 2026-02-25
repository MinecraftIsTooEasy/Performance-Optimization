package com.inf1nlty.perfopt;

import net.minecraft.World;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存每个 world 每个列(x,z)的 getPrecipitationHeight 结果。
 * 该调用会逐列向下扫描，在大量 activeChunk 时消耗显著 CPU。
 * 缓存在方块更新或方块放置时通过 invalidateColumn 主动失效。
 * 每个 Tick 内同一列只计算一次，之后直接返回缓存值。
 */
public final class PrecipitationHeightCache
{
    // world -> (columnKey -> height)
    private static final Map<World, Map<Long, Integer>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PrecipitationHeightCache() {}

    private static long columnKey(int x, int z)
    {
        return (((long) x) << 32) | ((long) z & 0xFFFFFFFFL);
    }

    public static void invalidateColumn(World world, int x, int z)
    {
        if (world == null) return;

        long key = columnKey(x, z);

        synchronized (CACHE)
        {
            Map<Long, Integer> map = CACHE.get(world);

            if (map != null) map.remove(key);
        }
    }

    /**
     * 尝试从缓存获取；若不存在，计算并缓存。
     * @param world     目标世界
     * @param x         方块 X
     * @param z         方块 Z
     * @param computed  调用方已计算好的真实值（由 @Redirect 提供）
     * @return          缓存的或传入的高度值
     */
    public static int getOrCache(World world, int x, int z, int computed)
    {
        if (world == null) return computed;

        long key = columnKey(x, z);

        synchronized (CACHE)
        {
            Map<Long, Integer> map = CACHE.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
            Integer cached = map.get(key);

            if (cached != null) return cached;

            map.put(key, computed);
            return computed;
        }
    }

    /** 每 tick 开始时清空整个世界的缓存，保证下一 tick 数据新鲜。 */
    public static void flushForWorld(World world)
    {
        if (world == null) return;

        synchronized (CACHE)
        {
            CACHE.remove(world);
        }
    }
}