package com.syaru.meremote.platform;

import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import com.syaru.meremote.RemoteSessions;
import com.syaru.meremote.client.ClientMirror;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

public record RemoteMenuLocator(BlockPos pos,UUID token) implements MenuLocator {
    public static void register() {
        MenuLocators.register(RemoteMenuLocator.class,(locator,buf)->{buf.writeBlockPos(locator.pos);buf.writeUUID(locator.token);},
                buf->new RemoteMenuLocator(buf.readBlockPos(),buf.readUUID()));
    }
    @Override public <T> T locate(Player player,Class<T> type) {
        var target=player instanceof ServerPlayer server?RemoteSessions.locate(server,token,pos):
                token.equals(ClientMirror.token())?ClientMirror.lookup(player.level(),pos):null;
        return type.isInstance(target)?type.cast(target):null;
    }
}
