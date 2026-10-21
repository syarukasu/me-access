package com.syaru.meremote.platform;

import com.syaru.meremote.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Platform.ID)
public final class RemoteMod {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,Platform.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,Platform.ID);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Platform.ID);
    public static final RegistryObject<Item> CARD=ITEMS.register("machine_access_card",()->appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TOOL=ITEMS.register("machine_registration_tool",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<MenuType<MachineMenu>> MENU=MENUS.register("machines",()->IForgeMenuType.create(MachineMenu::fromNetwork));
    public static final RegistryObject<MenuType<CardMenu>> CARD_MENU=MENUS.register("card",()->IForgeMenuType.create(CardMenu::fromNetwork));
    private static final RegistryObject<CreativeModeTab> TAB=TABS.register("devices",()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.me_remote_machine")).icon(()->CARD.get().getDefaultInstance())
            .displayItems((parameters,out)->{out.accept(CARD.get());out.accept(TOOL.get());}).build());
    public RemoteMod() {
        var bus=FMLJavaModLoadingContext.get().getModEventBus();ITEMS.register(bus);MENUS.register(bus);TABS.register(bus);
        RemoteNetwork.register();RemoteMenuLocator.register();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(RemoteMod::registerCards));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::registerMachine);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent event)->{
            if(event.phase==TickEvent.Phase.END&&event.player instanceof net.minecraft.server.level.ServerPlayer player)RemoteSessions.tick(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)RemoteSessions.clear(player);});
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event)->{if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)RemoteSessions.unload(level);});
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event)->RemoteSessions.stop());
    }
    private static void registerCards() {
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)if(item instanceof appeng.items.tools.powered.WirelessTerminalItem)
            appeng.api.upgrades.Upgrades.add(CARD.get(),item,1);
        appeng.api.upgrades.Upgrades.add(CARD.get(),appeng.core.definitions.AEParts.TERMINAL,1);
        appeng.api.upgrades.Upgrades.add(CARD.get(),appeng.core.definitions.AEParts.CRAFTING_TERMINAL,1);
        appeng.api.upgrades.Upgrades.add(CARD.get(),appeng.core.definitions.AEParts.PATTERN_ENCODING_TERMINAL,1);
    }
    private void registerMachine(PlayerInteractEvent.RightClickBlock event) {
        if(event.getItemStack().is(TOOL.get())) {
            if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)Registration.use(player,event.getHand(),event.getPos(),event.getFace());
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
        }
    }
}
