package com.syaru.meremote;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class OutputDestinationDiagnostics {
    /** Only report full when every available supported destination is demonstrably full. */
    public static boolean full(BlockEntity machine,List<Direction> sides) {
        var level=machine.getLevel();boolean found=false;
        if(level==null)return false;
        for(var side:sides) {
            var pos=machine.getBlockPos().relative(side);
            if(!level.hasChunkAt(pos))return false;
            var target=level.getBlockEntity(pos);
            if(target==null&&level.getBlockState(pos).isAir())continue;
            if(target==null)return false;
            var type=target.getClass();
            if(type!=ChestBlockEntity.class&&type!=BarrelBlockEntity.class&&type!=ShulkerBoxBlockEntity.class)return false;
            if(target instanceof ChestBlockEntity&&(!target.getBlockState().hasProperty(ChestBlock.TYPE)
                    ||target.getBlockState().getValue(ChestBlock.TYPE)!=ChestType.SINGLE))return false;
            var inventory=(Container)target;int size=inventory.getContainerSize();
            if(size==0)return false;
            found=true;
            for(int i=0;i<size;i++) {
                var stack=inventory.getItem(i);
                if(stack.isEmpty()||stack.getCount()<Math.min(inventory.getMaxStackSize(),stack.getMaxStackSize()))return false;
            }
        }
        return found;
    }
    private OutputDestinationDiagnostics() { }
}
