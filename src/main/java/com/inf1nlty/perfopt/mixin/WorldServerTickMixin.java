package com.inf1nlty.perfopt.mixin;

import com.inf1nlty.perfopt.ChunkTickThrottler;
import com.inf1nlty.perfopt.PrecipitationHeightCache;
import net.minecraft.Block;
import net.minecraft.Chunk;
import net.minecraft.World;
import net.minecraft.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 针对 WorldServer.tickBlocksAndAmbiance 的极致优化 Mixin。
 * 优化点：
 * 1. 对远离玩家的 chunk 跳过/降频雷暴、冰雪生成逻辑。
 * 2. 对 getPrecipitationHeight 进行列级缓存，避免重复扫描同一列。（缓存每 tick 在 WorldMixin 中刷新）
 * 3. updateSkylight / performPendingSandFalls 的降频由 ChunkMixin 处理。
 * 所有优化均保留近距离 chunk（玩家周围 {@link ChunkTickThrottler#NEAR_CHUNK_RADIUS} 格内）的完整行为。
 */
@Mixin(WorldServer.class)
public class WorldServerTickMixin
{
    // 当前正在处理的 chunk 坐标（thread-local，防止多世界并发干扰）
    @Unique private final ThreadLocal<Integer> currentChunkX = ThreadLocal.withInitial(() -> 0);

    @Unique private final ThreadLocal<Integer> currentChunkZ = ThreadLocal.withInitial(() -> 0);

    @Redirect(method = "tickBlocksAndAmbiance", at = @At(value = "INVOKE", target = "Lnet/minecraft/WorldServer;getChunkFromChunkCoords(II)Lnet/minecraft/Chunk;"))
    private Chunk onGetChunk(WorldServer self, int chunkX, int chunkZ)
    {
        currentChunkX.set(chunkX);
        currentChunkZ.set(chunkZ);
        return self.getChunkFromChunkCoords(chunkX, chunkZ);
    }

    // 2. For distant chunks, skip thunderstorm logic
    @Redirect(method = "tickBlocksAndAmbiance", at = @At(value = "INVOKE", target = "Lnet/minecraft/WorldServer;isThundering(Z)Z"))
    private boolean onIsThundering(WorldServer self, boolean checkDimension)
    {
        int cx = currentChunkX.get();
        int cz = currentChunkZ.get();

        if (!ChunkTickThrottler.shouldTickWeather(self, cx, cz)) return false;

        return self.isThundering(checkDimension);
    }

    // 3. Cache getPrecipitationHeight (the original method performs a full column scan every time).
    @Redirect(method = "tickBlocksAndAmbiance", at = @At(value = "INVOKE", target = "Lnet/minecraft/WorldServer;getPrecipitationHeight(II)I"))
    private int onGetPrecipitationHeight(WorldServer self, int x, int z)
    {
        int computed = self.getPrecipitationHeight(x, z);
        return PrecipitationHeightCache.getOrCache(self, x, z, computed);
    }

    // 4. Skip freezing logic for distant chunks: Intercept isBlockFreezableNaturally
    @Redirect(method = "tickBlocksAndAmbiance", at = @At(value = "INVOKE", target = "Lnet/minecraft/WorldServer;isBlockFreezableNaturally(III)Z"))
    private boolean onIsBlockFreezableNaturally(WorldServer self, int x, int y, int z)
    {
        int cx = currentChunkX.get();
        int cz = currentChunkZ.get();

        if (!ChunkTickThrottler.shouldTickWeather(self, cx, cz)) return false;

        return self.isBlockFreezableNaturally(x, y, z);
    }

    // 5. Skip snowfall for distant chunks: Intercept canSnowAt
    @Redirect(method = "tickBlocksAndAmbiance", at = @At(value = "INVOKE", target = "Lnet/minecraft/WorldServer;canSnowAt(III)Z"))
    private boolean onCanSnowAt(WorldServer self, int x, int y, int z)
    {
        int cx = currentChunkX.get();
        int cz = currentChunkZ.get();

        if (!ChunkTickThrottler.shouldTickWeather(self, cx, cz)) return false;

        return self.canSnowAt(x, y, z);
    }

    // 6. Skip fillWithRain for distant chunks (fills blocks with rain, such as wet farmland).
    @Redirect(method = "tickBlocksAndAmbiance", at = @At(value = "INVOKE", target = "Lnet/minecraft/Block;fillWithRain(Lnet/minecraft/World;III)V"))
    private void onFillWithRain(Block block, World world, int x, int y, int z)
    {
        int cx = currentChunkX.get();
        int cz = currentChunkZ.get();

        if (ChunkTickThrottler.shouldTickWeather(world, cx, cz))
        {
            block.fillWithRain(world, x, y, z);
        }
    }
}
