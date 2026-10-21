package com.syaru.meremote.mixin;

import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=CraftingCpuLogic.class,remap=false)
public interface CraftingJobAccessor {
    @Accessor("job") ExecutingCraftingJob remote$currentJob();
}
