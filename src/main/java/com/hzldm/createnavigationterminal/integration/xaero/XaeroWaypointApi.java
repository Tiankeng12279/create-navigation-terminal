package com.hzldm.createnavigationterminal.integration.xaero;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 直接调用 Xaero Minimap 的路点 API（编译期依赖，非反射）。
 *
 * <b>类隔离警告</b>：这个类是唯一 import 了 {@code xaero.*} 的地方。
 * 未安装 Xaero 时 JVM 一旦解析本类的常量池就会抛 {@code NoClassDefFoundError}，
 * 所以<b>所有</b>调用点必须先检查 {@link #isAvailable()}，再触发本类加载。
 *
 * 调用链（实测于 Xaero's Minimap 26.5.0）：
 * <pre>
 * XaeroMinimapSession.getCurrentSession()
 *   .getWaypointsManager()      -> WaypointsManager
 *   .getCurrentWorld()          -> WaypointWorld
 *   .getCurrentSet()            -> WaypointSet
 *   .getList()                  -> ArrayList&lt;Waypoint&gt;
 *       wp.getX() / getY() / getZ() / getName()
 * </pre>
 */
public final class XaeroWaypointApi {

    private XaeroWaypointApi() {
    }

    /**
     * 判断 Xaero Minimap 是否可用。
     *
     * 只查 mod 列表，不触碰任何 {@code xaero.*} 类型，可安全调用。
     */
    public static boolean isAvailable() {
        boolean mod = ModList.get().isLoaded("xaerominimap");
        Minecraft mc = Minecraft.getInstance();
        boolean ready = mc != null && mc.player != null && mc.level != null;
        return mod && ready;
    }

    /**
     * 取当前世界的全部路点。
     *
     * 只能在 {@link #isAvailable()} 为 true 时调用。
     */
    public static List<XaeroReflect.RawWp> listWaypoints() {
        List<XaeroReflect.RawWp> out = new ArrayList<>();
        try {
            XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
            if (session == null) {
                return out;
            }
            WaypointsManager manager = session.getWaypointsManager();
            if (manager == null) {
                return out;
            }
            WaypointWorld world = manager.getCurrentWorld();
            if (world == null) {
                return out;
            }
            WaypointSet set = world.getCurrentSet();
            if (set == null) {
                return out;
            }
            List<Waypoint> list = set.getList();
            if (list == null) {
                return out;
            }
            for (Waypoint wp : list) {
                if (wp == null) {
                    continue;
                }
                String name = wp.getName();
                if (name == null || name.isEmpty()) {
                    name = "waypoint";
                }
                out.add(new XaeroReflect.RawWp(name, wp.getX(), wp.getY(), wp.getZ()));
            }
        } catch (Throwable t) {
            // Xaero 内部状态异常时返回已收集到的部分，但把原因说出来（否则界面只是"空列表"）
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.warn(
                    "[Xaero] listWaypoints 异常：{}", t.toString());
        }
        return out;
    }

    /**
     * 把路点列表交给回调（兼容旧反射版 {@code listWaypointsOnClient(Consumer)} 的调用形态）。
     *
     * 实际上同步执行，只是为了让现有调用点不用改写。
     */
    public static void listWaypointsOnClient(
            java.util.function.Consumer<List<XaeroReflect.RawWp>> callback) {
        if (callback == null) {
            return;
        }
        if (!isAvailable()) {
            callback.accept(List.of());
            return;
        }
        callback.accept(listWaypoints());
    }
}
