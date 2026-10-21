package com.syaru.meremote;

import com.syaru.meremote.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** A related live CPU order, not proof that this provider exclusively owns its dispatch. */
public record OrderInfo(Component target,Component cpu,String id) {
    public CompoundTag save(Level level) {
        var tag=new CompoundTag();tag.putString("target",Platform.writeText(target,level));
        tag.putString("cpu",Platform.writeText(cpu,level));tag.putString("job",id);return tag;
    }
    public static OrderInfo read(CompoundTag tag,Level level) {
        return new OrderInfo(Platform.readText(tag.getString("target"),level),Platform.readText(tag.getString("cpu"),level),tag.getString("job"));
    }
}
