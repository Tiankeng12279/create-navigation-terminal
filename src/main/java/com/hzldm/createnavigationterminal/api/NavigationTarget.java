package com.hzldm.createnavigationterminal.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 一个导航目标（不可变）。
 *
 * @param name      显示名
 * @param pos       方块坐标，{@link BlockPos#getY()} 即高度
 * @param dimension 所在维度
 */
public record NavigationTarget(String name, BlockPos pos, ResourceKey<Level> dimension) {

    /** 目标高度（Y） */
    public int height() {
        return pos.getY();
    }

    /** 维度 id，例如 {@code minecraft:overworld} */
    public String dimensionId() {
        return dimension.location().toString();
    }

    /** 由内部数据构造；维度字符串无法解析时返回 null */
    public static NavigationTarget of(String name, int x, int y, int z, String dimension) {
        ResourceLocation rl = ResourceLocation.tryParse(dimension);
        if (rl == null) {
            return null;
        }
        return new NavigationTarget(name, new BlockPos(x, y, z), ResourceKey.create(Registries.DIMENSION, rl));
    }
}
