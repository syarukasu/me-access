package com.syaru.meremote;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import appeng.menu.AEBaseMenu;

public record Control(int container, int action, UUID target) {
    public static final int OPEN=0, REMOVE=1, REFRESH=2, STORAGE=3, READY=4, RETURN=5, MACHINES=6, REJECT=7, CARDS=8, BIND=9;
    public static final UUID NONE=new UUID(0,0);
    public void write(FriendlyByteBuf buffer) { buffer.writeVarInt(container);buffer.writeVarInt(action);buffer.writeUUID(target); }
    public static Control read(FriendlyByteBuf buffer) { return new Control(buffer.readVarInt(),buffer.readVarInt(),buffer.readUUID()); }
    public void handle(ServerPlayer player) {
        if (player==null || !player.isAlive() || player.isSpectator()) return;
        if (action==READY||action==REJECT) { RemoteSessions.ready(player,target,action==READY);return; }
        if (action==RETURN) { RemoteSessions.returnToTerminal(player,target);return; }
        if (player.containerMenu.containerId!=container) return;
        if (action==MACHINES && player.containerMenu instanceof AEBaseMenu ae) {
            var host=TerminalSources.fromMenu(player,ae);
            if(host!=null&&host.access(player,false)) {
                if(host.hasCard())host.openMachines(player);
                else if(host.lockedPlayerSlot()>=0)com.syaru.meremote.platform.Platform.openCards(player,host);
                else Registration.tell(player,"insert_view_cell_card");
            } else Registration.tell(player,"offline");
            return;
        }
        if(player.containerMenu instanceof CardMenu cards) {
            if(cards.stillValid(player)&&action==MACHINES) {
                if(cards.host().hasCard())cards.host().openMachines(player);else cards.host().openStorage(player);
            }
            return;
        }
        if (!(player.containerMenu instanceof MachineMenu menu) || !menu.stillValid(player)) return;
        var host=menu.host();
        switch(action) {
            case OPEN -> RemoteSessions.request(player,menu,target);
            case REMOVE -> { if(host.book().remove(target))host.changed();menu.sendRows(player); }
            case REFRESH -> menu.sendRows(player);
            case STORAGE -> host.openStorage(player);
            case CARDS -> com.syaru.meremote.platform.Platform.openCards(player,host);
            case BIND -> Registration.bindTool(player,host);
            default -> { }
        }
    }
}
