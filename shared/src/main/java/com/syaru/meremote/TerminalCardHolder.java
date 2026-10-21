package com.syaru.meremote;

import net.minecraft.world.Container;

public interface TerminalCardHolder {
    Container remoteMachineCards();
    boolean remoteMachineHasLegacyCard();
}
