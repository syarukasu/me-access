package com.syaru.meremote;

import appeng.api.parts.PartHelper;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.parts.reporting.AbstractTerminalPart;
import com.syaru.meremote.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class TerminalSources {
    public static boolean supports(Object target) {
        return target instanceof AbstractTerminalPart&&target instanceof TerminalCardHolder||target instanceof WirelessTerminalMenuHost;
    }
    public static TerminalHost fromMenu(ServerPlayer player,AEBaseMenu menu) {
        if(menu.getTarget() instanceof AbstractTerminalPart part&&part instanceof TerminalCardHolder)return new CardTerminalHost(part);
        if(menu.getTarget() instanceof WirelessTerminalMenuHost wireless) {
            for(int i=0;i<player.getInventory().getContainerSize();i++) {
                if(player.getInventory().getItem(i)==wireless.getItemStack())return new CardTerminalHost(player,wireless,i);
            }
        }
        return null;
    }
    public static TerminalHost resolve(ServerPlayer player,CompoundTag tag) {
        if(!tag.getString("dimension").equals(player.level().dimension().location().toString()))return null;
        TerminalHost host=null;
        if(tag.contains("slot")) {
            int slot=tag.getInt("slot");
            if(slot<0||slot>=player.getInventory().getContainerSize())return null;
            var wireless=Platform.createWirelessHost(player,slot);
            if(wireless!=null)host=new CardTerminalHost(player,wireless,slot);
        } else {
            var pos=BlockPos.of(tag.getLong("pos"));int side=tag.getInt("side");
            if(side<0||side>5||!player.level().hasChunkAt(pos))return null;
            var part=PartHelper.getPart(player.level(),pos,Direction.from3DDataValue(side));
            if(part instanceof AbstractTerminalPart terminal&&part instanceof TerminalCardHolder)host=new CardTerminalHost(terminal);
        }
        return host!=null&&host.hasCard()&&tag.hasUUID("card")&&host.book().id().equals(tag.getUUID("card"))?host:null;
    }
    private TerminalSources() { }
}
