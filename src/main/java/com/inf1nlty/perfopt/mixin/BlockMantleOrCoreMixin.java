package com.inf1nlty.perfopt.mixin;

import com.inf1nlty.perfopt.WorldActivityCache;
import net.minecraft.BlockMantleOrCore;
import net.minecraft.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(BlockMantleOrCore.class)
public class BlockMantleOrCoreMixin
{
    @Unique
    private static final int THINNING = 4;

    @Inject(method = "updateTick", at = @At("HEAD"), cancellable = true)
    private void onUpdateTick(World world, int x, int y, int z, Random rand, CallbackInfoReturnable<Boolean> cir)
    {
        if (world.isRemote)
        {
            cir.setReturnValue(false);
            return;
        }

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!WorldActivityCache.isChunkActive(world, chunkX, chunkZ))
        {
            cir.setReturnValue(false);
            return;
        }

        if (THINNING > 1 && rand.nextInt(THINNING) != 0)
        {
            cir.setReturnValue(false);
        }

    }
}