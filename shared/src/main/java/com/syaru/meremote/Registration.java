package com.syaru.meremote;

import appeng.api.parts.PartHelper;
import com.syaru.meremote.platform.Platform;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;

public final class Registration {
    public static void use(ServerPlayer player, InteractionHand hand, BlockPos pos, Direction side) {
        var level=player.serverLevel(); var stack=player.getItemInHand(hand);
        if (side==null || !level.hasChunkAt(pos) || !player.mayBuild() || player.isSpectator()
                || player.distanceToSqr(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)>64 || !level.mayInteract(player,pos)) return;
        var part=PartHelper.getPart(level,pos,side);
        if (part instanceof appeng.parts.reporting.AbstractTerminalPart base&&part instanceof TerminalCardHolder) {
            var terminal=new CardTerminalHost(base);
            if (!Machines.terminalAccess(terminal,player,true)) { tell(player,"offline");return; }
            Platform.writeTool(stack,terminal.reference());tell(player,"bound");return;
        }
        var tag=Platform.readTool(stack);
        if(!tag.hasUUID("card")) { tell(player,"bind_first");return; }
        var terminal=TerminalSources.resolve(player,tag);
        if(terminal==null||!Machines.terminalAccess(terminal,player,false)) { tell(player,"offline");return; }
        var target=level.getBlockEntity(pos);var adapter=Machines.adapter(target);
        if (adapter==null) { tell(player,"unsupported");return; }
        if (!adapter.canAccess(player,target) || !Machines.connected(terminal,target)) { tell(player,"unreachable");return; }
        var existing=terminal.book().entries().stream().filter(e->e.pos().equals(pos)&&e.dimension().equals(tag.getString("dimension"))).findFirst();
        if (player.isShiftKeyDown() && existing.isPresent()) {
            terminal.book().remove(existing.get().id());terminal.changed();tell(player,"removed");return;
        }
        if (existing.isPresent()) { tell(player,"already_registered");return; }
        if (terminal.book().entries().size()>=MachineBook.CAPACITY) { tell(player,"full");return; }
        String label=target instanceof Nameable named&&named.hasCustomName() ? named.getCustomName().getString() : "";
        terminal.book().add(new Endpoint(UUID.randomUUID(),level.dimension().location().toString(),pos,
                BuiltInRegistries.BLOCK.getKey(target.getBlockState().getBlock()).toString(),label.substring(0,Math.min(label.length(),128))));
        terminal.changed();tell(player,"registered");
    }
    public static void bindTool(ServerPlayer player,TerminalHost host) {
        if(!host.access(player,true))return;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var stack=player.getInventory().getItem(i);
            if(stack.is(Platform.toolItem())) { Platform.writeTool(stack,host.reference());player.getInventory().setChanged();tell(player,"bound");return; }
        }
        tell(player,"need_tool");
    }
    public static void tell(ServerPlayer player,String code) { player.displayClientMessage(Component.translatable("message.me_remote_machine."+code),true); }
    private Registration() { }
}
