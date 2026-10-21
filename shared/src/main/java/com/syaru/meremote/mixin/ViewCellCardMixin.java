package com.syaru.meremote.mixin;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.reporting.AbstractTerminalPart;
import com.syaru.meremote.TerminalCardHolder;
import com.syaru.meremote.platform.Platform;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=RestrictedInputSlot.class,remap=false)
public abstract class ViewCellCardMixin extends AppEngSlot {
    protected ViewCellCardMixin(InternalInventory inventory,int index) { super(inventory,index); }
    // Only extend the view-cell item check, after AE2's permission and slot checks.
    @Inject(method={"mayPlace(Lnet/minecraft/world/item/ItemStack;)Z", "m_5857_(Lnet/minecraft/world/item/ItemStack;)Z"},
            at=@At(value="FIELD",target="Lappeng/core/definitions/AEItems;VIEW_CELL:Lappeng/core/definitions/ItemDefinition;"),
            cancellable=true,require=1,allow=1)
    private void remote$acceptCard(ItemStack stack,CallbackInfoReturnable<Boolean> ci) {
        if(!stack.is(Platform.cardItem()))return;
        if(!(getMenu().getTarget() instanceof AbstractTerminalPart part)
                ||getInventory()!=part.getViewCellStorage())return;
        if(part instanceof TerminalCardHolder holder&&holder.remoteMachineHasLegacyCard()) {
            ci.setReturnValue(false);return;
        }
        var cells=part.getViewCellStorage();
        for(int i=0;i<cells.size();i++)if(i!=getContainerSlot()&&cells.getStackInSlot(i).is(Platform.cardItem())) {
            ci.setReturnValue(false);return;
        }
        ci.setReturnValue(true);
    }
}
