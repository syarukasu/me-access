package com.syaru.meremote;

import appeng.api.networking.IGrid;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.parts.reporting.AbstractTerminalPart;
import com.syaru.meremote.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CardTerminalHost implements TerminalHost {
    private final AbstractTerminalPart part;
    private final WirelessTerminalMenuHost wireless;
    private final ServerPlayer owner;
    private final int inventorySlot;
    private final ItemStack terminalStack;
    private final Container cards;
    private ItemStack installed=ItemStack.EMPTY;
    private MachineBook book;
    private Object cardData;
    private int lastTick=Integer.MIN_VALUE;
    private boolean tickValid;

    public CardTerminalHost(AbstractTerminalPart part) {
        this.part=part;wireless=null;owner=null;inventorySlot=-1;terminalStack=ItemStack.EMPTY;
        cards=((TerminalCardHolder)part).remoteMachineCards();
    }
    public CardTerminalHost(ServerPlayer player,WirelessTerminalMenuHost host,int slot) {
        part=null;wireless=host;owner=player;inventorySlot=slot;terminalStack=host.getItemStack();
        cards=host.getUpgrades().toContainer();
    }
    private ItemStack currentCard() {
        for(int i=0;i<cards.getContainerSize();i++)if(cards.getItem(i).is(Platform.cardItem()))return cards.getItem(i);
        return ItemStack.EMPTY;
    }
    @Override public boolean hasCard() { return !currentCard().isEmpty(); }
    @Override public Container cards() { return cards; }
    @Override public int lockedPlayerSlot() { return inventorySlot; }
    @Override public MachineBook book() {
        var card=currentCard();
        var data=Platform.cardDataIdentity(card);
        if(card!=installed||book==null||cardData!=data) {
            installed=card;cardData=data;book=new MachineBook();
            if(!card.isEmpty()) {
                var tag=Platform.readCard(card);book.load(tag);
                if(!remoteLevel().isClientSide&&!tag.hasUUID("id"))changed();
            }
        }
        return book;
    }
    @Override public void changed() {
        if(!installed.isEmpty()&&installed==currentCard()&&book!=null) {
            Platform.writeCard(installed,book.save());cardData=Platform.cardDataIdentity(installed);
        }
        cards.setChanged();
        if(part!=null)part.saveChanges();else {
            for(int i=0;i<cards.getContainerSize();i++)if(cards.getItem(i)==installed)wireless.getUpgrades().sendChangeNotification(i);
            owner.getInventory().setChanged();
        }
    }
    @Override public Level remoteLevel() { return part==null?owner.level():part.getLevel(); }
    @Override public IGrid grid() {
        if(part!=null)return part.getMainNode().getGrid();
        var node=wireless.getActionableNode();return node==null?null:node.getGrid();
    }
    @Override public boolean access(ServerPlayer player,boolean requireCard) {
        return validate(player,requireCard,true);
    }
    @Override public boolean registrationAccess(ServerPlayer player) { return validate(player,true,false); }
    private boolean validate(ServerPlayer player,boolean requireCard,boolean nearby) {
        if(!player.isAlive()||player.isSpectator()||!player.mayBuild()||(requireCard&&!hasCard()))return false;
        if(part!=null) {
            var block=part.getBlockEntity();var pos=block.getBlockPos();var level=part.getLevel();
            return !block.isRemoved()&&level==player.level()&&level.hasChunkAt(pos)&&part.getHost().getPart(part.getSide())==part
                    &&part.getMainNode().isActive()&&level.mayInteract(player,pos)&&(!nearby||player.distanceToSqr(pos.getCenter())<=64);
        }
        return player==owner&&inventorySlot>=0&&player.getInventory().getItem(inventorySlot)==terminalStack
                &&Platform.wirelessAccess(player,wireless)&&grid()!=null;
    }
    @Override public boolean tick(ServerPlayer player,AbstractContainerMenu menu) {
        if(part!=null)return access(player,false);
        if(lastTick!=player.tickCount) {
            lastTick=player.tickCount;
            tickValid=player.getInventory().getItem(inventorySlot)==terminalStack&&Platform.tickWireless(wireless,menu);
        }
        return tickValid&&access(player,false);
    }
    @Override public CompoundTag reference() {
        var tag=new CompoundTag();tag.putString("dimension",remoteLevel().dimension().location().toString());
        tag.putUUID("card",book().id());
        if(part!=null) { tag.putLong("pos",part.getBlockEntity().getBlockPos().asLong());tag.putInt("side",part.getSide().ordinal()); }
        else tag.putInt("slot",inventorySlot);
        return tag;
    }
    @Override public void openStorage(ServerPlayer player) {
        if(!access(player,false))return;
        if(part!=null)Platform.openStorage(player,part);else Platform.openWireless(player,inventorySlot);
    }
}
