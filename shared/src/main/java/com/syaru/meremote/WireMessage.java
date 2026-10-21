package com.syaru.meremote;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record WireMessage(int kind,int container,UUID token,String dimension,BlockPos pos,int state,CompoundTag data) {
    public static final int PREPARE=0,CLEAR=1,ROWS=2,UPDATE=3,ROW_UPDATES=4;
    public static WireMessage rows(int menu,CompoundTag data) { return new WireMessage(ROWS,menu,Control.NONE,"",BlockPos.ZERO,0,data); }
    public static WireMessage rowUpdates(int menu,CompoundTag data) { return new WireMessage(ROW_UPDATES,menu,Control.NONE,"",BlockPos.ZERO,0,data); }
    public static WireMessage clear(UUID token) { return new WireMessage(CLEAR,0,token,"",BlockPos.ZERO,0,new CompoundTag()); }
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(kind);buf.writeVarInt(container);buf.writeUUID(token);buf.writeUtf(dimension,128);
        buf.writeBlockPos(pos);buf.writeVarInt(state);buf.writeNbt(data);
    }
    public static WireMessage read(FriendlyByteBuf buf) {
        return new WireMessage(buf.readVarInt(),buf.readVarInt(),buf.readUUID(),buf.readUtf(128),buf.readBlockPos(),buf.readVarInt(),buf.readNbt());
    }
}
