package com.inf1nlty.perfopt.mixin;

import com.inf1nlty.perfopt.PendingBlockUpdateIndex;
import net.minecraft.NextTickListEntry;
import net.minecraft.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(WorldServer.class)
@SuppressWarnings("unchecked,rawtypes")
public class WorldScheduleIndexMixin {

    @Redirect(method = "scheduleBlockUpdateWithPriority(IIIIII)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean onSetAdd_schedule(Set set, Object entry) {
        boolean res = set.add(entry);
        if (res && entry instanceof NextTickListEntry listEntry) {
            WorldServer self = (WorldServer) (Object) this;
            PendingBlockUpdateIndex.addToBucket(self, listEntry.xCoord >> 4, listEntry.zCoord >> 4, listEntry);
        }
        return res;
    }

    @Redirect(method = "scheduleBlockUpdateFromLoad(IIIIII)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean onSetAdd_fromLoad(Set set, Object entry) {
        boolean res = set.add(entry);
        if (res && entry instanceof NextTickListEntry listEntry) {
            WorldServer self = (WorldServer) (Object) this;
            PendingBlockUpdateIndex.addToBucket(self, listEntry.xCoord >> 4, listEntry.zCoord >> 4, listEntry);
        }
        return res;
    }
}