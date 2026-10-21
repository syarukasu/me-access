package com.syaru.meremote;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class MachineBook {
    public static final int CAPACITY = 256;
    private UUID id = UUID.randomUUID();
    private final List<Endpoint> targets = new ArrayList<>();
    public UUID id() { return id; }
    public List<Endpoint> entries() { return List.copyOf(targets); }
    public Endpoint find(UUID target) { return targets.stream().filter(e -> e.id().equals(target)).findFirst().orElse(null); }
    public boolean add(Endpoint endpoint) {
        if (targets.stream().anyMatch(e -> e.dimension().equals(endpoint.dimension()) && e.pos().equals(endpoint.pos()))) return false;
        if (targets.size() == CAPACITY) return false;
        targets.add(endpoint); return true;
    }
    public boolean remove(UUID target) { return targets.removeIf(e -> e.id().equals(target)); }
    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putUUID("id", id);
        var list = new ListTag(); for (var e : targets) list.add(e.save());
        tag.put("machines", list); return tag;
    }
    public void load(CompoundTag tag) {
        if (tag.hasUUID("id")) id = tag.getUUID("id");
        targets.clear(); var list = tag.getList("machines", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), CAPACITY); i++) {
            var e = Endpoint.read(list.getCompound(i)); if (e != null && find(e.id()) == null) add(e);
        }
    }
}
