package com.syaru.meremote.platform;

import com.syaru.meremote.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

public final class Platform {
    public static final String ID="me_remote_machine";
    public static MenuType<MachineMenu> menu() { return RemoteMod.MENU.get(); }
    public static Item cardItem() { return RemoteMod.CARD.get(); }
    public static String writeText(net.minecraft.network.chat.Component text,Level level) { return net.minecraft.network.chat.Component.Serializer.toJson(text,level.registryAccess()); }
    public static net.minecraft.network.chat.Component readText(String text,Level level) {
        var value=net.minecraft.network.chat.Component.Serializer.fromJson(text,level.registryAccess());return value==null?net.minecraft.network.chat.Component.empty():value;
    }
    public static MenuType<com.syaru.meremote.CardMenu> cardMenu() { return RemoteMod.CARD_MENU.get(); }
    public static CompoundTag readCard(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("MachineAccess"); }
    public static Object cardDataIdentity(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY); }
    public static void writeCard(ItemStack stack,CompoundTag tag) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.put("MachineAccess",tag);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
    }
    public static void openStorage(ServerPlayer player,appeng.parts.reporting.AbstractTerminalPart part) {
        appeng.menu.MenuOpener.open(part.getMenuType(player),player,appeng.menu.locator.MenuLocators.forPart(part));
    }
    public static void openWireless(ServerPlayer player,int slot) {
        var stack=player.getInventory().getItem(slot);
        if(stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem item)item.openFromInventory(player,appeng.menu.locator.MenuLocators.forInventorySlot(slot));
    }
    public static appeng.helpers.WirelessTerminalMenuHost createWirelessHost(ServerPlayer player,int slot) {
        var stack=player.getInventory().getItem(slot);
        return stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem item
                ?item.getMenuHost(player,appeng.menu.locator.MenuLocators.forInventorySlot(slot),null):null;
    }
    public static boolean wirelessAccess(ServerPlayer player,appeng.helpers.WirelessTerminalMenuHost host) {
        return host.isValid()&&host.getLinkStatus().connected()&&host.consumeIdlePower(appeng.api.config.Actionable.SIMULATE);
    }
    public static boolean tickWireless(appeng.helpers.WirelessTerminalMenuHost host,net.minecraft.world.inventory.AbstractContainerMenu menu) {
        host.tick();return host.isValid()&&host.getLinkStatus().connected();
    }
    public static Item toolItem() { return RemoteMod.TOOL.get(); }
    public static ResourceLocation id(String id) { return ResourceLocation.parse(id); }
    public static CompoundTag readTool(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("RemoteTerminal"); }
    public static void writeTool(ItemStack stack,CompoundTag tag) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.put("RemoteTerminal",tag);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
    }
    public static MachineAdapter mekanismAdapter(BlockEntity target) {
        return ModList.get().isLoaded("mekanism")&&MekanismAdapter.INSTANCE.supports(target)?MekanismAdapter.INSTANCE:null;
    }
    public static int mekanismInputStatus(BlockEntity target,net.minecraft.core.Direction side) {
        return ModList.get().isLoaded("mekanism")&&target instanceof mekanism.common.tile.base.TileEntityMekanism tile
                ?com.syaru.meremote.MekanismDiagnostics.inputSide(tile,side):0;
    }
    public static void openMachines(ServerPlayer player,TerminalHost host) {
        player.openMenu(new SimpleMenuProvider((id,inventory,owner)->new MachineMenu(id,inventory,host),
                net.minecraft.network.chat.Component.translatable("menu.me_remote_machine")),buf->{
                    buf.writeNbt(new MachineMenu(0,player.getInventory(),host).snapshot(player));
                });
    }
    public static void openCards(ServerPlayer player,TerminalHost host) {
        if(!host.access(player,false))return;
        if(host.lockedPlayerSlot()<0&&host.cards().getContainerSize()>1) { host.openStorage(player);return; }
        player.openMenu(new SimpleMenuProvider((id,inventory,owner)->new com.syaru.meremote.CardMenu(id,inventory,host),
                net.minecraft.network.chat.Component.translatable("menu.me_remote_machine.card")),buf->{buf.writeVarInt(host.cards().getContainerSize());buf.writeVarInt(host.lockedPlayerSlot());});
    }
    public static void send(ServerPlayer player,WireMessage message) { RemoteNetwork.send(player,message); }
    public static void control(Control control) { RemoteNetwork.control(control); }
    public static void view(com.syaru.meremote.ViewRequest view) { net.neoforged.neoforge.network.PacketDistributor.sendToServer(new RemoteNetwork.View(view)); }
    public static CompoundTag snapshot(BlockEntity target) {
        var tag=target.getUpdateTag(target.getLevel().registryAccess()).copy();tag.putString("id",net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(target.getType()).toString());return tag;
    }
    public static BlockEntity createMirror(Level level,BlockPos pos,BlockState state,CompoundTag tag) {
        if(!(state.getBlock() instanceof EntityBlock block))return null;
        var tile=block.newBlockEntity(pos,state);if(tile!=null){tile.setLevel(level);tile.handleUpdateTag(tag,level.registryAccess());}return tile;
    }
    public static void updateMirror(BlockEntity mirror,CompoundTag tag) { mirror.handleUpdateTag(tag,mirror.getLevel().registryAccess()); }
    private Platform() { }
}
