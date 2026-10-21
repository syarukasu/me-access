package com.syaru.meremote;

import com.syaru.meremote.platform.Platform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CardMenu extends AbstractContainerMenu {
    private final TerminalHost host;
    private final int cardSlots;
    private final int lockedSlot;
    public CardMenu(int id,Inventory inventory,TerminalHost host) {
        this(id,inventory,host,host.cards(),host.lockedPlayerSlot());
    }
    private CardMenu(int id,Inventory inventory,TerminalHost host,Container cards,int lockedSlot) {
        super(Platform.cardMenu(),id);this.host=host;cardSlots=cards.getContainerSize();this.lockedSlot=lockedSlot;
        for(int i=0;i<cardSlots;i++)addSlot(new Slot(cards,i,(176-cardSlots*18)/2+1+i*18,35) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) {
                if(!stack.is(Platform.cardItem()))return false;
                for(int j=0;j<cards.getContainerSize();j++)if(j!=getContainerSlot()&&cards.getItem(j).is(Platform.cardItem()))return false;
                return cards.canPlaceItem(getContainerSlot(),stack);
            }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addPlayerSlot(inventory,col+row*9+9,8+col*18,76+row*18);
        for(int col=0;col<9;col++)addPlayerSlot(inventory,col,8+col*18,134);
    }
    private void addPlayerSlot(Inventory inventory,int index,int x,int y) {
        addSlot(new Slot(inventory,index,x,y) {
            @Override public boolean mayPickup(Player player) { return index!=lockedSlot; }
            @Override public boolean mayPlace(ItemStack stack) { return index!=lockedSlot; }
        });
    }
    public static CardMenu fromNetwork(int id,Inventory inventory,FriendlyByteBuf buf) {
        int count=buf.readVarInt();if(count<1||count>8)throw new IllegalArgumentException("Invalid terminal card inventory size");
        int locked=buf.readVarInt();if(locked< -1||locked>=inventory.getContainerSize())throw new IllegalArgumentException("Invalid protected inventory slot");
        return new CardMenu(id,inventory,null,new SimpleContainer(count),locked);
    }
    public TerminalHost host() { return host; }
    public int cardSlots() { return cardSlots; }
    @Override public boolean stillValid(Player player) { return player.level().isClientSide||player instanceof ServerPlayer server&&host.access(server,false); }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if(host!=null&&getOwner() instanceof ServerPlayer player&&!host.tick(player,this))player.closeContainer();
    }
    private Player getOwner() { return slots.get(cardSlots).container instanceof Inventory inventory?inventory.player:null; }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(type==ClickType.SWAP&&button==lockedSlot)return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<cardSlots) { if(!moveItemStackTo(stack,cardSlots,slots.size(),true))return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,0,cardSlots,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);
        return copy;
    }
}
