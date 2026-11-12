package com.hzldm.createnavigationterminal.api;

import java.util.List;

/**
 * 某台导航终端在某时刻的只读快照。
 *
 * @param target       当前目标，可为 null
 * @param route        当前航线（不可变）
 * @param routeIndex   当前站序号，-1 表示无
 * @param mode         0=距离渐变 1=到达开关 2=固定强度
 * @param maxPower     强度上限 0..15
 * @param maxRange     覆盖距离 8..512
 * @param arriveRadius 到达半径 1..64
 * @param arriveAction 到达动作（内部枚举序号）
 * @param routeLoop    航线是否循环
 * @param driveLocked  驾驶锁是否开启
 * @param fleetChannel 编队频道，0 表示未入队
 * @param signal       当前输出强度 0..15
 * @param distances    距离信息
 */
public record TerminalSnapshot(
        NavigationTarget target,
        List<NavigationTarget> route,
        int routeIndex,
        int mode,
        int maxPower,
        int maxRange,
        int arriveRadius,
        int arriveAction,
        boolean routeLoop,
        boolean driveLocked,
        int fleetChannel,
        int signal,
        NavigationDistances distances
) {
    public TerminalSnapshot {
        route = List.copyOf(route);
    }

    public boolean hasTarget() {
        return target != null;
    }
}
