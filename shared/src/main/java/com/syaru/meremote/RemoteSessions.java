package com.syaru.meremote;

import com.syaru.meremote.platform.Platform;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server-thread only: one authorized native machine menu (or pending handshake) per player. */
public final class RemoteSessions {
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();
    private static final class Session {
        final UUID token=UUID.randomUUID();final TerminalHost source;final Endpoint endpoint;final BlockEntity machine;
        final MachineAdapter adapter;final MachineMenu origin;final int deadline;
        boolean opened;CompoundTag lastSnapshot;
        Session(ServerPlayer player,MachineMenu menu,Endpoint endpoint,BlockEntity machine,MachineAdapter adapter) {
            source=menu.host();origin=menu;this.endpoint=endpoint;this.machine=machine;this.adapter=adapter;deadline=player.tickCount+200;
        }
        boolean valid(ServerPlayer player) {
            return Machines.terminalAccess(source,player,true)&&source.book().find(endpoint.id())==endpoint
                    && Machines.target(source,player,endpoint)==machine;
        }
    }
    public static void request(ServerPlayer player,MachineMenu menu,UUID id) {
        var endpoint=menu.host().book().find(id);
        if(endpoint==null || !Machines.terminalAccess(menu.host(),player,true))return;
        var machine=Machines.target(menu.host(),player,endpoint);
        if(machine==null) { Registration.tell(player,"unreachable");return; }
        clear(player);
        var session=new Session(player,menu,endpoint,machine,Machines.adapter(machine));
        ACTIVE.put(player.getUUID(),session);
        var snapshot=Platform.snapshot(machine);session.lastSnapshot=snapshot.copy();
        Platform.send(player,new WireMessage(WireMessage.PREPARE,menu.containerId,session.token,endpoint.dimension(),endpoint.pos(),
                Block.getId(machine.getBlockState()),snapshot));
    }
    public static void ready(ServerPlayer player,UUID token,boolean success) {
        var s=ACTIVE.get(player.getUUID());
        if(s==null||s.opened||!s.token.equals(token))return;
        if(!success||player.containerMenu!=s.origin||player.tickCount>s.deadline||!s.valid(player)) {
            clear(player);Registration.tell(player,"unreachable");return;
        }
        try {
            if(!s.adapter.open(player,s.machine)||s.adapter.menuTarget(player.containerMenu)!=s.machine) {
                clear(player);player.closeContainer();Registration.tell(player,"unsupported");return;
            }
            s.opened=true;
        } catch(RuntimeException failure) {
            org.slf4j.LoggerFactory.getLogger("ME Remote Machine").error("Native machine menu failed at {}",s.endpoint.pos(),failure);
            clear(player);player.closeContainer();Registration.tell(player,"unsupported");
        }
    }
    public static boolean authorizeCurrent(ServerPlayer player) {
        var s=ACTIVE.get(player.getUUID());
        return s==null||!ownsMenu(player,s)||s.valid(player);
    }
    private static boolean ownsMenu(ServerPlayer player,Session s) {
        return s.opened?s.adapter.menuTarget(player.containerMenu)==s.machine:player.containerMenu==s.origin;
    }
    public static UUID token(ServerPlayer player) { var s=ACTIVE.get(player.getUUID());return s==null?Control.NONE:s.token; }
    public static BlockEntity locate(ServerPlayer player,UUID token,net.minecraft.core.BlockPos pos) {
        var s=ACTIVE.get(player.getUUID());return s!=null&&s.token.equals(token)&&s.endpoint.pos().equals(pos)&&s.valid(player)?s.machine:null;
    }
    public static void tick(ServerPlayer player) {
        var s=ACTIVE.get(player.getUUID());if(s==null)return;
        if(!ownsMenu(player,s)) { clear(player);return; }
        if(!s.source.tick(player,player.containerMenu)||!authorizeCurrent(player)||(!s.opened&&player.tickCount>s.deadline)) {
            clear(player);if(player.containerMenu!=player.inventoryMenu)player.closeContainer();return;
        }
        if(s.opened&&player.tickCount%20==0) {
            var next=Platform.snapshot(s.machine);
            if(!next.equals(s.lastSnapshot)) {
                s.lastSnapshot=next.copy();
                Platform.send(player,new WireMessage(WireMessage.UPDATE,player.containerMenu.containerId,s.token,s.endpoint.dimension(),s.endpoint.pos(),
                        Block.getId(s.machine.getBlockState()),next));
            }
        }
    }
    public static void returnToTerminal(ServerPlayer player,UUID token) {
        var s=ACTIVE.get(player.getUUID());
        if(s==null||!s.token.equals(token))return;
        boolean allowed=Machines.terminalAccess(s.source,player,true);clear(player);
        if(allowed)s.source.openMachines(player);else player.closeContainer();
    }
    public static void clear(ServerPlayer player) {
        var old=ACTIVE.remove(player.getUUID());if(old!=null)Platform.send(player,WireMessage.clear(old.token));
    }
    public static void unload(Level level) {
        for(var id:java.util.List.copyOf(ACTIVE.keySet())) {
            var s=ACTIVE.get(id);
            if(s.source.remoteLevel()!=level&&s.machine.getLevel()!=level)continue;
            var player=level.getServer().getPlayerList().getPlayer(id);
            if(player==null) { ACTIVE.remove(id);continue; }
            boolean owned=ownsMenu(player,s);clear(player);
            if(owned)player.closeContainer();
        }
    }
    public static void stop() { ACTIVE.clear(); }
    private RemoteSessions() { }
}
