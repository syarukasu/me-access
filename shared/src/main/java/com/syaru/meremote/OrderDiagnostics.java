package com.syaru.meremote;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.syaru.meremote.mixin.CraftingJobAccessor;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;

public final class OrderDiagnostics {
    public record ActiveOrder(OrderInfo info,AEKey target,JobPatterns tasks) { }
    public static List<ActiveOrder> snapshot(IGrid grid) {
        if(grid==null)return List.of();
        var orders=new java.util.ArrayList<ActiveOrder>();
        int serial=0;
        for(var cpu:grid.getCraftingService().getCpus()) {
            // AE2 assigns display serials to every CPU before filtering active orders.
            serial++;
            if(!cpu.isBusy())continue;
            var status=cpu.getJobStatus();if(status==null)continue;
            JobPatterns tasks=null;String id="";
            if(cpu.getClass()==CraftingCPUCluster.class&&cpu instanceof CraftingCPUCluster cluster) {
                var job=((CraftingJobAccessor)cluster.craftingLogic).remote$currentJob();
                if(job instanceof JobPatterns patterns)tasks=patterns;
                var link=cluster.craftingLogic.getLastLink();if(link!=null)id=link.getCraftingID().toString();
            }
            var name=cpu.getName();
            var info=new OrderInfo(status.crafting().what().getDisplayName().copy(),
                    name==null||name.getString().isBlank()?appeng.core.localization.GuiText.CPUs.text().append(" #"+serial):name.copy(),id);
            orders.add(new ActiveOrder(info,status.crafting().what(),tasks));
        }
        orders.sort(java.util.Comparator.comparing((ActiveOrder o)->o.info.id()).thenComparing(o->o.info.cpu().getString()));
        return orders;
    }
    public static List<OrderInfo> related(List<ActiveOrder> orders,Set<IPatternDetails> patterns) {
        if(patterns.isEmpty())return List.of();
        var result=new java.util.ArrayList<OrderInfo>();
        for(var order:orders)for(var pattern:patterns) {
            boolean matches=order.tasks!=null&&order.tasks.remote$hasPattern(pattern);
            if(!matches)for(var output:pattern.getOutputs())if(output.what().equals(order.target)) { matches=true;break; }
            if(matches) { result.add(order.info);break; }
        }
        return List.copyOf(result);
    }
    private OrderDiagnostics() { }
}
