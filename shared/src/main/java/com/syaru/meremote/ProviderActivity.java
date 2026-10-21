package com.syaru.meremote;

import net.minecraft.core.Direction;

/** Passive observations of AE2, never a second dispatch or crafting operation. */
public interface ProviderActivity {
    void remote$watch(long tick);
    boolean remote$rejected(long tick);
    int remote$rejectionReasons(long tick,Direction side);
    void remote$recordRejection(Direction side,int reason);
    boolean remote$active();
    Direction remote$pendingDirection();
}
