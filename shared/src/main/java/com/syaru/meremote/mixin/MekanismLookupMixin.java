package com.syaru.meremote.mixin;

import com.syaru.meremote.client.ClientMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="mekanism.common.util.WorldUtils",remap=false)
public abstract class MekanismLookupMixin {
    @Inject(method="getTileEntity(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",at=@At("HEAD"),cancellable=true)
    private static void remote$mirror(BlockGetter world,BlockPos pos,CallbackInfoReturnable<BlockEntity> result) {
        var mirror=ClientMirror.lookup(world,pos);if(mirror!=null)result.setReturnValue(mirror);
    }
}
