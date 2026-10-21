package com.syaru.meremote;

import appeng.api.networking.IGrid;
import com.syaru.meremote.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

public interface TerminalHost {
    MachineBook book();
    Container cards();
    boolean hasCard();
    Level remoteLevel();
    IGrid grid();
    boolean access(ServerPlayer player, boolean requireCard);
    boolean registrationAccess(ServerPlayer player);
    boolean tick(ServerPlayer player, AbstractContainerMenu menu);
    int lockedPlayerSlot();
    void changed();
    CompoundTag reference();
    void openStorage(ServerPlayer player);
    default void openMachines(ServerPlayer player) {
        if(access(player,true)) Platform.openMachines(player,this);
        else Registration.tell(player,"no_card");
    }
}
