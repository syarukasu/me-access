package com.syaru.meremote.platform;

import com.syaru.meremote.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;

public final class Platform {
    public static final String ID="me_remote_machine";
    public static MenuType<MachineMenu> menu() { return RemoteMod.MENU.get(); }
    public static Item cardItem() { return RemoteMod.CARD.get(); }
    public static String writeText(net.minecraft.network.chat.Component text,Level level) { return net.minecraft.network.chat.Component.Serializer.toJson(text); }
    public static net.minecraft.network.chat.Component readText(String text,Level level) {
        var value=net.minecraft.network.chat.Component.Serializer.fromJson(text);return value==null?net.minecraft.network.chat.Component.empty():value;
    }
    public static MenuType<com.syaru.meremote.CardMenu> cardMenu() { return RemoteMod.CARD_MENU.get(); }
    public static CompoundTag readCard(ItemStack stack) { var tag=stack.getTagElement("MachineAccess");return tag==null?new CompoundTag():tag.copy(); }
    public static Object cardDataIdentity(ItemStack stack) { return stack.getTagElement("MachineAccess"); }
    public static void writeCard(ItemStack stack,CompoundTag tag) { stack.getOrCreateTag().put("MachineAccess",tag); }
    public static void openStorage(ServerPlayer player,appeng.parts.reporting.AbstractTerminalPart part) {
        appeng.menu.MenuOpener.open(part.getMenuType(player),player,appeng.menu.locator.MenuLocators.forPart(part));
    }
    public static void openWireless(ServerPlayer player,int slot) {
        var stack=player.getInventory().getItem(slot);
        if(stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem item)item.openFromInventory(player,slot);
    }
    public static appeng.helpers.WirelessTerminalMenuHost createWirelessHost(ServerPlayer player,int slot) {
        var stack=player.getInventory().getItem(slot);
        return stack.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem item
                ?(appeng.helpers.WirelessTerminalMenuHost)item.getMenuHost(player,slot,stack,null):null;
    }
    public static boolean wirelessAccess(ServerPlayer player,appeng.helpers.WirelessTerminalMenuHost host) {
        return host.rangeCheck()&&host.extractAEPower(1,appeng.api.config.Actionable.SIMULATE,appeng.api.config.PowerMultiplier.CONFIG)>0;
    }
    public static boolean tickWireless(appeng.helpers.WirelessTerminalMenuHost host,net.minecraft.world.inventory.AbstractContainerMenu menu) {
        return host.onBroadcastChanges(menu);
    }
    public static Item toolItem() { return RemoteMod.TOOL.get(); }
    public static ResourceLocation id(String id) { return new ResourceLocation(id); }
    public static CompoundTag readTool(ItemStack stack) { var tag=stack.getTagElement("RemoteTerminal");return tag==null?new CompoundTag():tag.copy(); }
    public static void writeTool(ItemStack stack,CompoundTag tag) { stack.getOrCreateTag().put("RemoteTerminal",tag); }
    public static MachineAdapter mekanismAdapter(BlockEntity target) {
        return ModList.get().isLoaded("mekanism")&&MekanismAdapter.INSTANCE.supports(target)?MekanismAdapter.INSTANCE:null;
    }
    public static int mekanismInputStatus(BlockEntity target,net.minecraft.core.Direction side) {
        return ModList.get().isLoaded("mekanism")&&target instanceof mekanism.common.tile.base.TileEntityMekanism tile
                ?com.syaru.meremote.MekanismDiagnostics.inputSide(tile,side):0;
    }
    public static void openMachines(ServerPlayer player,TerminalHost host) {
        NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inventory,owner)->new MachineMenu(id,inventory,host),
                net.minecraft.network.chat.Component.translatable("menu.me_remote_machine")),buf->{
                    buf.writeNbt(new MachineMenu(0,player.getInventory(),host).snapshot(player));
                });
    }
    public static void openCards(ServerPlayer player,TerminalHost host) {
        if(!host.access(player,false))return;
        if(host.lockedPlayerSlot()<0&&host.cards().getContainerSize()>1) { host.openStorage(player);return; }
        NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inventory,owner)->new com.syaru.meremote.CardMenu(id,inventory,host),
                net.minecraft.network.chat.Component.translatable("menu.me_remote_machine.card")),buf->{buf.writeVarInt(host.cards().getContainerSize());buf.writeVarInt(host.lockedPlayerSlot());});
    }
    public static void send(ServerPlayer player,WireMessage message) { RemoteNetwork.send(player,message); }
    public static void control(Control control) { RemoteNetwork.CHANNEL.sendToServer(control); }
    public static void view(com.syaru.meremote.ViewRequest view) { RemoteNetwork.CHANNEL.sendToServer(view); }
    public static CompoundTag snapshot(BlockEntity target) {
        var tag=target.getUpdateTag().copy();tag.putString("id",net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(target.getType()).toString());return tag;
    }
    public static BlockEntity createMirror(Level level,BlockPos pos,BlockState state,CompoundTag tag) {
        if(!(state.getBlock() instanceof EntityBlock block))return null;
        var tile=block.newBlockEntity(pos,state);if(tile!=null){tile.setLevel(level);tile.handleUpdateTag(tag);}return tile;
    }
    public static void updateMirror(BlockEntity mirror,CompoundTag tag) { mirror.handleUpdateTag(tag); }
    private Platform() { }
}
