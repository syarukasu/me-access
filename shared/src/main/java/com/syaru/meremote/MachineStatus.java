package com.syaru.meremote;

import net.minecraft.network.chat.Component;

/** Flags describe observed state, not a simulated recipe or an inferred failure. */
public enum MachineStatus {
    UNCHECKED, WORKING, UNKNOWN, OUTPUT_FULL, ENERGY, ENERGY_REDUCED, INPUT, SECONDARY_INPUT,
    LEFT_INPUT, RIGHT_INPUT, NO_RECIPE, REDSTONE, UNLOADED, DIMENSION, REMOVED, UNSUPPORTED, DENIED, DISCONNECTED, IN_PROGRESS,
    OUTPUT_DISABLED, AUTO_OUTPUT_OFF, PROVIDER_INPUT_DISABLED, PROVIDER_SEND_WAIT, PROVIDER_RETURN_WAIT,
    PROVIDER_LOCKED, PROVIDER_RESULT_WAIT, PROVIDER_INACTIVE, PROVIDER_REJECTED,
    PROVIDER_NO_TARGET, PROVIDER_BLOCKING, PROVIDER_INSERT_REJECTED, DESTINATION_FULL;
    private static final MachineStatus[] DISPLAY_ORDER=values();
    static { java.util.Arrays.sort(DISPLAY_ORDER,java.util.Comparator.comparingInt(MachineStatus::displayPriority)); }
    private int displayPriority() {
        return switch(this) {
            case PROVIDER_NO_TARGET, PROVIDER_BLOCKING, PROVIDER_INSERT_REJECTED -> 0;
            case PROVIDER_INPUT_DISABLED, OUTPUT_DISABLED, DESTINATION_FULL -> 1;
            default -> problem(bit())?2:3;
        };
    }
    public int bit() { return 1<<ordinal(); }
    public static Component primaryText(int flags) {
        return (flags&WORKING.bit())!=0?Component.translatable("status.me_remote_machine.working"):text(flags);
    }
    public static Component text(int flags) {
        var result=Component.empty();
        for(var status:DISPLAY_ORDER)if((flags&status.bit())!=0) {
            if(!result.getString().isEmpty())result.append(" / ");
            result.append(Component.translatable("status.me_remote_machine."+status.name().toLowerCase(java.util.Locale.ROOT)));
        }
        return result;
    }
    public static boolean problem(int flags) {
        return (flags&~(UNCHECKED.bit()|WORKING.bit()|UNKNOWN.bit()|IN_PROGRESS.bit()))!=0;
    }
}
