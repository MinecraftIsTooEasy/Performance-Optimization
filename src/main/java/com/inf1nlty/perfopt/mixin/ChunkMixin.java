package com.inf1nlty.perfopt.mixin;

import com.inf1nlty.perfopt.ChunkTickThrottler;
import com.inf1nlty.perfopt.RelightLimiter;
import net.minecraft.Chunk;
import net.minecraft.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chunk.class)
public class ChunkMixin
{
    @Inject(method = "performPendingSkylightUpdatesIfPossible", at = @At("HEAD"), cancellable = true)
    private void onPerformPendingSkylightUpdatesIfPossible(CallbackInfoReturnable<Boolean> cir)
    {
        Chunk self = (Chunk)(Object)this;
        World world = self.worldObj;

        if (world != null && !RelightLimiter.tryIncrement(world))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "performPendingBlocklightUpdatesIfPossible", at = @At("HEAD"), cancellable = true)
    private void onPerformPendingBlocklightUpdatesIfPossible(CallbackInfoReturnable<Boolean> cir)
    {
        Chunk self = (Chunk)(Object)this;
        World world = self.worldObj;

        if (world != null && !RelightLimiter.tryIncrement(world))
        {
            cir.setReturnValue(false);
        }
    }

    // 对远离玩家的 chunk 降频 updateSkylight（每隔几 tick 才执行一次）
    @Inject(method = "updateSkylight", at = @At("HEAD"), cancellable = true)
    private void onUpdateSkylight(boolean force, CallbackInfoReturnable<Boolean> cir)
    {
        if (force) return; // force=true 时不降频，保证亮度修正及时

        Chunk self = (Chunk)(Object)this;
        World world = self.worldObj;

        if (world != null && world.isRemote) return;

        if (world != null)
        {
            int cx = self.xPosition;
            int cz = self.zPosition;

            if (!ChunkTickThrottler.shouldTickChunkMaintenance(world, cx, cz))
            {
                cir.setReturnValue(false);
            }
        }
    }

    // 对远离玩家的 chunk 降频沙子下落计算
    @Inject(method = "performPendingSandFallsIfPossible", at = @At("HEAD"), cancellable = true)
    private void onPerformPendingSandFallsIfPossible(CallbackInfoReturnable<Boolean> cir)
    {
        Chunk self = (Chunk)(Object)this;
        World world = self.worldObj;

        if (world != null && world.isRemote) return;

        if (world != null)
        {
            int cx = self.xPosition;
            int cz = self.zPosition;

            if (!ChunkTickThrottler.shouldTickChunkMaintenance(world, cx, cz))
            {
                cir.setReturnValue(false);
            }
        }
    }
}