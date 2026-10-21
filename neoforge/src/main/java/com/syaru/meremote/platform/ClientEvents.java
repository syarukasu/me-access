package com.syaru.meremote.platform;

import appeng.client.gui.AEBaseScreen;
import com.syaru.meremote.Control;
import com.syaru.meremote.client.ClientMirror;
import com.syaru.meremote.client.TerminalIconButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid=Platform.ID,value=Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void setup(RegisterMenuScreensEvent event) {
        event.register(RemoteMod.MENU.get(),MachineScreen::new);
        event.register(RemoteMod.CARD_MENU.get(),com.syaru.meremote.client.CardScreen::new);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientMirror.clear(); }
    @SubscribeEvent public static void screen(ScreenEvent.Init.Post event) {
        if(ClientMirror.active()&&event.getScreen() instanceof AbstractContainerScreen<?> screen&&!(screen instanceof AEBaseScreen)
                &&screen.getClass().getName().startsWith("mekanism.")) {
            var button=new TerminalIconButton(Platform.cardItem().getDefaultInstance(),Component.translatable("tab.me_remote_machine.return"),
                    b->Platform.control(new Control(screen.getMenu().containerId,Control.RETURN,ClientMirror.token())));
            button.setPosition(Math.max(0,screen.getGuiLeft()),Math.max(0,screen.getGuiTop()-23));event.addListener(button);
        }
    }
}
