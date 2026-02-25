package com.inf1nlty.perfopt.mixin;

import com.inf1nlty.perfopt.WorldActivityCache;
import com.inf1nlty.perfopt.RelightLimiter;
import net.minecraft.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldMixin {

    @Unique
    private static final int ACTIVE_RANGE_BLOCKS = 32;

    @Inject(method = "tickBlocksAndAmbiance", at = @At("HEAD"))
    private void onTickBlocksAndAmbiance(CallbackInfo ci)
    {
        World self = (World)(Object)this;

        WorldActivityCache.rebuildForWorld(self, ACTIVE_RANGE_BLOCKS);

        RelightLimiter.resetForWorld(self);
    }
}