package com.syaru.meremote.client;

import com.syaru.meremote.*;
import com.syaru.meremote.platform.Platform;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A single non-ticking GUI mirror, never inserted into a client chunk or a server world. */
public final class ClientMirror {
    private static UUID token;
    private static BlockEntity mirror;
    private static Level level;
    public static boolean active() { return mirror!=null; }
    public static UUID token() { return token==null?Control.NONE:token; }
    public static BlockEntity lookup(BlockGetter world,BlockPos pos) {
        return mirror!=null&&world==level&&mirror.getBlockPos().equals(pos)?mirror:null;
    }
    public static void handle(WireMessage message) {
        var mc=Minecraft.getInstance();
        if(message.kind()==WireMessage.CLEAR) { if(message.token().equals(token))clear();return; }
        if(message.kind()==WireMessage.ROWS||message.kind()==WireMessage.ROW_UPDATES) {
            if(mc.player!=null&&mc.player.containerMenu instanceof MachineMenu menu&&menu.containerId==message.container()) {
                if(message.kind()==WireMessage.ROWS)menu.receive(message.data());else menu.receiveUpdates(message.data());
            }
            return;
        }
        if(mc.level==null||!mc.level.dimension().location().toString().equals(message.dimension())) {
            if(message.kind()==WireMessage.PREPARE)Platform.control(new Control(message.container(),Control.REJECT,message.token()));return;
        }
        if(message.kind()==WireMessage.PREPARE) {
            clear();
            try {
                level=mc.level;
                mirror=Platform.createMirror(level,message.pos(),Block.stateById(message.state()),message.data());
                if(mirror==null)throw new IllegalStateException("Unsupported client block entity snapshot");
                token=message.token();
                Platform.control(new Control(message.container(),Control.READY,token));
            } catch(RuntimeException failure) {
                org.slf4j.LoggerFactory.getLogger("ME Remote Machine").warn("Cannot prepare remote GUI mirror",failure);
                clear();Platform.control(new Control(message.container(),Control.REJECT,message.token()));
            }
        } else if(message.kind()==WireMessage.UPDATE&&message.token().equals(token)&&mirror!=null) {
            Platform.updateMirror(mirror,message.data());
        }
    }
    public static void clear() {
        if(mirror!=null)mirror.setRemoved();
        mirror=null;token=null;level=null;
    }
    private ClientMirror() { }
}
