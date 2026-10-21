package com.syaru.meremote.platform;

import appeng.client.gui.AEBaseScreen;
import com.syaru.meremote.Control;
import com.syaru.meremote.client.ClientMirror;
import com.syaru.meremote.client.TerminalIconButton;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=Platform.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->{
            MenuScreens.register(RemoteMod.MENU.get(),MachineScreen::new);
            MenuScreens.register(RemoteMod.CARD_MENU.get(),com.syaru.meremote.client.CardScreen::new);
        });
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::screen);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e)->ClientMirror.clear());
    }
    private static void screen(ScreenEvent.Init.Post event) {
        if(ClientMirror.active()&&event.getScreen() instanceof AbstractContainerScreen<?> screen&&!(screen instanceof AEBaseScreen)
                &&screen.getClass().getName().startsWith("mekanism.")) {
            var button=new TerminalIconButton(Platform.cardItem().getDefaultInstance(),Component.translatable("tab.me_remote_machine.return"),
                    b->Platform.control(new Control(screen.getMenu().containerId,Control.RETURN,ClientMirror.token())));
            button.setPosition(Math.max(0,screen.getGuiLeft()),Math.max(0,screen.getGuiTop()-23));event.addListener(button);
        }
    }
}
