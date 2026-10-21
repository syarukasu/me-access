package com.syaru.meremote.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class TerminalIconButton extends Button {
    private final ItemStack icon;
    public TerminalIconButton(ItemStack icon,Component tooltip,OnPress action) {
        super(0,0,20,20,Component.empty(),action,DEFAULT_NARRATION);this.icon=icon;
        setTooltip(Tooltip.create(tooltip));
    }
    @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial) {
        super.renderWidget(g,mouseX,mouseY,partial);g.renderItem(icon,getX()+2,getY()+2);
    }
}
