package com.syaru.meremote;

import appeng.api.networking.GridHelper;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.syaru.meremote.platform.Platform;

public final class Machines {
    private static final MachineAdapter AE2 = new Ae2Adapter();
    public static MachineAdapter adapter(BlockEntity target) {
        if (target == null) return null;
        if (AE2.supports(target)) return AE2;
        return Platform.mekanismAdapter(target);
    }
    public static boolean terminalAccess(TerminalHost host, ServerPlayer player, boolean nearby) {
        return host != null && (nearby?host.access(player,true):host.registrationAccess(player));
    }
    public static BlockEntity target(TerminalHost host, ServerPlayer player, Endpoint endpoint) {
        if (!endpoint.dimension().equals(player.level().dimension().location().toString())) return null;
        var level = player.serverLevel();
        if (!level.hasChunkAt(endpoint.pos())) return null;
        var target = level.getBlockEntity(endpoint.pos());
        if (target == null || target.isRemoved()
                || !BuiltInRegistries.BLOCK.getKey(target.getBlockState().getBlock()).toString().equals(endpoint.blockId())) return null;
        var adapter = adapter(target);
        return adapter != null && adapter.canAccess(player, target) && connected(host, target) ? target : null;
    }
    public static boolean connected(TerminalHost host, BlockEntity target) {
        var level = target.getLevel(); var grid = host.grid();
        if (grid == null || level == null || !level.hasChunkAt(target.getBlockPos())) return false;
        for (var side : Direction.values()) {
            var own = GridHelper.getExposedNode(level, target.getBlockPos(), side);
            if (own != null && own.getGrid() == grid) return true;
            var pos = target.getBlockPos().relative(side);
            if (!level.hasChunkAt(pos)) continue;
            var adjacent = GridHelper.getExposedNode(level, pos, side.getOpposite());
            if (adjacent != null && adjacent.getGrid() == grid) return true;
        }
        return false;
    }
    public static List<MachineRow> rows(TerminalHost host, ServerPlayer player) {
        var orders=OrderDiagnostics.snapshot(host.grid());
        return host.book().entries().stream().map(e -> row(host,player,e,orders)).toList();
    }
    public static MachineRow row(TerminalHost host,ServerPlayer player,Endpoint e) {
        return row(host,player,e,OrderDiagnostics.snapshot(host.grid()));
    }
    public static MachineRow row(TerminalHost host,ServerPlayer player,Endpoint e,List<OrderDiagnostics.ActiveOrder> orders) {
        if(!e.dimension().equals(player.level().dimension().location().toString()))return offline(e,MachineStatus.DIMENSION);
        var level=player.serverLevel();
        if(!level.hasChunkAt(e.pos()))return offline(e,MachineStatus.UNLOADED);
        var tile=level.getBlockEntity(e.pos());
        if(tile==null||tile.isRemoved()||!BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()).toString().equals(e.blockId()))return offline(e,MachineStatus.REMOVED);
        var adapter=adapter(tile);
        if(adapter==null)return offline(e,MachineStatus.UNSUPPORTED);
        if(!adapter.canAccess(player,tile))return offline(e,MachineStatus.DENIED);
        if(!connected(host,tile))return offline(e,MachineStatus.DISCONNECTED);
        var provider=ProviderDiagnostics.read(host,tile,orders);
        int status=adapter.status(tile)|provider.flags();
        if((status&~MachineStatus.UNKNOWN.bit())!=0)status&=~MachineStatus.UNKNOWN.bit();
        return new MachineRow(e,true,status,provider.orders());
    }
    private static MachineRow offline(Endpoint e,MachineStatus reason) { return new MachineRow(e,false,reason.bit()); }
    public record MachineRow(Endpoint endpoint, boolean available,int status,List<OrderInfo> orders) {
        public MachineRow(Endpoint endpoint,boolean available,int status) { this(endpoint,available,status,List.of()); }
    }
    private Machines() { }
}
