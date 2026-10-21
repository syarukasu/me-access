package com.syaru.meremote.platform;

import com.syaru.meremote.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(Platform.ID)
public final class RemoteMod {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,Platform.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,Platform.ID);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Platform.ID);
    public static final DeferredHolder<Item,Item> CARD=ITEMS.register("machine_access_card",()->appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item,Item> TOOL=ITEMS.register("machine_registration_tool",()->new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<MenuType<?>,MenuType<MachineMenu>> MENU=MENUS.register("machines",()->IMenuTypeExtension.create(MachineMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>,MenuType<CardMenu>> CARD_MENU=MENUS.register("card",()->IMenuTypeExtension.create(CardMenu::fromNetwork));
    private static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("devices",()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.me_remote_machine")).icon(()->CARD.get().getDefaultInstance())
            .displayItems((parameters,out)->{out.accept(CARD.get());out.accept(TOOL.get());}).build());
    public RemoteMod(IEventBus bus) {
        ITEMS.register(bus);MENUS.register(bus);TABS.register(bus);bus.addListener(RemoteNetwork::register);RemoteMenuLocator.register();
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(RemoteMod::registerCards));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::registerMachine);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event)->{if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)RemoteSessions.tick(player);});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)RemoteSessions.clear(player);});
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event)->{if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)RemoteSessions.unload(level);});
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event)->RemoteSessions.stop());
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
