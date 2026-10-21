package com.syaru.meremote.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.execution.ExecutingCraftingJob;
import com.syaru.meremote.JobPatterns;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value=ExecutingCraftingJob.class,remap=false)
public abstract class JobPatternsMixin implements JobPatterns {
    @Shadow @Final private Map<IPatternDetails,?> tasks;
    @Override public boolean remote$hasPattern(IPatternDetails pattern) { return tasks.containsKey(pattern); }
}
