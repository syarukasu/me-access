package com.syaru.meremote.client;

import com.syaru.meremote.CardMenu;
import com.syaru.meremote.Control;
import com.syaru.meremote.platform.Platform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CardScreen extends AbstractContainerScreen<CardMenu> {
    public CardScreen(CardMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=158;inventoryLabelY=64;
    }
    @Override protected void init() {
        super.init();
        var back=new TerminalIconButton(Platform.cardItem().getDefaultInstance(),Component.translatable("tab.me_remote_machine.machines"),
                b->Platform.control(new Control(menu.containerId,Control.MACHINES,Control.NONE)));
        back.setPosition(leftPos+imageWidth+2,topPos);addRenderableWidget(back);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF373737);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xFFC6C6C6);
        for(var slot:menu.slots) {
            int sx=leftPos+slot.x,sy=topPos+slot.y;
            g.fill(sx-1,sy-1,sx+17,sy+17,0xFF373737);g.fill(sx,sy,sx+16,sy+16,0xFF8B8B8B);
        }
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        g.fill(0,0,width,height,0x80000000);super.render(g,x,y,partial);renderTooltip(g,x,y);
    }
}
