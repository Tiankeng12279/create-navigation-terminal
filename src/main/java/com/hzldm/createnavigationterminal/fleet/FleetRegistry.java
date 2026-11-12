package com.hzldm.createnavigationterminal.fleet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 服务端编队注册表：按「维度 + 频道」索引已加载的导航终端。
 *
 * <p>终端在 {@code onLoad} 注册、{@code setRemoved} 注销。
 * 推送时只扫这个表，不去遍历整个世界。
 */
public final class FleetRegistry {

    /** 维度 → (终端坐标 → 频道号) */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> ENTRIES = new HashMap<>();

    private FleetRegistry() {
    }

    public static void register(ResourceKey<Level> dim, BlockPos pos, int channel) {
        ENTRIES.computeIfAbsent(dim, k -> new HashMap<>()).put(pos, channel);
    }

    public static void unregister(ResourceKey<Level> dim, BlockPos pos) {
        Map<BlockPos, Integer> m = ENTRIES.get(dim);
        if (m != null) {
            m.remove(pos);
            if (m.isEmpty()) {
                ENTRIES.remove(dim);
            }
        }
    }

    /** 更新某终端的频道（切换频道时调用）。 */
    public static void updateChannel(ResourceKey<Level> dim, BlockPos pos, int channel) {
        Map<BlockPos, Integer> m = ENTRIES.get(dim);
        if (m != null && m.containsKey(pos)) {
            m.put(pos, channel);
        }
    }

    /**
     * 取同一维度、同一频道、且不在给定范围内的其他终端坐标。
     *
     * @param dim     维度
     * @param channel 频道号
     * @param exclude 排除的坐标（自己）
     * @param maxDist 最大距离（格），&lt;=0 表示不限
     */
    public static List<BlockPos> sameChannel(ResourceKey<Level> dim, int channel,
                                             BlockPos exclude, double maxDist) {
        List<BlockPos> out = new ArrayList<>();
        Map<BlockPos, Integer> m = ENTRIES.get(dim);
        if (m == null) {
            return out;
        }
        double maxSq = maxDist * maxDist;
        Iterator<Map.Entry<BlockPos, Integer>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> e = it.next();
            if (e.getValue() == null || e.getValue() != channel) {
                continue;
            }
            BlockPos p = e.getKey();
            if (p.equals(exclude)) {
                continue;
            }
            if (maxDist > 0 && p.distSqr(exclude) > maxSq) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    /** 取某维度某频道的全部终端坐标（只读副本，不排除任何位置）。 */
    public static List<BlockPos> members(ResourceKey<Level> dim, int channel) {
        List<BlockPos> out = new ArrayList<>();
        Map<BlockPos, Integer> m = ENTRIES.get(dim);
        if (m == null) {
            return out;
        }
        for (Map.Entry<BlockPos, Integer> e : m.entrySet()) {
            if (e.getValue() != null && e.getValue() == channel) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** 该维度里正在使用的频道号（升序，只读）。 */
    public static List<Integer> channels(ResourceKey<Level> dim) {
        java.util.TreeSet<Integer> set = new java.util.TreeSet<>();
        Map<BlockPos, Integer> m = ENTRIES.get(dim);
        if (m != null) {
            for (Integer ch : m.values()) {
                if (ch != null && ch > 0) {
                    set.add(ch);
                }
            }
        }
        return new ArrayList<>(set);
    }

    /** 服务端关闭时清空（防止跨存档残留）。 */
    public static void clear() {
        ENTRIES.clear();
    }
}
