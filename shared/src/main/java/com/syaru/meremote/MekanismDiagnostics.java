package com.syaru.meremote;

import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.tile.prefab.TileEntityRecipeMachine;
import mekanism.api.RelativeSide;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import net.minecraft.core.Direction;

/** Optional Mekanism bridge: reads the same error flags used by its own machine screens. */
public final class MekanismDiagnostics {
    private static final RecipeError[] ERRORS={RecipeError.NOT_ENOUGH_OUTPUT_SPACE,RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_ENERGY_REDUCED_RATE,RecipeError.NOT_ENOUGH_INPUT,RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_LEFT_INPUT,RecipeError.NOT_ENOUGH_RIGHT_INPUT,RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT};
    private static final MachineStatus[] STATES={MachineStatus.OUTPUT_FULL,MachineStatus.ENERGY,MachineStatus.ENERGY_REDUCED,
            MachineStatus.INPUT,MachineStatus.SECONDARY_INPUT,MachineStatus.LEFT_INPUT,MachineStatus.RIGHT_INPUT,MachineStatus.NO_RECIPE};
    public static int read(TileEntityMekanism tile,boolean redstoneEnabled) {
        int flags=tile.getActive()?MachineStatus.WORKING.bit():0;
        if(!redstoneEnabled)flags|=MachineStatus.REDSTONE.bit();
        if(tile instanceof TileEntityRecipeMachine<?> machine) {
            for(int i=0;i<ERRORS.length;i++)if(machine.getWarningCheck(ERRORS[i]).getAsBoolean())flags|=STATES[i].bit();
        } else if(tile instanceof TileEntityFactory<?> factory) {
            for(int i=0;i<ERRORS.length;i++)for(int process=0;process<factory.progress.length;process++) {
                if(factory.getWarningCheck(ERRORS[i],process).getAsBoolean()) { flags|=STATES[i].bit();break; }
            }
        }
        flags|=transport(tile);
        return flags==0?MachineStatus.UNKNOWN.bit():flags;
    }
    private static int transport(TileEntityMekanism tile) {
        if(!(tile instanceof ISideConfiguration sided))return 0;
        int flags=0;
        for(var type:sided.getConfig().getTransmissions()) {
            var config=sided.getConfig().getConfig(type);
            if(config==null)continue;
            boolean outputSupported=false,storedItems=false,outputSide=false;
            for(var mode:config.getSupportedDataTypes()) {
                var info=config.getSlotInfo(mode);
                if(info==null||!info.canOutput())continue;
                outputSupported=true;
                if(info instanceof InventorySlotInfo slots)for(var slot:slots.getSlots())if(!slot.isEmpty())storedItems=true;
            }
            if(!outputSupported)continue;
            for(var side:RelativeSide.values()) {
                var info=config.getSlotInfo(side);
                if(config.isSideEnabled(side)&&info!=null&&info.isEnabled()&&info.canOutput()) { outputSide=true;break; }
            }
            if(!outputSide)flags|=MachineStatus.OUTPUT_DISABLED.bit();
            else if(type==TransmissionType.ITEM&&storedItems&&config.canEject()&&!config.isEjecting())flags|=MachineStatus.AUTO_OUTPUT_OFF.bit();
            else if(type==TransmissionType.ITEM&&storedItems&&config.isEjecting()) {
                var sides=new java.util.ArrayList<Direction>();
                for(var side:Direction.values()) {
                    var info=sided.getConfig().getSlotInfo(type,side);
                    if(info!=null&&info.isEnabled()&&info.canOutput())sides.add(side);
                }
                if(OutputDestinationDiagnostics.full(tile,sides))flags|=MachineStatus.DESTINATION_FULL.bit();
            }
        }
        return flags;
    }
    public static int inputSide(TileEntityMekanism tile,Direction side) {
        if(!(tile instanceof ISideConfiguration sided)||!sided.getConfig().supports(TransmissionType.ITEM))return 0;
        var info=sided.getConfig().getSlotInfo(TransmissionType.ITEM,side);
        return info==null||!info.isEnabled()||!info.canInput()?MachineStatus.PROVIDER_INPUT_DISABLED.bit():0;
    }
    private MekanismDiagnostics() { }
}
