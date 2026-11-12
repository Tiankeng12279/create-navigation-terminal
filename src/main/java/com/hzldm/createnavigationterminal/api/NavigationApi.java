package com.hzldm.createnavigationterminal.api;

import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import com.hzldm.createnavigationterminal.item.WpTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 导航终端只读 API 主入口。全部为静态方法，不持有状态。
 *
 * <pre>{@code
 * // 读取某台终端的目标高度与三维距离
 * NavigationApi.getTarget(level, pos).ifPresent(t ->
 *         System.out.println(t.name() + " 高度 " + t.height()));
 * NavigationDistances d = NavigationApi.getDistances(level, pos);
 * if (d.isKnown()) System.out.println("水平 " + d.horizontal() + " 垂直 " + d.vertical());
 * }</pre>
 */
public final class NavigationApi {

    private NavigationApi() {
    }

    /** 该位置是否为导航终端 */
    public static boolean isTerminal(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).isPresent();
    }

    /** 当前目标；无目标返回 {@link Optional#empty()} */
    public static Optional<NavigationTarget> getTarget(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).flatMap(be -> be.hasTarget()
                ? Optional.ofNullable(NavigationTarget.of(
                        be.getTargetName(), be.getTargetX(), be.getTargetY(), be.getTargetZ(),
                        be.getTargetDimension()))
                : Optional.empty());
    }

    /** 目标高度（Y）；无目标返回 {@link Optional#empty()} */
    public static Optional<Integer> getTargetHeight(BlockGetter level, BlockPos pos) {
        return getTarget(level, pos).map(NavigationTarget::height);
    }

    /** 当前航线（不可变副本，保持站序） */
    public static List<NavigationTarget> getRoute(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).map(be -> {
            List<NavigationTarget> out = new ArrayList<>();
            for (WpTarget t : be.getRoute()) {
                NavigationTarget nt = NavigationTarget.of(t.name(), t.x(), t.y(), t.z(), t.dimension());
                if (nt != null) {
                    out.add(nt);
                }
            }
            return List.copyOf(out);
        }).orElse(List.of());
    }

    /** 当前站序号；无目标或无航线返回 -1 */
    public static int getRouteIndex(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).map(NavigationTerminalBlockEntity::getRouteIndex).orElse(-1);
    }

    /**
     * 到目标的距离。导航信号只使用水平分量；高度差与三维距离供外部参考。
     * 无目标 / 跨维度 / 未加载时返回 {@link NavigationDistances#unknown()}
     */
    public static NavigationDistances getDistances(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).map(be -> {
            double horizontal = be.distanceToTarget();
            if (horizontal < 0) {
                return NavigationDistances.unknown();
            }
            double vertical = (double) be.getTargetY() - (double) be.getBlockPos().getY();
            double straight = Math.sqrt(horizontal * horizontal + vertical * vertical);
            return new NavigationDistances(horizontal, vertical, straight);
        }).orElse(NavigationDistances.unknown());
    }

    /** 水平距离；无目标返回 -1（与信号计算同源） */
    public static double getHorizontalDistance(BlockGetter level, BlockPos pos) {
        return getDistances(level, pos).horizontal();
    }

    /** 高度差（目标 Y − 终端 Y）；无目标返回 -1 */
    public static double getVerticalDistance(BlockGetter level, BlockPos pos) {
        return getDistances(level, pos).vertical();
    }

    /** 三维直线距离；无目标返回 -1 */
    public static double getStraightDistance(BlockGetter level, BlockPos pos) {
        return getDistances(level, pos).straight();
    }

    /** 当前输出强度 0..15 */
    public static int getSignal(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).map(NavigationTerminalBlockEntity::getRedstonePower).orElse(0);
    }

    /** 驾驶锁是否开启 */
    public static boolean isDriveLocked(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).map(NavigationTerminalBlockEntity::isDriveLocked).orElse(false);
    }

    /** 编队频道；未入队或非终端返回 0 */
    public static int getFleetChannel(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).map(NavigationTerminalBlockEntity::getFleetChannel).orElse(0);
    }

    /** 完整快照；非终端返回 {@link Optional#empty()} */
    public static Optional<TerminalSnapshot> snapshot(BlockGetter level, BlockPos pos) {
        return resolve(level, pos).map(be -> new TerminalSnapshot(
                getTarget(level, pos).orElse(null),
                getRoute(level, pos),
                be.getRouteIndex(),
                be.getMode(),
                be.getMaxPower(),
                be.getMaxRange(),
                be.getArriveRadius(),
                be.getArriveAction(),
                be.isRouteLoop(),
                be.isDriveLocked(),
                be.getFleetChannel(),
                be.getRedstonePower(),
                getDistances(level, pos)));
    }

    private static Optional<NavigationTerminalBlockEntity> resolve(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof NavigationTerminalBlockEntity term ? Optional.of(term) : Optional.empty();
    }
}
