package com.hzldm.createnavigationterminal.api;

/**
 * 终端到目标的距离（单位：格）。
 *
 * <p>导航信号只使用 {@link #horizontal()}；{@link #vertical()} 与 {@link #straight()}
 * 供 HUD、载具姿态等外部逻辑参考。
 *
 * @param horizontal 水平距离（只算 X/Z），无目标或跨维度为 -1
 * @param vertical   高度差（目标 Y − 终端 Y），无目标或跨维度为 -1
 * @param straight   三维直线距离，无目标或跨维度为 -1
 */
public record NavigationDistances(double horizontal, double vertical, double straight) {

    private static final NavigationDistances UNKNOWN = new NavigationDistances(-1, -1, -1);

    /** 无目标 / 跨维度 / 未加载 */
    public static NavigationDistances unknown() {
        return UNKNOWN;
    }

    /** 是否有有效数据 */
    public boolean isKnown() {
        return horizontal >= 0;
    }

    /** 目标是否在终端上方 */
    public boolean isTargetAbove() {
        return isKnown() && vertical > 0;
    }
}
