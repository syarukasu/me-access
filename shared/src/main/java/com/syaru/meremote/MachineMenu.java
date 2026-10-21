package com.syaru.meremote;

import com.syaru.meremote.platform.Platform;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class MachineMenu extends AbstractContainerMenu {
    private final TerminalHost host;
    private List<Machines.MachineRow> rows=List.of();
    private List<java.util.UUID> visible=List.of();
    private int lastSnapshot=Integer.MIN_VALUE;
    private final Player owner;
    public MachineMenu(int id,Inventory inventory,TerminalHost host) {
        super(Platform.menu(),id);this.host=host;owner=inventory.player;
    }
    public static MachineMenu fromNetwork(int id,Inventory inventory,FriendlyByteBuf data) {
        var menu=new MachineMenu(id,inventory,null);
        menu.receive(data.readNbt());return menu;
    }
    public TerminalHost host() { return host; }
    public List<Machines.MachineRow> rows() { return rows; }
    public CompoundTag snapshot(ServerPlayer player) {
        var entries=host.book().entries();
        if(rows.isEmpty()&&visible.isEmpty())visible=entries.stream().limit(9).map(Endpoint::id).toList();
        var previous=new java.util.HashMap<java.util.UUID,Machines.MachineRow>();for(var row:rows)previous.put(row.endpoint().id(),row);
        var orders=OrderDiagnostics.snapshot(host.grid());
        rows=entries.stream().map(e->visible.contains(e.id())?Machines.row(host,player,e,orders):
                previous.getOrDefault(e.id(),new Machines.MachineRow(e,false,MachineStatus.UNCHECKED.bit()))).toList();
        lastSnapshot=player.tickCount;
        var result=new CompoundTag();var list=new ListTag();
        for(var row:rows)list.add(encode(row,player.level()));
        result.put("rows",list);return result;
    }
    public void receive(CompoundTag tag) {
        if(tag==null)return;
        var list=tag.getList("rows",net.minecraft.nbt.Tag.TAG_COMPOUND);
        var next=new java.util.ArrayList<Machines.MachineRow>();
        for(int i=0;i<Math.min(list.size(),MachineBook.CAPACITY);i++) {
            var entry=list.getCompound(i);var endpoint=Endpoint.read(entry);
            var orders=new java.util.ArrayList<OrderInfo>();
            var listOrders=entry.getList("orders",net.minecraft.nbt.Tag.TAG_COMPOUND);
            for(int j=0;j<listOrders.size();j++)orders.add(OrderInfo.read(listOrders.getCompound(j),owner.level()));
            if(endpoint!=null)next.add(new Machines.MachineRow(endpoint,entry.getBoolean("available"),entry.getInt("status"),List.copyOf(orders)));
        }
        rows=List.copyOf(next);
    }
    public void sendRows(ServerPlayer player) { Platform.send(player,WireMessage.rows(containerId,snapshot(player))); }
    private static CompoundTag encode(Machines.MachineRow row,net.minecraft.world.level.Level level) {
        var tag=row.endpoint().save();tag.putBoolean("available",row.available());tag.putInt("status",row.status());
        var orders=new ListTag();for(var order:row.orders())orders.add(order.save(level));tag.put("orders",orders);return tag;
    }
    public void visible(List<java.util.UUID> ids) {
        var book=host.book();visible=ids.stream().distinct().filter(id->book.find(id)!=null).limit(9).toList();
    }
    public void receiveUpdates(CompoundTag tag) {
        var previous=rows;receive(tag);var updates=new java.util.HashMap<java.util.UUID,Machines.MachineRow>();
        for(var row:rows)updates.put(row.endpoint().id(),row);
        rows=previous.stream().map(row->updates.getOrDefault(row.endpoint().id(),row)).toList();
    }
    private void updateVisible(ServerPlayer player) {
        var book=host.book();var next=new java.util.ArrayList<>(rows);var changes=new ListTag();
        var orders=OrderDiagnostics.snapshot(host.grid());
        for(var id:visible) {
            var endpoint=book.find(id);
            if(endpoint==null) { sendRows(player);return; }
            for(int i=0;i<next.size();i++)if(next.get(i).endpoint().id().equals(id)) {
                var updated=Machines.row(host,player,endpoint,orders);
                if(!updated.equals(next.get(i))) { next.set(i,updated);changes.add(encode(updated,player.level())); }
                break;
            }
        }
        rows=List.copyOf(next);lastSnapshot=player.tickCount;
        if(!changes.isEmpty()) {
            var tag=new CompoundTag();tag.put("rows",changes);Platform.send(player,WireMessage.rowUpdates(containerId,tag));
        }
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if(owner instanceof ServerPlayer player) {
            if(!host.tick(player,this)) { player.closeContainer();return; }
            if(lastSnapshot==Integer.MIN_VALUE)sendRows(player);
            else if(player.tickCount-lastSnapshot>=20)updateVisible(player);
        }
    }
    @Override public boolean stillValid(Player player) {
        return player.level().isClientSide || player instanceof ServerPlayer server&&Machines.terminalAccess(host,server,true);
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
}
