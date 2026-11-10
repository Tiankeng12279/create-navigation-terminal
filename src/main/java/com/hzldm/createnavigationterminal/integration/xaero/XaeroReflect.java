package com.hzldm.createnavigationterminal.integration.xaero;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Xaero's Minimap / World Map 反射封装
 *
 * 原理：
 *  - Xaero 没有官方 API（ARR 许可），只能反射访问内部类
 *  - 调用链：BuiltInHudModules.MINIMAP → HudModule#getCurrentSession()
 *            → MinimapSession#getWorldManager() → MinimapWorldManager#getCurrentWorld()
 *            → MinimapWorld#getCurrentWaypointSet() → WaypointSet#getWaypoints()
 *  - 版本升级可能改包名/改方法名，因此每处访问都做「先方法、后字段」的多候选兜底，
 *    失败只警告一次（避免日志刷屏），调用方按空结果降级
 *  - Xaero 不存在时全部静默返回 null / 空列表，不影响本模组其它功能
 */
public final class XaeroReflect {

    private static final Logger LOGGER = LoggerFactory.getLogger("NavTerminal-Xaero");

    private static volatile Boolean xaeroPresent = null;
    private static final AtomicBoolean WARNED = new AtomicBoolean(false);

    private XaeroReflect() {
    }

    /** 检测 Xaero 是否存在（结果缓存） */
    public static boolean isXaeroPresent() {
        Boolean cached = xaeroPresent;
        if (cached != null) {
            return cached;
        }
        boolean present;
        try {
            Class.forName("xaero.hud.minimap.BuiltInHudModules");
            present = true;
        } catch (Throwable t) {
            present = false;
        }
        xaeroPresent = present;
        return present;
    }

    // ------------------------------------------------------------------
    // 会话链路
    // ------------------------------------------------------------------

    /** 取当前 MinimapWorld（玩家所在维度），失败返回 null */
    public static Object getCurrentMinimapWorld() {
        if (!isXaeroPresent()) {
            return null;
        }
        try {
            Class<?> builtInCls = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            Field minimapField = builtInCls.getField("MINIMAP");
            Object minimapModule = minimapField.get(null);
            if (minimapModule == null) {
                return null;
            }
            Class<?> hudModuleCls = Class.forName("xaero.hud.module.HudModule");
            Object session = hudModuleCls.getMethod("getCurrentSession").invoke(minimapModule);
            if (session == null) {
                return null;
            }
            Class<?> sessionCls = Class.forName("xaero.hud.minimap.module.MinimapSession");
            Object worldManager = sessionCls.getMethod("getWorldManager").invoke(session);
            if (worldManager == null) {
                return null;
            }
            Class<?> wmCls = Class.forName("xaero.hud.minimap.world.MinimapWorldManager");
            return wmCls.getMethod("getCurrentWorld").invoke(worldManager);
        } catch (Throwable t) {
            warnOnce("getCurrentMinimapWorld", t);
            return null;
        }
    }

    /** 从 MinimapWorld 取当前 WaypointSet */
    public static Object getCurrentWaypointSet(Object minimapWorld) {
        if (minimapWorld == null) {
            return null;
        }
        try {
            Class<?> worldCls = Class.forName("xaero.hud.minimap.world.MinimapWorld");
            return worldCls.getMethod("getCurrentWaypointSet").invoke(minimapWorld);
        } catch (Throwable t) {
            warnOnce("getCurrentWaypointSet", t);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 读取通道：路点 → 纯数据快照
    // ------------------------------------------------------------------

    /**
     * 路点快照（纯 Java 数据）
     *
     * 为什么要转存：Xaero 的 Waypoint 是内部对象，不应跨线程/跨帧持有。
     * 一次性读出名称与坐标，之后全部按纯数据使用。
     */
    public record RawWp(String name, int x, int y, int z) {}

    /**
     * 列出 WaypointSet 里全部路点
     *
     * - 临时路点（死亡点等）不导出：那是临时标记，不该变成常驻导航目标
     * - 名称/坐标访问器跨版本可能改名，逐项「方法 → 字段」兜底
     *
     * @return 只读列表；Xaero 缺失或反射漂移时为空列表
     */
    public static java.util.List<RawWp> listWaypoints(Object waypointSet) {
        java.util.List<RawWp> out = new java.util.ArrayList<>();
        if (waypointSet == null || !isXaeroPresent()) {
            return out;
        }
        try {
            Class<?> setCls = Class.forName("xaero.hud.minimap.waypoint.set.WaypointSet");
            Class<?> wpCls = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Object iterable = setCls.getMethod("getWaypoints").invoke(waypointSet);
            if (!(iterable instanceof Iterable<?> list)) {
                return out;
            }
            for (Object wp : list) {
                if (wp == null) {
                    continue;
                }
                String name = getWaypointName(wp, wpCls);
                if (name == null || name.isEmpty()) {
                    continue;
                }
                if (isTemporary(wp, wpCls)) {
                    continue;
                }
                out.add(new RawWp(name,
                        readIntMember(wp, wpCls, "getX", "x"),
                        readIntMember(wp, wpCls, "getY", "y"),
                        readIntMember(wp, wpCls, "getZ", "z")));
            }
        } catch (Throwable t) {
            warnOnce("listWaypoints", t);
        }
        return out;
    }

    /** 临时路点判定：多候选方法名，任一返回 true 即认（无该方法视为非临时） */
    private static boolean isTemporary(Object wp, Class<?> wpCls) {
        for (String m : new String[]{"isTemporary", "getTemporary", "isTemp"}) {
            try {
                Object v = wpCls.getMethod(m).invoke(wp);
                if (v instanceof Boolean b && b) {
                    return true;
                }
            } catch (Throwable ignored) {
                // 该候选不存在，继续
            }
        }
        return false;
    }

    /** 整数成员读取：先方法、再同名字段，都失败返回 0 */
    private static int readIntMember(Object wp, Class<?> wpCls, String getter, String field) {
        try {
            Object v = wpCls.getMethod(getter).invoke(wp);
            if (v instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
            // 落到字段
        }
        try {
            return wpCls.getField(field).getInt(wp);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** 内部辅助：安全获取 Waypoint 的 name（优先 getName()，失败用字段） */
    private static String getWaypointName(Object wp, Class<?> wpCls) {
        if (wp == null) {
            return null;
        }
        try {
            return (String) wpCls.getMethod("getName").invoke(wp);
        } catch (Throwable ignored) {
            // 落到字段
        }
        try {
            Object v = wpCls.getField("name").get(wp);
            return v instanceof String s ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // 说明：本类**只做只读反射**。早期曾实现「把目标回写成 Xaero 路点」
    // （addWaypoint + MinimapWorldManagerIO#saveWorld，需要 setAccessible 读私有字段），
    // 该功能没有任何 UI 入口（无调用方），且与「只读消费」的对外声明冲突，已整段移除。

    private static void warnOnce(String operation, Throwable cause) {
        if (WARNED.compareAndSet(false, true)) {
            LOGGER.warn("Xaero API 漂移: {} ({})", operation, cause.toString());
        }
    }
}
