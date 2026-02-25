package com.inf1nlty.perfopt.mixin;

import com.inf1nlty.perfopt.PendingBlockUpdateCache;
import net.minecraft.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldScheduleInvalidateMixin {

    @Inject(method = "scheduleBlockUpdateWithPriority", at = @At("HEAD"))
    private void onScheduleBlockUpdateWithPriority(int x, int y, int z, int blockID, int delay, int priority, CallbackInfo ci)
    {
        World self = (World)(Object)this;

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        PendingBlockUpdateCache.invalidateChunk(self, chunkX, chunkZ);
    }
}