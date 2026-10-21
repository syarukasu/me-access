package com.syaru.meremote.platform;

import com.syaru.meremote.MachineAdapter;
import mekanism.api.security.ISecurityUtils;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MekanismAdapter implements MachineAdapter {
    @Override public int status(BlockEntity target) {
        var tile=(TileEntityMekanism)target;
        return com.syaru.meremote.MekanismDiagnostics.read(tile,mekanism.common.util.MekanismUtils.canFunction(tile));
    }
    public static final MekanismAdapter INSTANCE=new MekanismAdapter();
    @Override public boolean supports(BlockEntity target) { return target instanceof TileEntityMekanism tile&&tile.hasGui()&&!(tile instanceof TileEntityMultiblock<?>); }
    @Override public boolean canAccess(ServerPlayer player,BlockEntity target) {
        return !player.isSpectator()&&player.mayBuild()&&player.level().mayInteract(player,target.getBlockPos())
                && ISecurityUtils.INSTANCE.canAccess(player,(TileEntityMekanism)target);
    }
    @Override public boolean open(ServerPlayer player,BlockEntity target) {
        var before=player.containerMenu;((TileEntityMekanism)target).openGui(player);return player.containerMenu!=before;
    }
    @Override public BlockEntity menuTarget(AbstractContainerMenu menu) { return menu instanceof MekanismTileContainer<?> tile?tile.getTileEntity():null; }
}
