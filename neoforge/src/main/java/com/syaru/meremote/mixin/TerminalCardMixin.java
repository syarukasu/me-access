package com.syaru.meremote.mixin;

import appeng.parts.reporting.AbstractTerminalPart;
import com.syaru.meremote.TerminalCardHolder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AbstractTerminalPart.class,remap=false)
public abstract class TerminalCardMixin implements TerminalCardHolder {
    @Unique private SimpleContainer remote$cards;
    @Override public Container remoteMachineCards() {
        return remoteMachineHasLegacyCard()?remote$legacyCards():((AbstractTerminalPart)(Object)this).getViewCellStorage().toContainer();
    }
    @Override public boolean remoteMachineHasLegacyCard() { return !remote$legacyCards().isEmpty(); }
    @Unique private SimpleContainer remote$legacyCards() {
        if(remote$cards==null) {
            remote$cards=new SimpleContainer(1);
            remote$cards.addListener(container->((AbstractTerminalPart)(Object)this).saveChanges());
        }
        return remote$cards;
    }
    @Inject(method="writeToNBT",at=@At("RETURN"))
    private void remote$save(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci) {
        var card=remote$legacyCards().getItem(0);
        if(card.isEmpty())tag.remove("RemoteMachineCard");else tag.put("RemoteMachineCard",card.save(registries));
    }
    @Inject(method="readFromNBT",at=@At("RETURN"))
    private void remote$load(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci) {
        var legacy=remote$legacyCards();
        legacy.setItem(0,ItemStack.parseOptional(registries,tag.getCompound("RemoteMachineCard")));
        var cells=((AbstractTerminalPart)(Object)this).getViewCellStorage();
        // Preserve old cards when all five cells are occupied; the old menu can retrieve them.
        if(!legacy.isEmpty())for(int i=0;i<cells.size();i++)if(cells.getStackInSlot(i).isEmpty()) {
            cells.setItemDirect(i,legacy.removeItemNoUpdate(0));break;
        }
    }
    @Inject(method="addAdditionalDrops",at=@At("RETURN"))
    private void remote$drops(List<ItemStack> drops,boolean wrenched,CallbackInfo ci) {
        var card=remote$legacyCards().getItem(0);if(!card.isEmpty())drops.add(card.copy());
    }
    @Inject(method="clearContent",at=@At("RETURN"))
    private void remote$clear(CallbackInfo ci) { remote$legacyCards().clearContent(); }
}
