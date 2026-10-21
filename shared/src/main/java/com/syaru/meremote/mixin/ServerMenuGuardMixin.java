package com.syaru.meremote.mixin;

import com.syaru.meremote.RemoteSessions;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMenuGuardMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleContainerClick",at=@At("HEAD"),cancellable=true)
    private void remote$click(ServerboundContainerClickPacket packet,CallbackInfo ci) {
        if(player.server.isSameThread()&&!RemoteSessions.authorizeCurrent(player)) { player.closeContainer();RemoteSessions.clear(player);ci.cancel(); }
    }
    @Inject(method="handleContainerButtonClick",at=@At("HEAD"),cancellable=true)
    private void remote$button(ServerboundContainerButtonClickPacket packet,CallbackInfo ci) {
        if(player.server.isSameThread()&&!RemoteSessions.authorizeCurrent(player)) { player.closeContainer();RemoteSessions.clear(player);ci.cancel(); }
    }
}
