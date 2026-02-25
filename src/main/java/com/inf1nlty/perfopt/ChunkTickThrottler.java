package com.inf1nlty.perfopt;

import net.minecraft.World;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对远离玩家的 chunk 进行降频处理。
 * 近距离 chunk（玩家附近 NEAR_CHUNK_RADIUS 个 chunk 内）：正常每 tick 运行所有逻辑。
 * 中距离 chunk：每 MID_INTERVAL tick 运行一次冰雪/雷暴等非必要逻辑。
 * 远距离 chunk：每 FAR_INTERVAL tick 运行一次，或完全跳过。
 * 不影响随机方块刻（已由 BlockMantleOrCoreMixin 处理）。
 */
public final class ChunkTickThrottler
{
    /** 近距离半径（chunk 数），在此范围内不做降频 */
    public static final int NEAR_CHUNK_RADIUS = 4;

    /** 中距离降频间隔：每 N tick 执行一次冰雪/雷暴逻辑 */
    public static final int MID_INTERVAL = 3;

    /** 远距离降频间隔：每 N tick 执行一次冰雪/雷暴逻辑 */
    public static final int FAR_INTERVAL = 8;

    /** 超出此半径（chunk）完全跳过冰雪/雷暴逻辑 */
    public static final int SKIP_CHUNK_RADIUS = 12;

    // world -> (chunkKey -> tickCounter)
    private static final Map<World, Map<Long, Integer>> COUNTERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ChunkTickThrottler() {}

    private static long chunkKey(int chunkX, int chunkZ)
    {
        return (((long) chunkX) << 32) | ((long) chunkZ & 0xFFFFFFFFL);
    }

    /**
     * 根据该 chunk 与最近玩家的距离，判断本 tick 是否应执行冰雪/雷暴等耗时逻辑。
     *
     * @param world   目标世界
     * @param chunkX  chunk X 坐标
     * @param chunkZ  chunk Z 坐标
     * @return true = 本 tick 应执行，false = 跳过
     */
    public static boolean shouldTickWeather(World world, int chunkX, int chunkZ)
    {
        if (world == null) return true;

        int minDistSq = getMinPlayerChunkDistSq(world, chunkX, chunkZ);

        // 近距离 —— 始终执行
        if (minDistSq <= NEAR_CHUNK_RADIUS * NEAR_CHUNK_RADIUS) return true;

        // 超过跳过半径 —— 完全跳过
        if (minDistSq > SKIP_CHUNK_RADIUS * SKIP_CHUNK_RADIUS) return false;

        // 中距离 / 远距离 —— 按计数器降频
        int interval = (minDistSq <= 7 * 7) ? MID_INTERVAL : FAR_INTERVAL;

        return shouldTickByInterval(world, chunkX, chunkZ, interval);
    }

    /**
     * 判断是否应对该 chunk 执行 skylight / sandfall 等更新。
     * 比 weather 的阈值更宽松（允许更远的 chunk 执行）。
     */
    public static boolean shouldTickChunkMaintenance(World world, int chunkX, int chunkZ)
    {
        if (world == null) return true;

        int minDistSq = getMinPlayerChunkDistSq(world, chunkX, chunkZ);

        // 近距离 —— 始终执行
        if (minDistSq <= NEAR_CHUNK_RADIUS * NEAR_CHUNK_RADIUS) return true;

        // 超过较大半径 —— 每 FAR_INTERVAL 执行一次
        if (minDistSq > SKIP_CHUNK_RADIUS * SKIP_CHUNK_RADIUS)
        {
            return shouldTickByInterval(world, chunkX, chunkZ, FAR_INTERVAL);
        }

        return shouldTickByInterval(world, chunkX, chunkZ, MID_INTERVAL);
    }

    private static int getMinPlayerChunkDistSq(World world, int chunkX, int chunkZ)
    {
        int minDistSq = Integer.MAX_VALUE;

        for (Object obj : world.playerEntities)
        {
            net.minecraft.EntityPlayer player = (net.minecraft.EntityPlayer) obj;
            int pcx = ((int) Math.floor(player.posX)) >> 4;
            int pcz = ((int) Math.floor(player.posZ)) >> 4;
            int dx = chunkX - pcx;
            int dz = chunkZ - pcz;
            int distSq = dx * dx + dz * dz;

            if (distSq < minDistSq) minDistSq = distSq;
        }

        return minDistSq;
    }

    private static boolean shouldTickByInterval(World world, int chunkX, int chunkZ, int interval)
    {
        long key = chunkKey(chunkX, chunkZ);

        synchronized (COUNTERS)
        {
            Map<Long, Integer> map = COUNTERS.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
            int count = map.getOrDefault(key, 0);
            int next = (count + 1) % interval;
            map.put(key, next);
            return next == 0;
        }
    }
}