package com.syaru.meremote.platform;

import com.syaru.meremote.Control;
import com.syaru.meremote.WireMessage;
import com.syaru.meremote.client.ClientMirror;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class RemoteNetwork {
    public record Request(Control message) implements CustomPacketPayload {
        static final Type<Request> TYPE=new Type<>(Platform.id(Platform.ID+":request"));
        static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((buf,p)->p.message.write(buf),buf->new Request(Control.read(buf)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(WireMessage message) implements CustomPacketPayload {
        static final Type<Response> TYPE=new Type<>(Platform.id(Platform.ID+":response"));
        static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((buf,p)->p.message.write(buf),buf->new Response(WireMessage.read(buf)));
        @Override public Type<Response> type() { return TYPE; }
    }
    public record View(com.syaru.meremote.ViewRequest message) implements CustomPacketPayload {
        static final Type<View> TYPE=new Type<>(Platform.id(Platform.ID+":view"));
        static final StreamCodec<RegistryFriendlyByteBuf,View> CODEC=StreamCodec.of((buf,p)->p.message.write(buf),buf->new View(com.syaru.meremote.ViewRequest.read(buf)));
        @Override public Type<View> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("2");
        registrar.playToServer(Request.TYPE,Request.CODEC,(packet,context)->context.enqueueWork(()->packet.message.handle((ServerPlayer)context.player())));
        registrar.playToClient(Response.TYPE,Response.CODEC,(packet,context)->context.enqueueWork(()->ClientMirror.handle(packet.message)));
        registrar.playToServer(View.TYPE,View.CODEC,(packet,context)->context.enqueueWork(()->packet.message.handle((ServerPlayer)context.player())));
    }
    public static void send(ServerPlayer player,WireMessage message) { PacketDistributor.sendToPlayer(player,new Response(message)); }
    public static void control(Control message) { PacketDistributor.sendToServer(new Request(message)); }
}
