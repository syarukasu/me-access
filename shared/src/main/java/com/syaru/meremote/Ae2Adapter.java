package com.syaru.meremote;

import appeng.blockentity.crafting.MolecularAssemblerBlockEntity;
import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.InscriberMenu;
import appeng.menu.implementations.MolecularAssemblerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class Ae2Adapter implements MachineAdapter {
    @Override public int status(BlockEntity target) {
        if(target instanceof InscriberBlockEntity inscriber) {
            if(inscriber.isSmash())return MachineStatus.WORKING.bit();
            if(inscriber.getProcessingTime()>0)return MachineStatus.IN_PROGRESS.bit();
        }
        if(target instanceof MolecularAssemblerBlockEntity assembler&&assembler.getCraftingProgress()>0)return MachineStatus.IN_PROGRESS.bit();
        return MachineStatus.UNKNOWN.bit();
    }
    @Override public boolean supports(BlockEntity target) {
        return target instanceof InscriberBlockEntity || target instanceof MolecularAssemblerBlockEntity;
    }
    @Override public boolean canAccess(ServerPlayer player, BlockEntity target) {
        return !player.isSpectator() && player.mayBuild() && player.level().mayInteract(player, target.getBlockPos());
    }
    @Override public boolean open(ServerPlayer player, BlockEntity target) {
        return MenuOpener.open(target instanceof InscriberBlockEntity ? InscriberMenu.TYPE : MolecularAssemblerMenu.TYPE,
                player, new com.syaru.meremote.platform.RemoteMenuLocator(target.getBlockPos(),RemoteSessions.token(player)));
    }
    @Override public BlockEntity menuTarget(AbstractContainerMenu menu) {
        return menu instanceof AEBaseMenu ae && supports(ae.getBlockEntity()) ? ae.getBlockEntity() : null;
    }
}
