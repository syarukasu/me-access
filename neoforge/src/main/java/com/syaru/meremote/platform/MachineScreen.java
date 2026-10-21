package com.syaru.meremote.platform;

import com.syaru.meremote.MachineMenu;
import com.syaru.meremote.client.MachineScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MachineScreen extends MachineScreenBase {
    public MachineScreen(MachineMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) { return scroll(vertical); }
}
