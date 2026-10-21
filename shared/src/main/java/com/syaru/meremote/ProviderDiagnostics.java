package com.syaru.meremote;

import appeng.api.config.LockCraftingMode;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.syaru.meremote.platform.Platform;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ProviderDiagnostics {
    public record Report(int flags,java.util.List<OrderInfo> orders) { }
    public static Report read(TerminalHost terminal,BlockEntity machine,java.util.List<OrderDiagnostics.ActiveOrder> orders) {
        var level=machine.getLevel();int flags=0;
        var patterns=new java.util.LinkedHashSet<appeng.api.crafting.IPatternDetails>();
        if(level==null)return new Report(0,java.util.List.of());
        for(var side:Direction.values()) {
            var pos=machine.getBlockPos().relative(side);
            if(!level.hasChunkAt(pos))continue;
            if(level.getBlockEntity(pos) instanceof PatternProviderLogicHost provider)
                flags|=read(terminal,machine,provider,side,patterns);
            if(PartHelper.getPart(level,pos,side.getOpposite()) instanceof PatternProviderLogicHost provider)
                flags|=read(terminal,machine,provider,side,patterns);
        }
        return new Report(flags,OrderDiagnostics.related(orders,patterns));
    }
    private static int read(TerminalHost terminal,BlockEntity machine,PatternProviderLogicHost provider,Direction machineSide,java.util.Set<appeng.api.crafting.IPatternDetails> related) {
        if(provider.getGrid()!=terminal.grid()||!provider.getTargets().contains(machineSide.getOpposite()))return 0;
        var logic=provider.getLogic();var patterns=logic.getAvailablePatterns();
        if(patterns.isEmpty())return 0;
        related.addAll(patterns);
        int flags=0;
        if(logic instanceof ProviderActivity activity) {
            long tick=machine.getLevel().getGameTime();activity.remote$watch(tick);
            if(!activity.remote$active())flags|=MachineStatus.PROVIDER_INACTIVE.bit();
            if(logic.isBusy()&&activity.remote$pendingDirection()==machineSide.getOpposite())flags|=MachineStatus.PROVIDER_SEND_WAIT.bit();
            int reasons=activity.remote$rejectionReasons(tick,machineSide.getOpposite());
            flags|=reasons;
            if(activity.remote$rejected(tick)&&reasons==0)flags|=MachineStatus.PROVIDER_REJECTED.bit();
        }
        var lock=logic.getCraftingLockedReason();
        if(lock!=LockCraftingMode.NONE)flags|=(lock==LockCraftingMode.LOCK_UNTIL_RESULT?MachineStatus.PROVIDER_RESULT_WAIT:MachineStatus.PROVIDER_LOCKED).bit();
        if(!logic.getReturnInv().isEmpty())flags|=MachineStatus.PROVIDER_RETURN_WAIT.bit();
        boolean usesItems=false;
        outer:for(var pattern:patterns)for(var input:pattern.getInputs())for(var possible:input.getPossibleInputs())if(possible.what() instanceof AEItemKey) {
            usesItems=true;break outer;
        }
        if(usesItems)flags|=Platform.mekanismInputStatus(machine,machineSide);
        return flags;
    }
    private ProviderDiagnostics() { }
}
