package com.syaru.meremote;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record Endpoint(UUID id, String dimension, BlockPos pos, String blockId, String label) {
    public Endpoint { pos = pos.immutable(); }
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putUUID("id", id); tag.putString("dimension", dimension); tag.putLong("pos", pos.asLong());
        tag.putString("block", blockId); tag.putString("label", label);
        return tag;
    }
    public static Endpoint read(CompoundTag tag) {
        if (!tag.hasUUID("id") || ResourceLocation.tryParse(tag.getString("dimension")) == null
                || ResourceLocation.tryParse(tag.getString("block")) == null) return null;
        var label = tag.getString("label");
        if (label.length() > 128) return null;
        return new Endpoint(tag.getUUID("id"), tag.getString("dimension"), BlockPos.of(tag.getLong("pos")), tag.getString("block"), label);
    }
}
