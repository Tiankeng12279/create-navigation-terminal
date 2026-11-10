package com.hzldm.createnavigationterminal.integration.sable;

import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * SubLevel（飞行器）坐标换算桥。
 *
 * <p><b>为什么需要</b>：终端放在飞行器上时，它的 {@code worldPosition} 是
 * <b>SubLevel 局部坐标</b>，而导航目标是<b>主世界坐标</b>。直接把两者相减，
 * 方向完全是错的 —— 表现为飞行器一直朝一个方向打转。
 *
 * <p>本类用 {@code SubLevel.logicalPose()} 把局部坐标 / 局部法线投影到世界坐标系：
 * <pre>
 *   transformPosition(局部点)   -> 世界点
 *   transformNormal(局部向量)   -> 世界向量
 * </pre>
 *
 * <b>类隔离</b>：本类 import 了 {@code dev.ryanhcode.sable.*}。
 * 未安装 Sable（或没装 Aeronautics）时 JVM 解析本类会抛 {@code NoClassDefFoundError}，
 * 所以所有调用点必须先过 {@link #isAvailable()}。
 */
public final class SableTransform {

    private SableTransform() {
    }

    /** 只查 mod 列表，不触碰 sable 类型，可安全调用 */
    public static boolean isAvailable() {
        return ModList.get().isLoaded("sable");
    }

    /**
     * 姿态调试串：位置 + 四元数。用来判断「每次进存档机头都在变」到底是
     * 姿态恢复不可靠，还是载具本来就停在别的方向。
     *
     * @return 形如 {@code pos=(1.0,2.0,3.0) q=(w,x,y,z)}；不在 SubLevel 时返回 null
     */
    public static String debugPose(NavigationTerminalBlockEntity terminal) {
        SubLevel sub = containing(terminal);
        if (sub == null) {
            return null;
        }
        try {
            var pose = sub.logicalPose();
            var p = pose.position();
            var q = pose.orientation();
            return String.format(java.util.Locale.ROOT,
                    "pos=(%.2f,%.2f,%.2f) q=(%.4f,%.4f,%.4f,%.4f)",
                    p.x(), p.y(), p.z(), q.w(), q.x(), q.y(), q.z());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 终端是否位于 SubLevel（飞行器）上。
     *
     * <p>用途：距离/到达判定要选「姿态投影出的实时世界坐标」还是「座椅绑定的世界参考坐标」。
     * 后者只在玩家坐进绑定载具时更新，离开座椅后就是**陈旧值**，会导致
     * 距离不再变化 → 永远判不到到达 → 「到达后自动清除目标」不生效。
     */
    public static boolean isInSubLevel(NavigationTerminalBlockEntity terminal) {
        return containing(terminal) != null;
    }

    /** 取终端所在的 SubLevel；不在飞行器上返回 null */
    private static SubLevel containing(NavigationTerminalBlockEntity terminal) {
        if (terminal.getLevel() == null) {
            return null;
        }
        try {
            return Sable.HELPER.getContaining(terminal.getLevel(),
                    (net.minecraft.core.Vec3i) terminal.getBlockPos());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 终端中心点的<b>世界坐标</b>。
     *
     * 不在 SubLevel 上时就是普通方块中心；在 SubLevel 上时做投影。
     */
    public static Vec3 worldCenter(NavigationTerminalBlockEntity terminal) {
        Vec3 local = Vec3.atCenterOf(terminal.getBlockPos());
        SubLevel sub = containing(terminal);
        if (sub == null) {
            return local;
        }
        try {
            return sub.logicalPose().transformPosition(local);
        } catch (Throwable ignored) {
            return local;
        }
    }

    /**
     * 载具在世界坐标里的实际航向角（度，MC 的 atan2(x,z) 约定：南=0 东=90 北=180 西=-90）。
     *
     * <p><b>为什么不能用 FACING</b>：方块朝向只有 N/E/S/W 四个值，分辨率 90°，
     * 用来做转向控制会形成台阶，必然来回过冲。
     * 这里直接从 SubLevel 的姿态四元数取连续航向。
     *
     * @return 航向角；不在 SubLevel 上或出错时返回 Double.NaN（调用方应回退到 FACING）
     */
    public static double headingDegrees(NavigationTerminalBlockEntity terminal) {
        SubLevel sub = containing(terminal);
        if (sub == null) {
            return Double.NaN;
        }
        try {
            org.joml.Quaterniondc q = sub.logicalPose().orientation();
            // 把载具的局部 +Z 旋到世界，得到实际前进方向
            org.joml.Vector3d fwd = q.transform(new org.joml.Vector3d(0.0, 0.0, 1.0));
            if (Math.abs(fwd.x) < 1.0e-6 && Math.abs(fwd.z) < 1.0e-6) {
                return Double.NaN;
            }
            return Math.toDegrees(Math.atan2(fwd.x, fwd.z));
        } catch (Throwable ignored) {
            return Double.NaN;
        }
    }

    /**
     * 某个面的法线在<b>世界坐标</b>里的方向（已归一化，只保留水平分量）。
     *
     * 飞行器转向时，方块的 FACING 会跟着转，这里返回的是转完之后的世界朝向。
     */
    public static Vec3 worldNormal(NavigationTerminalBlockEntity terminal, Direction dir) {
        Vec3 local = new Vec3(dir.getStepX(), 0.0, dir.getStepZ());
        SubLevel sub = containing(terminal);
        if (sub == null) {
            return local;
        }
        try {
            Vec3 world = sub.logicalPose().transformNormal(local);
            // 只保留水平分量再归一化
            Vec3 flat = new Vec3(world.x, 0.0, world.z);
            return flat.lengthSqr() < 1.0e-6 ? local : flat.normalize();
        } catch (Throwable ignored) {
            return local;
        }
    }
}
