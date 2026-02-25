package com.inf1nlty.perfopt;

import net.minecraft.World;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RelightLimiter
{
    private static final Map<World, AtomicInteger> COUNTS = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    public static final int MAX_RELIGHT_CALLS_PER_TICK = 64;

    private RelightLimiter() {}

    public static boolean tryIncrement(World world)
    {
        if (world == null) return false;

        AtomicInteger ai;
        synchronized (COUNTS)
        {
            ai = COUNTS.computeIfAbsent(world, k -> new AtomicInteger(0));
        }
        int prev = ai.getAndUpdate(v -> v >= MAX_RELIGHT_CALLS_PER_TICK ? v : v + 1);

        return prev < MAX_RELIGHT_CALLS_PER_TICK;
    }

    public static void resetForWorld(World world)
    {
        if (world == null) return;

        synchronized (COUNTS)
        {
            COUNTS.remove(world);
        }
    }
}