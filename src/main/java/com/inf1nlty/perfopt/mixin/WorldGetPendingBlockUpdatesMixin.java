package com.inf1nlty.perfopt.mixin;

import com.inf1nlty.perfopt.PendingBlockUpdateCache;
import net.minecraft.Chunk;
import net.minecraft.World;
import net.minecraft.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Mixin(WorldServer.class)
@SuppressWarnings("unchecked,rawtypes")
public class WorldGetPendingBlockUpdatesMixin {

    @Shadow private TreeSet pendingTickListEntriesTreeSet;
    @Shadow private List pendingTickListEntriesThisTick;
    @Shadow private Set pendingTickListEntriesHashSet;

    @Inject(method = "getPendingBlockUpdates", at = @At("HEAD"), cancellable = true)
    private void onGetPendingBlockUpdates(Chunk par1Chunk, boolean par2, CallbackInfoReturnable<List> cir)
    {
        World self = (World)(Object)this;
        int chunkX = par1Chunk.getChunkCoordIntPair().chunkXPos;
        int chunkZ = par1Chunk.getChunkCoordIntPair().chunkZPos;

        List res = PendingBlockUpdateCache.getCachedOrBuild(
                self,
                chunkX,
                chunkZ,
                pendingTickListEntriesTreeSet,
                pendingTickListEntriesThisTick,
                pendingTickListEntriesHashSet,
                par2
        );

        if (res == null || res.isEmpty())
        {
            cir.setReturnValue(null);
        }
        else
        {
            cir.setReturnValue(res);
        }
    }
}