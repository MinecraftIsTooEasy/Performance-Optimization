package com.inf1nlty.perfopt.mixin;

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
}