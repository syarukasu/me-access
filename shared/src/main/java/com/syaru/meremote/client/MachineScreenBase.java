package com.syaru.meremote.client;

import com.syaru.meremote.*;
import com.syaru.meremote.platform.Platform;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public abstract class MachineScreenBase extends AbstractContainerScreen<MachineMenu> {
    private static final int ROW_HEIGHT=34;
    private final Button[] open=new Button[9],remove=new Button[9];
    private EditBox search;
    private int offset;
    private int visibleRows=9;
    private List<Machines.MachineRow> shown=List.of();
    private List<java.util.UUID> lastVisible=List.of();
    protected MachineScreenBase(MachineMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=246;imageHeight=280;
    }
    private void send(int action,java.util.UUID id) { Platform.control(new Control(menu.containerId,action,id)); }
    @Override protected void init() {
        visibleRows=Math.min(9,Math.max(1,(height-82)/ROW_HEIGHT));imageHeight=82+visibleRows*ROW_HEIGHT;
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("tab.me_remote_machine.items"),b->send(Control.STORAGE,Control.NONE))
                .bounds(leftPos+8,topPos+6,66,20).build());
        var current=addRenderableWidget(Button.builder(Component.translatable("tab.me_remote_machine.machines"),b->{})
                .bounds(leftPos+76,topPos+6,76,20).build());current.active=false;
        var cards=new TerminalIconButton(Platform.cardItem().getDefaultInstance(),Component.translatable("menu.me_remote_machine.card"),
                b->send(Control.CARDS,Control.NONE));cards.setPosition(leftPos+194,topPos+6);addRenderableWidget(cards);
        var bind=new TerminalIconButton(Platform.toolItem().getDefaultInstance(),Component.translatable("button.me_remote_machine.bind"),
                b->send(Control.BIND,Control.NONE));bind.setPosition(leftPos+218,topPos+6);addRenderableWidget(bind);
        var refresh=addRenderableWidget(Button.builder(Component.literal("\u21bb"),b->send(Control.REFRESH,Control.NONE))
                .bounds(leftPos+218,topPos+30,20,18).build());
        refresh.setTooltip(Tooltip.create(Component.translatable("button.me_remote_machine.refresh")));
        search=new EditBox(font,leftPos+8,topPos+31,204,16,Component.translatable("button.me_remote_machine.search"));
        search.setMaxLength(128);search.setResponder(value->offset=0);addRenderableWidget(search);
        for(int i=0;i<9;i++) {
            int row=i;
            open[i]=addRenderableWidget(Button.builder(Component.empty(),b->{var e=entry(row);if(e!=null)send(Control.OPEN,e.endpoint().id());})
                    .bounds(leftPos+8,topPos+54+i*ROW_HEIGHT,208,ROW_HEIGHT-1).build());
            remove[i]=addRenderableWidget(Button.builder(Component.literal("\u00d7"),b->{var e=entry(row);if(e!=null)send(Control.REMOVE,e.endpoint().id());})
                    .bounds(leftPos+220,topPos+54+i*ROW_HEIGHT,18,ROW_HEIGHT-1).build());
            remove[i].setTooltip(Tooltip.create(Component.translatable("button.me_remote_machine.remove")));
            open[i].visible=remove[i].visible=i<visibleRows;
        }
        addRenderableWidget(Button.builder(Component.literal("^"),b->scroll(1)).bounds(leftPos+196,topPos+imageHeight-22,20,16).build());
        addRenderableWidget(Button.builder(Component.literal("v"),b->scroll(-1)).bounds(leftPos+218,topPos+imageHeight-22,20,16).build());
    }
    private Machines.MachineRow entry(int row) { int i=offset+row;return row<visibleRows&&i>=0&&i<shown.size()?shown.get(i):null; }
    protected boolean scroll(double delta) { offset=Math.max(0,Math.min(Math.max(0,shown.size()-visibleRows),offset+(delta>0?-1:1)));return true; }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF373737);
        g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+imageHeight-2,0xFFC6C6C6);
        String query=search==null?"":search.getValue().toLowerCase(Locale.ROOT);
        shown=menu.rows().stream().filter(row->displayName(row.endpoint()).toLowerCase(Locale.ROOT).contains(query)
                || row.endpoint().blockId().toLowerCase(Locale.ROOT).contains(query)
                || row.orders().stream().anyMatch(o->o.target().getString().toLowerCase(Locale.ROOT).contains(query)||o.cpu().getString().toLowerCase(Locale.ROOT).contains(query))).toList();
        offset=Math.min(offset,Math.max(0,shown.size()-visibleRows));
        var visible=shown.stream().skip(offset).limit(visibleRows).map(row->row.endpoint().id()).toList();
        if(!visible.equals(lastVisible)) { lastVisible=visible;Platform.view(new ViewRequest(menu.containerId,visible)); }
        for(int i=0;i<9;i++) {
            var row=entry(i);open[i].visible=remove[i].visible=row!=null;
            if(row!=null) {
                open[i].active=row.available();
                var e=row.endpoint();
                var tooltip=Component.literal(displayName(e)+"\n"+e.blockId()+"\n"+e.dimension()+" "+e.pos().toShortString())
                        .append("\n").append(MachineStatus.text(row.status()));
                for(var order:row.orders()) {
                    tooltip.append("\n").append(Component.translatable("menu.me_remote_machine.related_order",order.target())).append(" / ").append(order.cpu());
                    if(!order.id().isBlank())tooltip.append("\n").append(order.id());
                }
                open[i].setTooltip(Tooltip.create(tooltip));
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) { }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        g.fill(0,0,width,height,0x80000000);super.render(g,mouseX,mouseY,partial);
        for(int i=0;i<9;i++) {
            var row=entry(i);if(row==null)continue;
            var e=row.endpoint();int x=leftPos+12,y=topPos+57+i*ROW_HEIGHT;
            var block=BuiltInRegistries.BLOCK.get(Platform.id(e.blockId()));g.renderItem(new ItemStack(block),x,y);
            int color=row.available()?0xFFFFFFFF:0xFFA0A0A0;
            g.drawString(font,fit(displayName(e),156),x+21,y,color,false);
            boolean working=(row.status()&MachineStatus.WORKING.bit())!=0;
            var primary=MachineStatus.primaryText(row.status());
            g.drawString(font,fit(primary.getString(),174),x+21,y+10,working?0xFF81D967:MachineStatus.problem(row.status())?0xFFFFD37A:0xFFBBBBBB,false);
            Component orderText=row.orders().isEmpty()?Component.translatable("menu.me_remote_machine.no_related_order"):
                    Component.translatable("menu.me_remote_machine.related_order",row.orders().get(0).target()).append(" / ").append(row.orders().get(0).cpu());
            if(row.orders().size()>1)orderText=orderText.copy().append(Component.translatable("menu.me_remote_machine.more_orders",row.orders().size()-1));
            g.drawString(font,fit(orderText.getString(),174),x+21,y+20,0xFFBFC8D4,false);
            int indicator=!row.available()?0xFFBD6D66:working?0xFF81D967:MachineStatus.problem(row.status())?0xFFE1B853:0xFF999999;
            g.fill(leftPos+206,y+5,leftPos+210,y+9,indicator);
        }
        var count=Component.literal(shown.size()+" / "+MachineBook.CAPACITY);
        g.drawString(font,count,leftPos+8,topPos+imageHeight-19,0xFF303030,false);
        if(shown.isEmpty())g.drawCenteredString(font,Component.translatable("menu.me_remote_machine.empty"),leftPos+imageWidth/2,topPos+54+(imageHeight-82)/2,0xFF555555);
        renderTooltip(g,mouseX,mouseY);
    }
    private String fit(String text,int limit) { return font.width(text)<=limit?text:font.plainSubstrByWidth(text,limit-font.width("..."))+"..."; }
    private static String displayName(Endpoint endpoint) {
        if(!endpoint.label().isBlank())return endpoint.label();
        var key=Platform.id(endpoint.blockId());
        return BuiltInRegistries.BLOCK.containsKey(key)?BuiltInRegistries.BLOCK.get(key).getName().getString():endpoint.blockId();
    }
}
