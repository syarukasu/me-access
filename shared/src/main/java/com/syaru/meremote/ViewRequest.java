package com.syaru.meremote;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record ViewRequest(int container,List<UUID> ids) {
    public ViewRequest { if(ids.size()>9)throw new IllegalArgumentException("Too many visible machines");ids=List.copyOf(ids); }
    public void write(FriendlyByteBuf buf) { buf.writeVarInt(container);buf.writeVarInt(ids.size());for(var id:ids)buf.writeUUID(id); }
    public static ViewRequest read(FriendlyByteBuf buf) {
        int container=buf.readVarInt(),count=buf.readVarInt();
        if(count<0||count>9)throw new IllegalArgumentException("Invalid visible machine count");
        var ids=new java.util.ArrayList<UUID>(count);for(int i=0;i<count;i++)ids.add(buf.readUUID());
        return new ViewRequest(container,ids);
    }
    public void handle(ServerPlayer player) {
        if(player.containerMenu instanceof MachineMenu menu&&menu.containerId==container&&menu.stillValid(player))menu.visible(ids);
    }
}
