package com.syaru.meremote.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.AEBaseMenu;
import com.syaru.meremote.*;
import com.syaru.meremote.client.ClientMirror;
import com.syaru.meremote.client.TerminalIconButton;
import com.syaru.meremote.platform.Platform;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AEBaseScreen.class,remap=false)
public abstract class MachineTabMixin {
    @Shadow protected abstract <B extends Button> B addToLeftToolbar(B button);
    @Inject(method="<init>",at=@At("RETURN"))
    private void remote$tab(AEBaseMenu menu,Inventory inventory,Component title,ScreenStyle style,CallbackInfo ci) {
        if(TerminalSources.supports(menu.getTarget())) {
            addToLeftToolbar(new TerminalIconButton(Platform.cardItem().getDefaultInstance(),Component.translatable("item.me_remote_machine.machine_access_card"),
                    button->Platform.control(new Control(menu.containerId,Control.MACHINES,Control.NONE))));
        } else if(ClientMirror.active()) {
            addToLeftToolbar(new TerminalIconButton(Platform.cardItem().getDefaultInstance(),Component.translatable("tab.me_remote_machine.return"),
                    button->Platform.control(new Control(menu.containerId,Control.RETURN,ClientMirror.token()))));
        }
    }
}
