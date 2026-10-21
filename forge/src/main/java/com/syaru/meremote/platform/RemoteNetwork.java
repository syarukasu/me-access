package com.syaru.meremote.platform;

import com.syaru.meremote.Control;
import com.syaru.meremote.WireMessage;
import com.syaru.meremote.client.ClientMirror;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RemoteNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Platform.id(Platform.ID+":main"),()->"2","2"::equals,"2"::equals);
    public static void register() {
        CHANNEL.messageBuilder(Control.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Control::write).decoder(Control::read)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
        CHANNEL.messageBuilder(WireMessage.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(WireMessage::write).decoder(WireMessage::read)
                .consumerMainThread((packet,context)->ClientMirror.handle(packet)).add();
        CHANNEL.messageBuilder(com.syaru.meremote.ViewRequest.class,2,NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.syaru.meremote.ViewRequest::write).decoder(com.syaru.meremote.ViewRequest::read)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
    }
    public static void send(ServerPlayer player,WireMessage message) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message); }
}
