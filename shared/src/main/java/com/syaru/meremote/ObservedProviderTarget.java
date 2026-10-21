package com.syaru.meremote;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.helpers.patternprovider.PatternProviderTarget;
import java.util.Set;
import net.minecraft.core.Direction;

/** Delegates each native I/O operation exactly once; no diagnostic probes or item movement. */
public final class ObservedProviderTarget implements PatternProviderTarget {
    private final PatternProviderTarget delegate;
    private final ProviderActivity activity;
    private final Direction side;
    public ObservedProviderTarget(PatternProviderTarget delegate,ProviderActivity activity,Direction side) {
        this.delegate=delegate;this.activity=activity;this.side=side;
    }
    public PatternProviderTarget delegate() { return delegate; }
    @Override public long insert(AEKey key,long amount,Actionable mode) {
        long inserted=delegate.insert(key,amount,mode);
        if(amount>0&&inserted==0)activity.remote$recordRejection(side,MachineStatus.PROVIDER_INSERT_REJECTED.bit());
        return inserted;
    }
    @Override public boolean containsPatternInput(Set<AEKey> inputs) {
        boolean blocked=delegate.containsPatternInput(inputs);
        if(blocked)activity.remote$recordRejection(side,MachineStatus.PROVIDER_BLOCKING.bit());
        return blocked;
    }
}
