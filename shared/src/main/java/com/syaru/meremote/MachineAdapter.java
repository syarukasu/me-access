package com.syaru.meremote;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface MachineAdapter {
    boolean supports(BlockEntity target);
    boolean canAccess(ServerPlayer player, BlockEntity target);
    boolean open(ServerPlayer player, BlockEntity target);
    BlockEntity menuTarget(AbstractContainerMenu menu);
    default int status(BlockEntity target) { return MachineStatus.UNKNOWN.bit(); }
}
