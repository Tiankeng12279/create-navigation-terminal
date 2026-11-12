package com.hzldm.createnavigationterminal.api;

import com.hzldm.createnavigationterminal.fleet.FleetRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 编队（频道）只读 API。
 *
 * <p>编队没有独立的“共享目标”状态：推送目标只是把目标复制给同频道的其他终端。
 * 因此 {@link #getSharedTarget} 的语义是「该频道所有终端当前目标一致时返回它」，
 * 只要有一台不同就返回 {@link Optional#empty()}。
 *
 * <p>成员列表来自服务端注册表（终端 {@code onLoad} 注册、移除时注销），
 * 因此只包含<b>已加载</b>的终端。
 */
public final class FleetApi {

    private FleetApi() {
    }

    /** 某维度某频道的全部终端坐标（只读副本，可能为空） */
    public static List<BlockPos> getMembers(ResourceKey<Level> dim, int channel) {
        if (dim == null || channel <= 0) {
            return List.of();
        }
        return List.copyOf(FleetRegistry.members(dim, channel));
    }

    /** 某维度正在使用的频道号（升序；不含 0） */
    public static List<Integer> getChannels(ResourceKey<Level> dim) {
        if (dim == null) {
            return List.of();
        }
        return List.copyOf(FleetRegistry.channels(dim));
    }

    /** 该终端所在频道的全部成员（含自己）；非终端或未入队返回空表 */
    public static List<BlockPos> getMembersOf(BlockGetter level, BlockPos pos) {
        NavigationApi.getTarget(level, pos);   // 触发空安全路径（非终端会走到 empty）
        int ch = NavigationApi.getFleetChannel(level, pos);
        if (ch <= 0 || level == null || pos == null) {
            return List.of();
        }
        ResourceKey<Level> dim = dimensionOf(level);
        return dim == null ? List.of() : getMembers(dim, ch);
    }

    /**
     * 该频道的共享目标：所有已加载成员的目标一致时返回它，否则空。
     * 频道内没有成员时也返回空。
     */
    public static Optional<NavigationTarget> getSharedTarget(BlockGetter level, ResourceKey<Level> dim, int channel) {
        NavigationTarget agreed = null;
        boolean any = false;
        for (BlockPos p : getMembers(dim, channel)) {
            Optional<NavigationTarget> t = NavigationApi.getTarget(level, p);
            if (t.isEmpty()) {
                return Optional.empty();          // 有成员没有目标 → 不算一致
            }
            NavigationTarget nt = t.get();
            if (!any) {
                agreed = nt;
                any = true;
            } else if (!sameTarget(agreed, nt)) {
                return Optional.empty();
            }
        }
        return any ? Optional.ofNullable(agreed) : Optional.empty();
    }

    private static boolean sameTarget(NavigationTarget a, NavigationTarget b) {
        return a != null && b != null
                && a.pos().equals(b.pos())
                && a.dimensionId().equals(b.dimensionId())
                && a.name().equals(b.name());
    }

    private static ResourceKey<Level> dimensionOf(BlockGetter level) {
        return level instanceof Level l ? l.dimension() : null;
    }
}
