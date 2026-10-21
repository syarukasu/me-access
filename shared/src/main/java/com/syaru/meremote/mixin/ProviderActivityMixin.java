package com.syaru.meremote.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import com.syaru.meremote.ProviderActivity;
import com.syaru.meremote.MachineStatus;
import com.syaru.meremote.ObservedProviderTarget;
import java.util.Arrays;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=PatternProviderLogic.class,remap=false)
public abstract class ProviderActivityMixin implements ProviderActivity {
    @Shadow @Final private IManagedGridNode mainNode;
    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow private Direction sendDirection;
    @Unique private long remote$watchUntil=-1;
    @Unique private long remote$rejectedAt=Long.MIN_VALUE;
    @Unique private ObservedProviderTarget[] remote$targets;
    @Unique private int[] remote$reasons;
    @Unique private long[] remote$observedAt;
    @Override public void remote$watch(long tick) {
        remote$watchUntil=tick+40;
        if(remote$targets==null) {
            remote$targets=new ObservedProviderTarget[6];remote$reasons=new int[6];remote$observedAt=new long[6];
            Arrays.fill(remote$observedAt,Long.MIN_VALUE);
        }
    }
    @Override public boolean remote$rejected(long tick) { return remote$rejectedAt>=tick-40; }
    @Override public int remote$rejectionReasons(long tick,Direction side) {
        return remote$reasons!=null&&remote$observedAt[side.ordinal()]>=tick-40?remote$reasons[side.ordinal()]:0;
    }
    @Override public void remote$recordRejection(Direction side,int reason) {
        if(remote$watchUntil<0)return;
        var level=host.getBlockEntity().getLevel();
        if(level==null||level.getGameTime()>remote$watchUntil)return;
        remote$reasons[side.ordinal()]|=reason;remote$observedAt[side.ordinal()]=level.getGameTime();
    }
    @Override public boolean remote$active() { return mainNode.isActive(); }
    @Override public Direction remote$pendingDirection() { return sendDirection; }
    @Inject(method="pushPattern",at=@At("HEAD"))
    private void remote$begin(IPatternDetails pattern,KeyCounter[] inputs,CallbackInfoReturnable<Boolean> ci) {
        if(remote$watchUntil<0)return;
        var level=host.getBlockEntity().getLevel();
        if(level==null||level.getGameTime()>remote$watchUntil)return;
        Arrays.fill(remote$reasons,0);Arrays.fill(remote$observedAt,Long.MIN_VALUE);
    }
    @Inject(method="findAdapter",at=@At("RETURN"),cancellable=true)
    private void remote$target(Direction side,CallbackInfoReturnable<PatternProviderTarget> ci) {
        if(remote$watchUntil<0)return;
        var level=host.getBlockEntity().getLevel();
        if(level==null||level.getGameTime()>remote$watchUntil)return;
        int index=side.ordinal();var target=ci.getReturnValue();
        remote$observedAt[index]=level.getGameTime();
        remote$reasons[index]=target==null?MachineStatus.PROVIDER_NO_TARGET.bit():0;
        if(target==null)return;
        var observed=remote$targets[index];
        if(observed==null||observed.delegate()!=target)remote$targets[index]=observed=new ObservedProviderTarget(target,this,side);
        ci.setReturnValue(observed);
    }
    @Inject(method="pushPattern",at=@At("RETURN"))
    private void remote$observe(IPatternDetails pattern,KeyCounter[] inputs,CallbackInfoReturnable<Boolean> ci) {
        if(remote$watchUntil<0)return;
        var level=host.getBlockEntity().getLevel();
        if(level==null)return;
        long tick=level.getGameTime();
        if(tick>remote$watchUntil) {
            remote$watchUntil=-1;remote$rejectedAt=Long.MIN_VALUE;remote$targets=null;remote$reasons=null;remote$observedAt=null;return;
        }
        remote$rejectedAt=ci.getReturnValueZ()?Long.MIN_VALUE:tick;
        if(ci.getReturnValueZ())Arrays.fill(remote$reasons,0);
    }
}
