package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.network.NavigationHudPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 导航 HUD（战斗机式）
 *
 * 只在玩家正坐在「绑定座椅」上时显示。
 * 视觉：全息绿、无底板、中央准星、顶部航向刻度、目标标记 + 距离。
 */
@EventBusSubscriber(modid = com.hzldm.createnavigationterminal.CreateNavigationTerminal.MODID, value = Dist.CLIENT)
public final class NavigationHudOverlay {

    /** 主绿色（全息 HUD 绿）—— 由配置覆盖 */
    private static int HUD_GREEN = 0xFF3CFF5A;
    /** 暗绿（刻度/次要信息） */
    private static int HUD_DIM = 0xB028C040;
    /** 告警色（跨维度/无数据） */
    private static final int HUD_WARN = 0xFF60A0FF;

    /** 以下均由配置文件覆盖 */
    private static double REL_OFFSET_DEG = 0.0;

    /** 本帧已绘制的标记坐标（"x,z"），各绘制方法共享去重，避免同一位置画两遍 */
    /** 本帧已画过的坐标（long 打包 x/z，避免每帧拼 String） */
    private static final it.unimi.dsi.fastutil.longs.LongOpenHashSet DRAWN_KEYS =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /** 路点绘制上限（0 = 不限） */
    private static int MAX_OTHER_WAYPOINTS = 20;

    /** 把世界坐标打包成 long，用于去重 */
    private static long keyXZ(double x, double z) {
        return ((long) (int) Math.floor(x) << 32) ^ ((int) Math.floor(z) & 0xFFFFFFFFL);
    }
    /** 水平映射半视角（度）：标记贴边阈值 */
    private static double FOV_HALF = 100.0;
    private static int TAPE_Y = 46;
    private static int DIST_SCALE_BOTTOM = 82;
    private static int MARKER_Y_OFFSET = -6;
    /** 准星半长（像素） */
    private static int CROSSHAIR_SIZE = 10;
    private static boolean SHOW_DEBUG = false;
    private static boolean SHOW_RADAR = true;
    private static boolean SHOW_SPATIAL_MARKER = true;
    /** 是否显示航线其他站点 */
    private static boolean SHOW_OTHER_WAYPOINTS = true;
    /** 是否显示 Xaero 全部路点 */
    private static boolean SHOW_ALL_WAYPOINTS = true;
    /** 其他路点颜色 */
    private static int HUD_OTHER = 0xFFFFA500;

    /** 下一个航点：青色，与当前站（绿）、其他站点（橙）区分。 */
    private static int HUD_NEXT = 0xFF40E0FF;

    /** 航线起点：紫色，只在它既不是当前站也不是下一站时出现。 */
    private static int HUD_START = 0xFFC060FF;
    /** 隐藏的路点名（逗号分隔，小写比较） */
    private static String HIDDEN_WAYPOINTS = "";
    /** 是否显示海平面指示线 */
    private static boolean SHOW_HORIZON = true;
    /** 是否显示编队标记（附近其他载具） */
    private static boolean SHOW_FLEET = true;
    /** 编队标记颜色 */
    private static int HUD_FLEET = 0xFF4FA3FF;
    /** 是否显示信标（只有铭牌没有终端） */
    private static boolean SHOW_BEACONS = true;
    /** 信标颜色 */
    private static int HUD_BEACON = 0xFF00E5FF;
    /** 编队探测半径（格） */
    private static double FLEET_RANGE = 128.0;
    /** 是否要求佩戴护目镜才显示 HUD */
    private static boolean REQUIRE_GOGGLES = true;

    /**
     * 检查玩家是否佩戴护目镜。
     *
     * 兼容：Create 护目镜（头盔槽）、Curios 饰品槽、以及任何 id 含 "goggles" 的物品。
     */
    private static boolean hasGoggles(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return false;
        }
        // 1. 头盔槽
        try {
            net.minecraft.world.item.ItemStack head =
                    player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (isGogglesItem(head)) {
                return true;
            }
        } catch (Throwable ignored) {
            // 继续
        }

        // 2. Curios 饰品槽（反射，未安装时静默跳过）
        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var getCurios = curiosApi.getMethod("getCuriosInventory",
                    net.minecraft.world.entity.LivingEntity.class);
            Object lazy = getCurios.invoke(null, player);
            if (lazy != null) {
                var opt = lazy.getClass().getMethod("resolve").invoke(lazy);
                if (opt instanceof java.util.Optional<?> o && o.isPresent()) {
                    Object handler = o.get();
                    var findFirst = handler.getClass().getMethod("findFirstCurio",
                            java.util.function.Predicate.class);
                    Object result = findFirst.invoke(handler,
                            (java.util.function.Predicate<net.minecraft.world.item.ItemStack>)
                                    NavigationHudOverlay::isGogglesItem);
                    if (result instanceof java.util.Optional<?> r && r.isPresent()) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // 没装 Curios 或 API 变了，忽略
        }

        return false;
    }

    /** 物品是否是护目镜类（id 含 goggles） */
    private static boolean isGogglesItem(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var key = stack.getItem().builtInRegistryHolder().key().location();
        return key.getPath().contains("goggles");
    }

    /** Xaero 路点缓存 */
    private static volatile java.util.List<com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp>
            cachedWaypoints = java.util.List.of();
    private static long lastWpRefresh = 0L;

    /** 该路点是否被隐藏 */
    private static boolean isWaypointHidden(String name) {
        if (name == null || HIDDEN_WAYPOINTS == null || HIDDEN_WAYPOINTS.isBlank()) {
            return false;
        }
        for (String s : HIDDEN_WAYPOINTS.split(",")) {
            String t = s.trim();
            if (!t.isEmpty() && t.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** 供 GUI 查询：当前缓存的路点 */
    public static java.util.List<com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp>
            getCachedWaypoints() {
        return cachedWaypoints;
    }

    /** 供 GUI 查询：该路点是否被隐藏 */
    public static boolean isHiddenWaypoint(String name) {
        return isWaypointHidden(name);
    }

    /** 供按键使用：当前 HUD 数据 */
    public static NavigationHudPayload getCurrent() {
        return current;
    }

    /**
     * 返回准星正对的路点（相对角度 12° 以内且最近），没有则返回 null。
     * 忽略已隐藏的路点与当前导航目标。
     */
    public static com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp getAimedWaypoint() {
        NavigationHudPayload data = current;
        if (data == null || cachedWaypoints.isEmpty()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }

        // 必须和 HUD 绘制用同一套参考：相机位置 + 相机 yaw + relOffsetDeg 校准
        float heading = mc.player.getViewYRot(1.0f);
        double camX = mc.player.getX();
        double camZ = mc.player.getZ();
        try {
            var cam = mc.gameRenderer.getMainCamera();
            if (cam != null && cam.isInitialized()) {
                var cp = cam.getPosition();
                camX = cp.x;
                camZ = cp.z;
                heading = cam.getYRot();
            }
        } catch (Throwable ignored) {
            // 退回玩家视角
        }

        boolean hasTarget = data.targetName() != null && !data.targetName().isEmpty();

        com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp best = null;
        double bestAbs = 12.0;
        for (var wp : cachedWaypoints) {
            if (isWaypointHidden(wp.name())) {
                continue;
            }
            // 有目标时才跳过「当前目标」本身；没目标时全部候选
            if (hasTarget
                    && Math.abs(wp.x() - data.targetX()) <= 2
                    && Math.abs(wp.z() - data.targetZ()) <= 2) {
                continue;
            }
            double dx = wp.x() - camX;
            double dz = wp.z() - camZ;
            if (dx * dx + dz * dz < 4.0) {
                continue;
            }
            double tYaw = Math.toDegrees(Math.atan2(-dx, dz));
            // 与绘制保持完全一致：加上方位校准量
            double abs = Math.abs(normalizeDeg(tYaw - heading + REL_OFFSET_DEG));
            if (abs < bestAbs) {
                bestAbs = abs;
                best = wp;
            }
        }

        // 编队里的其他载具也可以直接当导航点
        var fleetPos = data.fleetTerminals();
        var fleetNm = data.fleetNames();
        if (fleetPos != null) {
            for (int i = 0; i < fleetPos.size(); i++) {
                int[] f = fleetPos.get(i);
                double abs = aimAngle(camX, camZ, f[0], f[2], heading);
                if (abs < bestAbs) {
                    String nm = (fleetNm != null && i < fleetNm.size() && !fleetNm.get(i).isBlank())
                            ? fleetNm.get(i) : "编队";
                    bestAbs = abs;
                    best = new com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp(
                            nm, f[0], f[1], f[2]);
                }
            }
        }

        // 信标同样可以当导航点
        var bcnPos = data.beaconPositions();
        var bcnNm = data.beaconNames();
        if (bcnPos != null) {
            for (int i = 0; i < bcnPos.size(); i++) {
                int[] b = bcnPos.get(i);
                double abs = aimAngle(camX, camZ, b[0], b[2], heading);
                if (abs < bestAbs) {
                    String nm = (bcnNm != null && i < bcnNm.size() && !bcnNm.get(i).isBlank())
                            ? bcnNm.get(i) : "信标";
                    bestAbs = abs;
                    best = new com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp(
                            nm, b[0], b[1], b[2]);
                }
            }
        }

        return best;
    }

    /** 目标相对准星的偏角（度，绝对值） */
    private static double aimAngle(double camX, double camZ, int tx, int tz, float heading) {
        double dx = tx - camX;
        double dz = tz - camZ;
        if (dx * dx + dz * dz < 4.0) {
            return 999.0;
        }
        double tYaw = Math.toDegrees(Math.atan2(-dx, dz));
        return Math.abs(normalizeDeg(tYaw - heading));
    }

    /** 供 GUI 调用：强制刷新路点缓存（打开界面时用） */
    public static void refreshWaypointsNow() {
        lastWpRefresh = 0L;
        refreshWaypointCache();
    }
    private static String MARKER_STYLE = "box";
    private static double SMOOTHING = 0.78;
    private static double DEAD_ZONE = 3.0;

    /** 标记位置平滑状态（NaN = 未初始化） */
    private static double smoothPx = Double.NaN;
    private static double smoothPy = Double.NaN;
    /** 角度平滑状态与系数 */
    private static double smoothRel = Double.NaN;
    private static double smoothRelPitch = Double.NaN;
    private static final double ANGLE_SMOOTH = 0.22;

    /** 配置读取节流（配置不需要每帧重读） */
    private static long lastConfigReload = 0L;

    /** 校准 GUI 打开时暂停从配置读取，让改动实时生效 */
    private static volatile boolean calibrationActive = false;

    // ---------------- 校准接口（供 HudCalibrationScreen 调用） ----------------

    public static void beginCalibration() {
        calibrationActive = true;
    }

    public static void endCalibration() {
        calibrationActive = false;
        lastConfigReload = 0L;
    }

    public static double getRelOffset() {
        return REL_OFFSET_DEG;
    }

    public static void setRelOffset(double v) {
        REL_OFFSET_DEG = v;
    }

    public static double getFovHalf() {
        return FOV_HALF;
    }

    public static void setFovHalf(double v) {
        FOV_HALF = v;
    }

    public static double getSmoothing() {
        return SMOOTHING;
    }

    public static void setSmoothing(double v) {
        SMOOTHING = v;
    }

    public static double getDeadZone() {
        return DEAD_ZONE;
    }

    public static void setDeadZone(double v) {
        DEAD_ZONE = v;
    }

    // ---- 其余可调项 ----

    public static double getTapeY() {
        return TAPE_Y;
    }

    public static void setTapeY(double v) {
        TAPE_Y = (int) Math.round(v);
    }

    public static double getDistScaleBottom() {
        return DIST_SCALE_BOTTOM;
    }

    public static void setDistScaleBottom(double v) {
        DIST_SCALE_BOTTOM = (int) Math.round(v);
    }

    public static double getMarkerYOffset() {
        return MARKER_Y_OFFSET;
    }

    public static void setMarkerYOffset(double v) {
        MARKER_Y_OFFSET = (int) Math.round(v);
    }

    public static double getCrosshairSize() {
        return CROSSHAIR_SIZE;
    }

    public static void setCrosshairSize(double v) {
        CROSSHAIR_SIZE = (int) Math.round(v);
    }

    public static String getMarkerStyle() {
        return MARKER_STYLE;
    }

    public static void setMarkerStyle(String v) {
        MARKER_STYLE = v;
    }

    public static boolean isShowRadar() {
        return SHOW_RADAR;
    }

    public static void setShowRadar(boolean v) {
        SHOW_RADAR = v;
    }

    public static boolean isShowSpatialMarker() {
        return SHOW_SPATIAL_MARKER;
    }

    public static void setShowSpatialMarker(boolean v) {
        SHOW_SPATIAL_MARKER = v;
    }

    public static boolean isShowOtherWaypoints() {
        return SHOW_OTHER_WAYPOINTS;
    }

    public static void setShowOtherWaypoints(boolean v) {
        SHOW_OTHER_WAYPOINTS = v;
    }

    public static boolean isShowAllWaypoints() {
        return SHOW_ALL_WAYPOINTS;
    }

    public static void setShowAllWaypoints(boolean v) {
        SHOW_ALL_WAYPOINTS = v;
    }

    public static boolean isShowHorizon() {
        return SHOW_HORIZON;
    }

    public static void setShowHorizon(boolean v) {
        SHOW_HORIZON = v;
    }

    public static String getHiddenWaypoints() {
        return HIDDEN_WAYPOINTS == null ? "" : HIDDEN_WAYPOINTS;
    }

    public static void setHiddenWaypoints(String v) {
        HIDDEN_WAYPOINTS = v == null ? "" : v;
    }

    public static String getColorOtherHex() {
        return String.format("%06X", HUD_OTHER & 0xFFFFFF);
    }

    public static boolean isShowFleet() {
        return SHOW_FLEET;
    }

    public static void setShowFleet(boolean v) {
        SHOW_FLEET = v;
    }

    public static String getColorFleetHex() {
        return String.format("%06X", HUD_FLEET & 0xFFFFFF);
    }

    public static void setColorFleetHex(String hex) {
        HUD_FLEET = 0xFF000000 | parseColor(hex, 0x4FA3FF);
    }

    public static boolean isShowBeacons() {
        return SHOW_BEACONS;
    }

    public static void setShowBeacons(boolean v) {
        SHOW_BEACONS = v;
    }

    public static String getColorBeaconHex() {
        return String.format("%06X", HUD_BEACON & 0xFFFFFF);
    }

    public static void setColorBeaconHex(String hex) {
        HUD_BEACON = 0xFF000000 | parseColor(hex, 0x00E5FF);
    }

    public static void setColorOtherHex(String hex) {
        HUD_OTHER = 0xFF000000 | parseColor(hex, 0xFFA500);
    }

    public static boolean isShowDebug() {
        return SHOW_DEBUG;
    }

    public static void setShowDebug(boolean v) {
        SHOW_DEBUG = v;
    }

    public static String getColorMainHex() {
        return String.format("%06X", HUD_GREEN & 0xFFFFFF);
    }

    public static void setColorMainHex(String hex) {
        HUD_GREEN = 0xFF000000 | parseColor(hex, 0x3CFF5A);
    }

    public static String getColorDimHex() {
        return String.format("%06X", HUD_DIM & 0xFFFFFF);
    }

    public static void setColorDimHex(String hex) {
        HUD_DIM = withAlpha(parseColor(hex, 0x28C040), 0xB0);
    }

    /** 把当前校准值写回配置文件 */
    public static void saveCalibration() {
        try {
            var c = com.hzldm.createnavigationterminal.config.ClientConfig.class;
            com.hzldm.createnavigationterminal.config.ClientConfig.REL_OFFSET_DEG.set(REL_OFFSET_DEG);
            com.hzldm.createnavigationterminal.config.ClientConfig.FOV_HALF_DEG.set(FOV_HALF);
            com.hzldm.createnavigationterminal.config.ClientConfig.SMOOTHING.set(SMOOTHING);
            com.hzldm.createnavigationterminal.config.ClientConfig.DEAD_ZONE.set(DEAD_ZONE);
            com.hzldm.createnavigationterminal.config.ClientConfig.TAPE_Y.set(TAPE_Y);
            com.hzldm.createnavigationterminal.config.ClientConfig.DIST_SCALE_BOTTOM.set(DIST_SCALE_BOTTOM);
            com.hzldm.createnavigationterminal.config.ClientConfig.MARKER_Y_OFFSET.set(MARKER_Y_OFFSET);
            com.hzldm.createnavigationterminal.config.ClientConfig.CROSSHAIR_SIZE.set(CROSSHAIR_SIZE);
            com.hzldm.createnavigationterminal.config.ClientConfig.MARKER_STYLE.set(MARKER_STYLE);
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_RADAR.set(SHOW_RADAR);
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_SPATIAL_MARKER.set(SHOW_SPATIAL_MARKER);
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_OTHER_WAYPOINTS.set(SHOW_OTHER_WAYPOINTS);
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_ALL_WAYPOINTS.set(SHOW_ALL_WAYPOINTS);
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_HORIZON.set(SHOW_HORIZON);
            com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_OTHER.set(getColorOtherHex());
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_FLEET.set(SHOW_FLEET);
            com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_FLEET.set(getColorFleetHex());
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_BEACONS.set(SHOW_BEACONS);
            com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_BEACON.set(getColorBeaconHex());
            com.hzldm.createnavigationterminal.config.ClientConfig.HIDDEN_WAYPOINTS.set(getHiddenWaypoints());
            com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_DEBUG.set(SHOW_DEBUG);
            com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_MAIN.set(getColorMainHex());
            com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_DIM.set(getColorDimHex());
            com.hzldm.createnavigationterminal.config.ClientConfig.SPEC.save();
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[HUD] 配置已保存：rel={} fovHalf={} smooth={} dead={} tapeY={} scaleY={} markerY={} style={}",
                    REL_OFFSET_DEG, FOV_HALF, SMOOTHING, DEAD_ZONE,
                    TAPE_Y, DIST_SCALE_BOTTOM, MARKER_Y_OFFSET, MARKER_STYLE);
        } catch (Throwable t) {
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.warn(
                    "[HUD] 配置保存失败", t);
        }
    }

    /** 从配置重新加载（校准 GUI 重置用） */
    public static void forceReload() {
        lastConfigReload = 0L;
    }

    /** 从配置刷新（带节流，避免每帧开销） */
    private static void reloadConfig() {
        if (calibrationActive) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastConfigReload < 500L) {
            return;
        }
        lastConfigReload = now;
        try {
            HUD_GREEN = parseColor(
                    com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_MAIN.get(), 0xFF3CFF5A);
            HUD_DIM = withAlpha(parseColor(
                    com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_DIM.get(), 0x28C040), 0xB0);
            MARKER_STYLE = com.hzldm.createnavigationterminal.config.ClientConfig.MARKER_STYLE.get();
            SHOW_RADAR = com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_RADAR.get();
            SHOW_SPATIAL_MARKER =
                    com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_SPATIAL_MARKER.get();
            SHOW_OTHER_WAYPOINTS =
                    com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_OTHER_WAYPOINTS.get();
            MAX_OTHER_WAYPOINTS =
                    com.hzldm.createnavigationterminal.config.ClientConfig.MAX_RENDERED_WAYPOINTS.get();
            SHOW_ALL_WAYPOINTS =
                    com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_ALL_WAYPOINTS.get();
            HUD_OTHER = 0xFF000000 | parseColor(
                    com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_OTHER.get(), 0xFFA500);
            HIDDEN_WAYPOINTS =
                    com.hzldm.createnavigationterminal.config.ClientConfig.HIDDEN_WAYPOINTS.get();
            SHOW_HORIZON =
                    com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_HORIZON.get();
            SHOW_FLEET = com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_FLEET.get();
            HUD_FLEET = 0xFF000000 | parseColor(
                    com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_FLEET.get(), 0x4FA3FF);
            SHOW_BEACONS =
                    com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_BEACONS.get();
            HUD_BEACON = 0xFF000000 | parseColor(
                    com.hzldm.createnavigationterminal.config.ClientConfig.COLOR_BEACON.get(), 0x00E5FF);
            REQUIRE_GOGGLES =
                    com.hzldm.createnavigationterminal.config.ClientConfig.REQUIRE_GOGGLES.get();
            SHOW_DEBUG = com.hzldm.createnavigationterminal.config.ClientConfig.SHOW_DEBUG.get();
            REL_OFFSET_DEG = com.hzldm.createnavigationterminal.config.ClientConfig.REL_OFFSET_DEG.get();
            FOV_HALF = com.hzldm.createnavigationterminal.config.ClientConfig.FOV_HALF_DEG.get();
            SMOOTHING = com.hzldm.createnavigationterminal.config.ClientConfig.SMOOTHING.get();
            DEAD_ZONE = com.hzldm.createnavigationterminal.config.ClientConfig.DEAD_ZONE.get();
            TAPE_Y = com.hzldm.createnavigationterminal.config.ClientConfig.TAPE_Y.get();
            DIST_SCALE_BOTTOM = com.hzldm.createnavigationterminal.config.ClientConfig.DIST_SCALE_BOTTOM.get();
            MARKER_Y_OFFSET = com.hzldm.createnavigationterminal.config.ClientConfig.MARKER_Y_OFFSET.get();
            CROSSHAIR_SIZE = com.hzldm.createnavigationterminal.config.ClientConfig.CROSSHAIR_SIZE.get();
        } catch (Throwable ignored) {
            // 配置未就绪时保留默认值
        }
    }

    private static int parseColor(String hex, int fallback) {
        try {
            String s = hex == null ? "" : hex.trim();
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            return 0xFF000000 | Integer.parseInt(s, 16);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** 客户端缓存的最新 HUD 数据（null = 无） */
    private static volatile NavigationHudPayload current = null;

    /** 世界渲染阶段缓存下来的真实投影矩阵（GUI 阶段拿不到） */
    private static volatile org.joml.Matrix4f cachedProjection = null;

    /**
     * 世界渲染阶段：只缓存投影矩阵。
     *
     * PoseStack 在 AFTER_PARTICLES 阶段内容不可靠（不是纯相机视图矩阵，
     * 实测会让 w 变成负数），所以视图矩阵改由相机自己构造。
     */
    @SubscribeEvent
    public static void onRenderLevel(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        try {
            org.joml.Matrix4f p = event.getProjectionMatrix();
            if (p != null) {
                cachedProjection = new org.joml.Matrix4f(p);
            }
        } catch (Throwable ignored) {
            // 保持上一帧的缓存
        }
    }

    /** 调试日志限流（毫秒） */
    private static long lastDebugLog = 0L;

    private NavigationHudOverlay() {
    }

    public static void accept(NavigationHudPayload payload) {
        NavigationHudPayload before = current;
        current = payload != null && payload.active() ? payload : null;
        if (before == null && current != null) {
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[HUD] 客户端收到 active：target={} pos=({},{},{})",
                    current.targetName(), current.targetX(), current.targetY(), current.targetZ());
        } else if (before != null && current == null) {
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[HUD] 客户端收到 inactive：HUD 清空");
        }
    }

    public static void clear() {
        current = null;
    }

    /**
     * 世界切换时清空全部客户端静态缓存。
     *
     * <p>否则换存档后 HUD 仍握着旧世界刷新来的路点列表、去重键、车道占位与距离文案缓存，
     * 表现就是「换了存档导航仪不动了」。
     */
    public static void clearAllCaches() {
        current = null;
        cachedWaypoints = java.util.List.of();
        lastWpRefresh = 0L;
        DRAWN_KEYS.clear();
        clearMarkerSlots();
        DIST_LABEL_CACHE.clear();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        long t0 = System.nanoTime();
        try {
            drawHud(event);
        } finally {
            ClientPerfProbe.HUD_NANOS.addAndGet(System.nanoTime() - t0);
            ClientPerfProbe.HUD_FRAMES.incrementAndGet();
        }
    }

    private static void drawHud(RenderGuiEvent.Post event) {
        NavigationHudPayload data = current;
        if (data == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        // 打开任何界面（地图/背包/终端）时隐藏 HUD，避免叠在 GUI 上
        if (mc.screen != null) {
            return;
        }
        // 只有佩戴护目镜的玩家才看得到 HUD
        if (REQUIRE_GOGGLES && !hasGoggles(mc.player)) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        reloadConfig();
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = h / 2;

        // ---------------- 中央准星 ----------------
        int cs = Math.max(3, CROSSHAIR_SIZE);
        int outer = cs + Math.max(4, cs / 2);
        int inner = cs + 2;
        g.fill(cx - cs, cy, cx + cs + 1, cy + 1, HUD_GREEN);
        g.fill(cx, cy - cs, cx + 1, cy + cs + 1, HUD_GREEN);
        g.fill(cx - outer, cy, cx - inner, cy + 1, HUD_DIM);
        g.fill(cx + inner + 1, cy, cx + outer + 1, cy + 1, HUD_DIM);
        g.fill(cx, cy - outer, cx + 1, cy - inner, HUD_DIM);
        g.fill(cx, cy + inner + 1, cx + 1, cy + outer + 1, HUD_DIM);

        // ---------------- 顶部航向刻度 ----------------
        drawHeadingTape(g, mc, cx, h, data);

        // ---------------- 海平面（地平线）指示 ----------------
        if (SHOW_HORIZON) {
            drawHorizonLine(g, mc, w, h, cx, cy);
        }

        // 未设置导航目标：仍然显示其他路点（橙色），只是不画主目标
        boolean hasTarget = data.targetName() != null && !data.targetName().isEmpty();
        if (!hasTarget) {
            g.drawCenteredString(mc.font, "未设置导航目标", cx, cy + 34, HUD_DIM);

            if (SHOW_ALL_WAYPOINTS || SHOW_OTHER_WAYPOINTS) {
                refreshWaypointCache();
                float hd = mc.player.getViewYRot(1.0f);
                double cxp = mc.player.getX();
                double czp = mc.player.getZ();
                try {
                    var cam0 = mc.gameRenderer.getMainCamera();
                    if (cam0 != null && cam0.isInitialized()) {
                        hd = cam0.getYRot();
                        cxp = cam0.getPosition().x;
                        czp = cam0.getPosition().z;
                    }
                } catch (Throwable ignored) {
                    // 用玩家视角
                }
                drawOtherWaypoints(g, mc, cx, cy, w, hd, cxp, czp, 0, 0);
                drawFleetMarkers(g, mc, cx, cy, w, hd, cxp, czp);
                drawBeacons(g, mc, cx, cy, w, hd, cxp, czp);
            }

            if (SHOW_RADAR) {
                drawRadar(g, mc, cx, cy, 0, 0, 0, false, "", false);
            }
            return;
        }

        // ---------------- 目标 ----------------
        boolean crossDim = !mc.player.level().dimension().location().toString().equals(data.targetDim());

        // 距离用玩家位置（你实际在哪）
        double dx = data.targetX() - mc.player.getX();
        double dz = data.targetZ() - mc.player.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        int dy = data.targetY() - mc.player.blockPosition().getY();

        // 方位与俯仰统一用相机坐标（第三人称 / Aeronautics 相机会绕载具旋转）
        double camX = mc.player.getX();
        double camY = mc.player.getEyeY();
        double camZ = mc.player.getZ();
        float heading = mc.player.getViewYRot(1.0f);
        float camPitch = mc.player.getViewXRot(1.0f);
        try {
            var cam = mc.gameRenderer.getMainCamera();
            if (cam != null && cam.isInitialized()) {
                var cp = cam.getPosition();
                camX = cp.x;
                camY = cp.y;
                camZ = cp.z;
                heading = cam.getYRot();
                camPitch = cam.getXRot();
            }
        } catch (Throwable ignored) {
            // 相机不可用时退回玩家视角
        }

        double cdx = data.targetX() - camX;
        double cdz = data.targetZ() - camZ;
        double camHoriz = Math.sqrt(cdx * cdx + cdz * cdz);
        double targetYaw = Math.toDegrees(Math.atan2(-cdx, cdz));
        double rel = normalizeDeg(targetYaw - heading + REL_OFFSET_DEG);

        // 真实渲染 FOV（仅用于调试行显示）
        double usedFov = 70.0;
        try {
            usedFov = mc.options.fov().get();
        } catch (Throwable ignored) {
            // 保持 70
        }

        if (crossDim) {
            drawTargetMarker(g, mc, cx, cy, 0, HUD_WARN, false);
            g.drawCenteredString(mc.font, "TGT IN " + shortDim(data.targetDim()).toUpperCase(),
                    cx, cy - 58, HUD_WARN);
            g.drawCenteredString(mc.font, data.targetName(), cx, cy - 46, HUD_WARN);
        } else {
            // ---------------- 3D 投影 ----------------
            // 用相机基向量把目标世界坐标投影到屏幕。
            // 不再分别算 yaw/pitch 再拼位置 —— 两个角度插值不同步就会漂。
            // ---------------- NDC 投影（同 Xaero） ----------------
            // ---------------- 2.7.3 风格：水平角度映射 ----------------
            // 只用已验证准确的 rel 做水平定位，垂直固定在准星高度，
            // 高度差改用「上/下虚线」表示（不会漂、不受相机姿态影响）。
            double sideHint = 0.0;
            double upHint = 0.0;
            double px = cx;
            double py = cy + MARKER_Y_OFFSET;
            boolean behind = Math.abs(rel) > 90.0;
            boolean edgeArrows = false;

            double effRel = rel;
            double nx = behind ? (effRel >= 0 ? 1.0 : -1.0) : effRel / FOV_HALF;
            if (Math.abs(nx) > 1.0) {
                nx = nx > 0 ? 1.0 : -1.0;
                edgeArrows = true;
            }
            sideHint = nx;
            upHint = 0.0;
            px = cx + nx * (w * 0.42);
            py = cy + MARKER_Y_OFFSET;

            boolean offX = behind;
            boolean offY = behind;
            int tx = (int) Math.round(px);
            int ty = (int) Math.round(py);
            if (behind) {
                tx = sideHint >= 0 ? w - 26 : 26;
                ty = cy + MARKER_Y_OFFSET;
                edgeArrows = true;
            }

            // 平滑 + 死区：相机数据每帧微变会表现为漂移
            // 死区让小幅抖动直接锁定，避免近距离时标记持续滑动
            if (!offX && !offY) {
                if (Double.isNaN(smoothPx)) {
                    smoothPx = tx;
                    smoothPy = ty;
                } else {
                    double ddx = tx - smoothPx;
                    double ddy = ty - smoothPy;
                    if (Math.abs(ddx) > DEAD_ZONE || Math.abs(ddy) > DEAD_ZONE) {
                        // SMOOTHING 越大越平滑（响应系数 = 1 - SMOOTHING）
                        double resp = 1.0 - SMOOTHING;
                        smoothPx += ddx * resp;
                        smoothPy += ddy * resp;
                    }
                }
                tx = (int) Math.round(smoothPx);
                ty = (int) Math.round(smoothPy);
            } else {
                // 贴边时重置平滑状态，避免回到视野内时从错误位置滑过来
                smoothPx = Double.NaN;
                smoothPy = Double.NaN;
            }

            // 空间标记可整体关闭（Aeronautics 下相机姿态特殊，投影可能不准）
            if (SHOW_SPATIAL_MARKER) {
                drawTargetMarker(g, mc, tx, ty, sideHint >= 0 ? 1 : -1,
                        HUD_GREEN, behind || offX);
            }

            // 垂直贴边箭头
            if (offY) {
                boolean upArrow = upHint >= 0;
                int ay = upArrow ? ty - 24 : ty + 14;
                for (int i = 0; i < 6; i++) {
                    int span = 6 - i;
                    int yy = upArrow ? ay + i : ay + 5 - i;
                    g.fill(tx - span, yy, tx + span + 1, yy + 1, HUD_GREEN);
                }
            }

            // 目标名 + 距离 + 高度差（标上方）
            g.drawCenteredString(mc.font, data.targetName(), tx, ty - 46, HUD_GREEN);
            g.drawCenteredString(mc.font, ((int) Math.round(horizDist)) + "m", tx, ty - 34, HUD_GREEN);
            String dyText = (dy >= 0 ? "+" : "") + dy;
            g.drawCenteredString(mc.font, dyText, tx, ty - 22, HUD_DIM);

            // 目标来源标签：编队 / 信标 / 航线
            String kind = targetKindLabel(data);
            if (kind != null) {
                g.drawCenteredString(mc.font, kind, tx, ty - 58, kindColor(data));
            }

            // Y 轴方向虚线：目标比你在上方 → 标记上方竖线；在下方 → 下方竖线
            if (Math.abs(dy) >= 1) {
                boolean upSide = dy > 0;
                int len = (int) Math.min(72, 12 + Math.abs(dy) * 0.7);
                int x0 = tx;
                int startY = upSide ? (ty - 16) : (ty + 16);
                for (int i = 0; i < len; i += 3) {
                    int yy = upSide ? (startY - i) : (startY + i);
                    g.fill(x0, yy, x0 + 1, yy + 2, HUD_DIM);
                }
                // 末端三角，指明方向
                int tipY = upSide ? (startY - len) : (startY + len);
                for (int i = 0; i < 4; i++) {
                    int span = 3 - i;
                    int yy = upSide ? (tipY - 3 + i) : (tipY + 3 - i);
                    g.fill(x0 - span, yy, x0 + span + 1, yy + 1, HUD_GREEN);
                }
            }

            // 航线其他站点 + 全部 Xaero 路点
            if (SHOW_ALL_WAYPOINTS || SHOW_OTHER_WAYPOINTS) {
                refreshWaypointCache();
                drawOtherWaypoints(g, mc, cx, cy, w, heading, camX, camZ,
                        data.targetX(), data.targetZ());
            }
            // 编队：附近的其他载具
            drawFleetMarkers(g, mc, cx, cy, w, heading, camX, camZ);
            // 信标：只有铭牌、没有导航终端
            drawBeacons(g, mc, cx, cy, w, heading, camX, camZ);

            // 引导线：准星 → 目标（仅当目标在屏幕内）
            if (!behind && !offX && !offY) {
                drawGuideLine(g, cx, cy + 12, tx, ty - 12, HUD_DIM);
            }

            // 小地图雷达：纯 2D，永远准确（不依赖相机矩阵）
            if (SHOW_RADAR) {
                drawRadar(g, mc, cx, cy, rel, horizDist, dy, behind,
                        data.targetName(), data.arrived());
            }
        }

        // ---------------- 底部信息 ----------------
        if (data.arrived()) {
            g.drawCenteredString(mc.font, "== ARRIVED ==", cx, cy + 52, HUD_GREEN);
        }
        if (data.routeTotal() > 1) {
            String prog = "RTE " + (data.routeIndex() + 1) + "/" + data.routeTotal();
            g.drawString(mc.font, prog, 8, 8, HUD_GREEN, false);
        }
        // 编队频道
        g.drawString(mc.font, "CH " + data.myChannel(), 8, 20, HUD_FLEET, false);
        // 临时调试：实际角度（排查方向用，确认后可移除）
        if (SHOW_DEBUG) {
            String dbg = String.format(
                    "h=%.1f fov=%.0f tgt=%.1f rel=%.1f dist=%.0f pts=%d | me=(%.0f,%.0f) tgtpos=(%d,%d,%d)",
                    heading, FOV_HALF, targetYaw, rel, horizDist,
                    data.routePoints() == null ? 0 : data.routePoints().size(),
                    mc.player.getX(), mc.player.getZ(),
                    data.targetX(), data.targetY(), data.targetZ());
            g.drawString(mc.font, dbg, 8, 22, 0xFFFFFF00, false);

            long nowMs = System.currentTimeMillis();
            if (nowMs - lastDebugLog > 1000L) {
                lastDebugLog = nowMs;
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                        "[HUDAngle] {}", dbg);
            }
        }

        // 距离刻度条（底部）
        drawDistScale(g, mc, cx, h, horizDist);
    }

    /**
     * 海平面（地平线）指示线。
     *
     * 海平面在无限远处，其视线俯仰角恒为 0°，所以屏幕上就是「平视方向」那条线：
     * 平视时与准星重合，低头时上移，抬头时下移。
     */
    private static void drawHorizonLine(GuiGraphics g, Minecraft mc,
                                        int w, int h, int cx, int cy) {
        float pitch = 0.0f;
        try {
            pitch = mc.player.getViewXRot(1.0f);
        } catch (Throwable ignored) {
            return;
        }
        // 垂直半视角：用设置里的 FOV 近似（垂直 FOV / 2）
        double vHalf = 35.0;
        try {
            vHalf = mc.options.fov().get() / 2.0;
        } catch (Throwable ignored) {
            // 保持默认
        }
        double offset = Math.tan(Math.toRadians(pitch)) / Math.tan(Math.toRadians(vHalf)) * (h / 2.0);
        int y = (int) Math.round(cy - offset);
        if (y < 4 || y > h - 4) {
            return;
        }

        // 两端短实线 + 中间虚线，避免遮挡视野中心
        int half = (int) (w * 0.42);
        for (int x = cx - half; x < cx + half; x += 9) {
            boolean nearCenter = Math.abs(x - cx) < 40;
            int len = nearCenter ? 3 : 5;
            g.fill(x, y, x + len, y + 1, nearCenter ? HUD_DIM : HUD_GREEN);
        }
        // 中央高度标记
        g.fill(cx - 2, y - 3, cx + 3, y - 2, HUD_GREEN);
        g.fill(cx - 2, y + 2, cx + 3, y + 3, HUD_GREEN);
    }

    /** 顶部航向刻度：中线 ±55°，每 10° 一档 */
    private static void drawHeadingTape(GuiGraphics g, Minecraft mc, int cx, int h, NavigationHudPayload data) {
        // 下移避开左上角 PTT / 性能提示
        int baseY = TAPE_Y;
        int halfW = (int) (g.guiWidth() * 0.42);
        g.fill(cx - halfW, baseY, cx + halfW, baseY + 1, HUD_DIM);

        double yaw = mc.player.getViewYRot(1.0f);
        try {
            var cam = mc.gameRenderer.getMainCamera();
            if (cam != null && cam.isInitialized()) {
                yaw = cam.getYRot();
            }
        } catch (Throwable ignored) {
            // 用玩家视角
        }
        // 刻度范围：与投影一致的半视角
        double tapeHalf = 60.0;
        for (int deg = -60; deg <= 60; deg += 10) {
            double abs = yaw + deg;
            int x = cx + (int) Math.round(deg / tapeHalf * halfW);
            boolean major = deg % 30 == 0;
            int len = major ? 8 : 5;
            g.fill(x, baseY, x + 1, baseY + len, major ? HUD_GREEN : HUD_DIM);
            if (major) {
                String label = String.valueOf((int) ((abs % 360 + 360) % 360));
                g.drawCenteredString(mc.font, label, x, baseY - 11, HUD_DIM);
            }
        }
        g.fill(cx, baseY - 3, cx + 1, baseY, HUD_GREEN);
    }

    /** 每 2 秒从 Xaero 刷新一次路点缓存 */
    private static void refreshWaypointCache() {
        long now = System.currentTimeMillis();
        if (now - lastWpRefresh < 2000L) {
            return;
        }
        lastWpRefresh = now;
        try {
            com.hzldm.createnavigationterminal.integration.xaero.XaeroWaypointService
                    .listWaypointsOnClient(NavigationHudOverlay::acceptWaypoints);
        } catch (Throwable ignored) {
            // Xaero 不可用时保持空列表
        }
    }

    /**
     * 收下 Xaero 路点：按到玩家的距离排序，并截断到配置上限。
     *
     * <p>排序与截断只在缓存刷新时做（2 秒一次），绘制循环因此恒定为 O(上限)，
     * 不再随存档路点数量增长。
     */
    private static void acceptWaypoints(java.util.List<com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp> list) {
        if (list == null || list.isEmpty()) {
            cachedWaypoints = java.util.List.of();
            return;
        }
        int cap = MAX_OTHER_WAYPOINTS;
        Minecraft mc = Minecraft.getInstance();
        if (cap <= 0 || mc.player == null || list.size() <= cap) {
            cachedWaypoints = list;
            return;
        }
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        java.util.ArrayList<com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect.RawWp> copy =
                new java.util.ArrayList<>(list);
        copy.sort(java.util.Comparator.comparingDouble(wp -> {
            double dx = wp.x() - px, dz = wp.z() - pz;
            return dx * dx + dz * dz;
        }));
        cachedWaypoints = java.util.List.copyOf(copy.subList(0, cap));
    }

    /**
     * 编队标记：附近的其他载具/飞行器，用蓝色菱形标出，带名字与距离。
     *
     * 筛选条件：非玩家、存活、且（有自定义名字 或 有玩家在操控）。
     * 这样能覆盖「命名过的友机」和「其他玩家驾驶的载具」，
     * 又不会把普通怪物刷满屏幕。
     */
    private static void drawFleetMarkers(GuiGraphics g, Minecraft mc,
                                         int cx, int cy, int w,
                                         float heading, double camX, double camZ) {
        if (!SHOW_FLEET) {
            return;
        }
        NavigationHudPayload data = current;
        if (data == null || data.fleetTerminals() == null || data.fleetTerminals().isEmpty()) {
            return;
        }

        int rowY0 = cy + MARKER_Y_OFFSET - 54;
        var names = data.fleetNames() == null ? java.util.List.<String>of() : data.fleetNames();
        var flags = data.fleetFlags() == null ? java.util.List.<Boolean>of() : data.fleetFlags();

        for (int i = 0; i < data.fleetTerminals().size(); i++) {
            int[] f = data.fleetTerminals().get(i);
            // 同频道 = 编队内（主色）；不同频道 = 编队外（编队色半透明）
            boolean inFleet = i < flags.size() && Boolean.TRUE.equals(flags.get(i));
            int color = inFleet ? HUD_GREEN : withAlpha(HUD_FLEET, 0xA0);
            double dx = f[0] - camX;
            double dz = f[2] - camZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 4.0) {
                continue;
            }
            // 同一位置已经画过别的标记就跳过，避免文字叠字
            if (!DRAWN_KEYS.add(keyXZ(f[0], f[2]))) {
                continue;
            }
            double tYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double rel = normalizeDeg(tYaw - heading);
            boolean behind = Math.abs(rel) > 90.0;

            int px;
            if (behind) {
                px = rel >= 0 ? w - 24 : 24;
            } else {
                double nx = Math.max(-1.0, Math.min(1.0, rel / FOV_HALF));
                px = cx + (int) Math.round(nx * (w * 0.42));
            }

            // 菱形 → 改成和主标记同一套样式
            drawTargetMarker(g, mc, px, rowY0, rel >= 0 ? 1 : -1, color, behind);

            // 高度差：编队机比你高/低多少，用上下虚线表示
            int dyFleet = f[1] - mc.player.blockPosition().getY();
            if (Math.abs(dyFleet) >= 1) {
                boolean upSide = dyFleet > 0;
                int len = (int) Math.min(56, 10 + Math.abs(dyFleet) * 0.6);
                int startY = upSide ? (rowY0 - 16) : (rowY0 + 16);
                for (int k = 0; k < len; k += 3) {
                    int yy = upSide ? (startY - k) : (startY + k);
                    g.fill(px, yy, px + 1, yy + 2, withAlpha(color, 0x90));
                }
                int tipY = upSide ? (startY - len) : (startY + len);
                for (int k = 0; k < 4; k++) {
                    int span = 3 - k;
                    int yy = upSide ? (tipY - 3 + k) : (tipY + 3 - k);
                    g.fill(px - span, yy, px + span + 1, yy + 1, color);
                }
            }

            String name = i < names.size() ? names.get(i) : "";
            if (name == null || name.isEmpty()) {
                name = "导航终端";
            }
            g.drawCenteredString(mc.font, trimText(name, 18), px, rowY0 - 34, color);
            g.drawCenteredString(mc.font, distLabel(dist, dyFleet), px, rowY0 + 18, color);
        }
    }

    /**
     * 信标：只有铭牌、没有导航终端的位置，用青色小三角标出。
     * 画在编队标记更高的位置，避免重叠。
     */
    private static void drawBeacons(GuiGraphics g, Minecraft mc,
                                    int cx, int cy, int w,
                                    float heading, double camX, double camZ) {
        if (!SHOW_BEACONS) {
            return;
        }
        NavigationHudPayload data = current;
        if (data == null || data.beaconPositions() == null || data.beaconPositions().isEmpty()) {
            return;
        }
        int rowY0 = cy + MARKER_Y_OFFSET - 76;
        int color = HUD_BEACON;
        var names = data.beaconNames() == null ? java.util.List.<String>of() : data.beaconNames();

        for (int i = 0; i < data.beaconPositions().size(); i++) {
            int[] b = data.beaconPositions().get(i);
            double dx = b[0] - camX;
            double dz = b[2] - camZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 4.0) {
                continue;
            }
            // 同一位置已经画过别的标记就跳过，避免文字叠字
            if (!DRAWN_KEYS.add(keyXZ(b[0], b[2]))) {
                continue;
            }
            double tYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double rel = normalizeDeg(tYaw - heading);
            boolean behind = Math.abs(rel) > 90.0;

            int px;
            if (behind) {
                px = rel >= 0 ? w - 20 : 20;
            } else {
                double nx = Math.max(-1.0, Math.min(1.0, rel / FOV_HALF));
                px = cx + (int) Math.round(nx * (w * 0.42));
            }

            // 小三角（正立）
            for (int k = 0; k < 4; k++) {
                int span = 3 - k;
                g.fill(px - span, rowY0 - 3 + k, px + span + 1, rowY0 - 2 + k, color);
            }

            String nm = i < names.size() ? names.get(i) : "";
            g.drawCenteredString(mc.font, trimText(nm, 16), px, rowY0 - 16, color);
            g.drawCenteredString(mc.font, ((int) Math.round(dist)) + "m", px, rowY0 + 6, color);
        }
    }

    /** 目标来源标签：编队 / 信标 / 航线 / null（普通 Xaero 路点） */
    private static String targetKindLabel(NavigationHudPayload data) {
        int k = targetKind(data);
        return switch (k) {
            case 1 -> "★ 编队";
            case 2 -> "◆ 信标";
            case 3 -> "◇ 航线";
            default -> null;
        };
    }

    /** 标签配色 */
    private static int kindColor(NavigationHudPayload data) {
        return switch (targetKind(data)) {
            case 1 -> HUD_GREEN;
            case 2 -> HUD_BEACON;
            case 3 -> HUD_OTHER;
            default -> HUD_DIM;
        };
    }

    /** 判断当前目标的来源：1=编队 2=信标 3=航线站点 0=普通路点 */
    private static int targetKind(NavigationHudPayload data) {
        if (data == null) {
            return 0;
        }
        int tx = data.targetX();
        int tz = data.targetZ();
        var fp = data.fleetTerminals();
        if (fp != null) {
            for (int[] f : fp) {
                if (Math.abs(f[0] - tx) <= 2 && Math.abs(f[2] - tz) <= 2) {
                    return 1;
                }
            }
        }
        var bp = data.beaconPositions();
        if (bp != null) {
            for (int[] b : bp) {
                if (Math.abs(b[0] - tx) <= 2 && Math.abs(b[2] - tz) <= 2) {
                    return 2;
                }
            }
        }
        var rp = data.routePoints();
        if (rp != null) {
            for (int[] r : rp) {
                if (Math.abs(r[0] - tx) <= 2 && Math.abs(r[2] - tz) <= 2) {
                    return 3;
                }
            }
        }
        return 0;
    }

    /** 截断过长文本 */
    private static String trimText(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** 航线其他站点：**先画航线**，再用 Xaero 路点补空白处（顺序保证「下一站」不会被去重吃掉）。 */
    private static void drawOtherWaypoints(GuiGraphics g, Minecraft mc,
                                           int cx, int cy, int w,
                                           float heading, double camX, double camZ,
                                           int curTargetX, int curTargetZ) {
        NavigationHudPayload data = current;
        int rowY0 = cy + MARKER_Y_OFFSET - 30;
        it.unimi.dsi.fastutil.longs.LongOpenHashSet drawn = DRAWN_KEYS;
        drawn.clear();
        clearMarkerSlots();   // 每帧重置车道占用

        // 1) 航线站点优先画。
        //
        // 旧顺序是「Xaero 先画 → 航线后画」，而两边用的是同一批坐标（航线多半就是从
        // Xaero 路点里选的），所以航线站点全被判成重复跳过：
        // 结果是「下一站」的青色 ▸ 标记永远不出现，也不随 routeIndex 变化。
        if (data != null && data.routePoints() != null) {
            var rpAll = data.routePoints();
            int curIdx2 = -1;
            for (int i = 0; i < rpAll.size(); i++) {
                int[] p0 = rpAll.get(i);
                if (p0.length > 3 && p0[3] == 1) {
                    curIdx2 = i;
                    break;
                }
            }
            int nextIdx2;
            if (curIdx2 < 0) {
                nextIdx2 = -1;
            } else if (curIdx2 + 1 < rpAll.size()) {
                nextIdx2 = curIdx2 + 1;
            } else {
                // 末站：循环航线回绕到第 1 站，单程航线没有「下一站」
                nextIdx2 = data.routeLoop() ? 0 : -1;
            }

            for (int ri = 0; ri < rpAll.size(); ri++) {
                int[] pt = rpAll.get(ri);
                if (pt.length > 3 && pt[3] == 1) {
                    continue;
                }
                if (Math.abs(pt[0] - curTargetX) <= 2 && Math.abs(pt[2] - curTargetZ) <= 2) {
                    continue;
                }
                boolean isNext = (ri == nextIdx2);
                if (!isNext && !SHOW_OTHER_WAYPOINTS) {
                    continue;
                }
                if (!drawn.add(keyXZ(pt[0], pt[2]))) {
                    continue;
                }
                drawOneWaypoint(g, mc, cx, w, heading, camX, camZ, rowY0,
                        pt[0], pt[2], isNext ? HUD_NEXT : HUD_OTHER, isNext ? "\u25b8 " : "");
            }
        }

        // 2) Xaero 全量路点：跳过航线已经画过的坐标
        String nearestName = null;
        double nearestAbs = 999.0;
        if (SHOW_ALL_WAYPOINTS) {
            for (var wp : cachedWaypoints) {
                if (isWaypointHidden(wp.name())) {
                    continue;
                }
                // 跳过当前导航目标
                if (Math.abs(wp.x() - curTargetX) <= 2 && Math.abs(wp.z() - curTargetZ) <= 2) {
                    continue;
                }
                if (!drawn.add(keyXZ(wp.x(), wp.z()))) {
                    continue;
                }
                double absRel = drawOneWaypoint(g, mc, cx, w, heading, camX, camZ, rowY0,
                        wp.x(), wp.z(), HUD_OTHER);
                if (absRel >= 0 && absRel < nearestAbs) {
                    nearestAbs = absRel;
                    nearestName = wp.name();
                }
            }
        }

        // 准星对准某个路点时，在上方显示它的名字
        if (nearestName != null && nearestAbs < 12.0) {
            g.drawCenteredString(mc.font, nearestName, cx, rowY0 - 18, HUD_OTHER);
        }
    }

    /**
     * 算出某个路点在屏幕上的 x 坐标（与 drawOneWaypoint 同一套投影）。
     * 用于把文字标签贴在标记旁边，而不是固定画在屏幕中央。
     */
    private static int markerX(int cx, int w, float heading,
                               double camX, double camZ, int wx, int wz) {
        double dx = wx - camX;
        double dz = wz - camZ;
        if (Math.sqrt(dx * dx + dz * dz) < 2.0) {
            return -1;
        }
        double tYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = normalizeDeg(tYaw - heading);
        if (Math.abs(rel) > 90.0) {
            return rel >= 0 ? w - 24 : 24;
        }
        double nx = Math.max(-1.0, Math.min(1.0, rel / FOV_HALF));
        return cx + (int) Math.round(nx * (w * 0.42));
    }

    /** 返回该路点相对准星的角度绝对值（用于判定"准星正对着谁"）；无效返回 -1 */
    private static double drawOneWaypoint(GuiGraphics g, Minecraft mc,
                                          int cx, int w, float heading,
                                          double camX, double camZ, int rowY0,
                                          int wx, int wz, int color) {
        return drawOneWaypoint(g, mc, cx, w, heading, camX, camZ, rowY0, wx, wz, color, "");
    }

    /** 同 {@link #drawOneWaypoint}，但距离文字带前缀（下一站用 "▸ "） */
    private static double drawOneWaypoint(GuiGraphics g, Minecraft mc,
                                          int cx, int w, float heading,
                                          double camX, double camZ, int rowY0,
                                          int wx, int wz, int color, String prefix) {
        double dx = wx - camX;
        double dz = wz - camZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 2.0) {
            return -1;
        }
        double tYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = normalizeDeg(tYaw - heading);
        boolean behind = Math.abs(rel) > 90.0;

        int px;
        if (behind) {
            px = rel >= 0 ? w - 24 : 24;
        } else {
            double nx = rel / FOV_HALF;
            nx = Math.max(-1.0, Math.min(1.0, nx));
            px = cx + (int) Math.round(nx * (w * 0.42));
        }

        String label = prefix + ((int) Math.round(dist)) + "m";
        int halfW = mc.font.width(label) / 2 + 4;
        int lane = pickMarkerLane(px, halfW);
        if (lane < 0) {
            // 所有车道都被占：宁可不画，也不要叠成一团
            return Math.abs(rel);
        }
        int rowY = rowY0 + lane * MARKER_LANE_H;
        int s = 4;
        g.fill(px - s, rowY - s, px + s + 1, rowY - s + 1, color);
        g.fill(px - s, rowY + s, px + s + 1, rowY + s + 1, color);
        g.fill(px - s, rowY - s, px - s + 1, rowY + s + 1, color);
        g.fill(px + s, rowY - s, px + s + 1, rowY + s + 1, color);

        g.drawCenteredString(mc.font, label, px, rowY + s + 2, color);
        return Math.abs(rel);
    }

    /** 车道高度（像素）：标记文字一行高 */
    private static final int MARKER_LANE_H = 13;
    /** 同一帧内最多叠几层，超过就不画（避免糊成一片） */
    private static final int MARKER_LANES = 4;
    /** 每帧的占用记录：预分配数组，避免每帧 new int[] */
    private static final int[] SLOT_LANE = new int[64];
    private static final int[] SLOT_LEFT = new int[64];
    private static final int[] SLOT_RIGHT = new int[64];
    private static int slotCount = 0;

    private static void clearMarkerSlots() {
        slotCount = 0;
    }

    /** 找一个不与已占用区间重叠的车道；找不到返回 -1 */
    private static int pickMarkerLane(int px, int halfW) {
        int left = px - halfW;
        int right = px + halfW;
        for (int lane = 0; lane < MARKER_LANES; lane++) {
            boolean free = true;
            for (int i = 0; i < slotCount; i++) {
                if (SLOT_LANE[i] != lane) {
                    continue;
                }
                if (right >= SLOT_LEFT[i] && left <= SLOT_RIGHT[i]) {
                    free = false;
                    break;
                }
            }
            if (free) {
                if (slotCount < SLOT_LANE.length) {
                    SLOT_LANE[slotCount] = lane;
                    SLOT_LEFT[slotCount] = left;
                    SLOT_RIGHT[slotCount] = right;
                    slotCount++;
                }
                return lane;
            }
        }
        return -1;
    }

    /** 目标标记：按 markerStyle 选形状，贴边时加箭头 */
    private static void drawTargetMarker(GuiGraphics g, Minecraft mc, int x, int y,
                                         int relDeg, int color, boolean edge) {
        switch (MARKER_STYLE == null ? "box" : MARKER_STYLE.toLowerCase()) {
            case "diamond" -> drawDiamond(g, x, y, color);
            case "cross" -> drawCross(g, x, y, color);
            case "bracket" -> drawBracket(g, x, y, color);
            case "dot" -> drawDot(g, x, y, color);
            case "chevron" -> drawChevron(g, x, y, color);
            default -> drawBox(g, x, y, color);
        }

        if (edge) {
            // 贴边箭头：relDeg > 0 目标在右，箭头指右
            boolean pointRight = relDeg > 0;
            int ax = pointRight ? x + 14 : x - 18;
            for (int i = 0; i < 6; i++) {
                int x0 = pointRight ? ax + i : ax + 5 - i;
                int x1 = pointRight ? ax + i + 1 : ax + 6 - i;
                g.fill(x0, y - 5 + i, x1, y + 6 - i, color);
            }
        }
    }

    /** 方框：四角加粗的测距框 + 中心十字 */
    private static void drawBox(GuiGraphics g, int x, int y, int color) {
        int r = 9;
        g.fill(x - r, y - r, x + r + 1, y - r + 1, color);
        g.fill(x - r, y + r, x + r + 1, y + r + 1, color);
        g.fill(x - r, y - r, x - r + 1, y + r + 1, color);
        g.fill(x + r, y - r, x + r + 1, y + r + 1, color);
        for (int i = 0; i < 4; i++) {
            g.fill(x - r, y - r + i, x - r + 4 - i, y - r + i + 1, color);
            g.fill(x + r - 3 + i, y - r + i, x + r + 1, y - r + i + 1, color);
            g.fill(x - r, y + r - i, x - r + 4 - i, y + r - i + 1, color);
            g.fill(x + r - 3 + i, y + r - i, x + r + 1, y + r - i + 1, color);
        }
        g.fill(x - 3, y, x + 4, y + 1, color);
        g.fill(x, y - 3, x + 1, y + 4, color);
    }

    /** 菱形轮廓 */
    private static void drawDiamond(GuiGraphics g, int x, int y, int color) {
        int r = 9;
        for (int i = 0; i <= r; i++) {
            int w = r - i;
            g.fill(x - w, y - i, x + w + 1, y - i + 1, color);
            g.fill(x - w, y + i, x + w + 1, y + i + 1, color);
        }
        g.fill(x - 2, y - 1, x + 3, y + 2, color);
    }

    /** 大十字 + 内框 */
    private static void drawCross(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 13, y, x + 14, y + 1, color);
        g.fill(x, y - 13, x + 1, y + 14, color);
        int r = 5;
        g.fill(x - r, y - r, x + r + 1, y - r + 1, color);
        g.fill(x - r, y + r, x + r + 1, y + r + 1, color);
        g.fill(x - r, y - r, x - r + 1, y + r + 1, color);
        g.fill(x + r, y - r, x + r + 1, y + r + 1, color);
    }

    /** 左右括号 [ ] */
    private static void drawBracket(GuiGraphics g, int x, int y, int color) {
        int r = 11;
        int hh = 8;
        // 左括号
        g.fill(x - r, y - hh, x - r + 1, y + hh + 1, color);
        g.fill(x - r, y - hh, x - r + 4, y - hh + 1, color);
        g.fill(x - r, y + hh, x - r + 4, y + hh + 1, color);
        // 右括号
        g.fill(x + r, y - hh, x + r + 1, y + hh + 1, color);
        g.fill(x + r - 3, y - hh, x + r + 1, y - hh + 1, color);
        g.fill(x + r - 3, y + hh, x + r + 1, y + hh + 1, color);
    }

    /** 圆点 + 外环 */
    private static void drawDot(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 2, y - 2, x + 3, y + 3, color);
        int r = 8;
        for (int i = -r; i <= r; i++) {
            int hh = (int) Math.sqrt(Math.max(0, r * r - i * i));
            g.fill(x + i, y - hh, x + i + 1, y - hh + 1, color);
            g.fill(x + i, y + hh, x + i + 1, y + hh + 1, color);
        }
    }

    /** V 形箭头（指向目标） */
    private static void drawChevron(GuiGraphics g, int x, int y, int color) {
        for (int i = 0; i < 7; i++) {
            g.fill(x - 9 + i, y - 7 + i, x - 9 + i + 1, y - 7 + i + 1, color);
            g.fill(x + 9 - i, y - 7 + i, x + 9 - i + 1, y - 7 + i + 1, color);
        }
        g.fill(x - 1, y, x + 2, y + 3, color);
    }

    /** 准星到目标的虚线引导 */
    private static void drawGuideLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = 40;
        for (int i = 0; i < steps; i += 2) {
            int px = x0 + (x1 - x0) * i / steps;
            int py = y0 + (y1 - y0) * i / steps;
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** 画圆环（逐行填像素） */
    private static void drawRing(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int i = -r; i <= r; i++) {
            int hh = (int) Math.sqrt(Math.max(0, r * r - i * i));
            g.fill(cx + i, cy - hh, cx + i + 1, cy - hh + 1, color);
            g.fill(cx + i, cy + hh, cx + i + 1, cy + hh + 1, color);
        }
    }

    /**
     * 小地图雷达：纯 2D，不依赖相机矩阵 / FOV。
     *
     * 玩家永远在圆心、朝上；目标按相对方位与距离归一化画成点。
     * 不投影、无漂移、无 FOV 校准。
     */
    private static void drawRadar(GuiGraphics g, Minecraft mc, int cx, int cy,
                                  double rel, double dist, int dy, boolean behind,
                                  String name, boolean arrived) {
        int radius = 48;
        int rcx = cx;
        int rcy = cy + 128;

        // 背景盘
        for (int i = -radius; i <= radius; i++) {
            int hh = (int) Math.sqrt(Math.max(0, radius * radius - i * i));
            g.fill(rcx + i, rcy - hh, rcx + i + 1, rcy + hh + 1, 0x40000000);
        }
        // 圆环与十字
        drawRing(g, rcx, rcy, radius, HUD_DIM);
        drawRing(g, rcx, rcy, radius / 2, HUD_DIM);
        g.fill(rcx - radius, rcy, rcx + radius, rcy + 1, HUD_DIM);
        g.fill(rcx, rcy - radius, rcx + 1, rcy + radius, HUD_DIM);

        // 玩家朝向标记（上方三角）
        for (int i = 0; i < 5; i++) {
            int span = 5 - i;
            g.fill(rcx - span, rcy - radius + 2 + i, rcx + span + 1, rcy - radius + 3 + i, HUD_GREEN);
        }

        // 目标点位置：距离归一化（满格 = radarRange）
        double range = 400.0;
        double ratio = Math.min(1.0, dist / range);
        double rad = Math.toRadians(rel);
        int px = rcx + (int) Math.round(Math.sin(rad) * (radius - 4) * ratio);
        int py = rcy - (int) Math.round(Math.cos(rad) * (radius - 4) * ratio);

        // 目标点（在圆心说明距离很近）
        int color = arrived ? HUD_GREEN : HUD_GREEN;
        g.fill(px - 3, py - 3, px + 4, py + 4, color);
        g.fill(px - 5, py, px - 4, py + 1, HUD_DIM);
        g.fill(px + 4, py, px + 5, py + 1, HUD_DIM);
        g.fill(px, py - 5, px + 1, py - 4, HUD_DIM);
        g.fill(px, py + 4, px + 1, py + 5, HUD_DIM);

        // 标签
        g.drawCenteredString(mc.font, distLabel(dist, dy), rcx, rcy + radius + 4, HUD_GREEN);
        if (name != null && !name.isEmpty()) {
            g.drawCenteredString(mc.font, name, rcx, rcy - radius - 12, HUD_GREEN);
        }
        if (behind) {
            g.drawCenteredString(mc.font, "BEHIND", rcx, rcy + radius + 16, HUD_DIM);
        }
    }
    private static void drawDistScale(GuiGraphics g, Minecraft mc, int cx, int h, double dist) {
        int y = h - DIST_SCALE_BOTTOM;
        int half = 130;
        g.fill(cx - half, y, cx + half, y + 1, HUD_DIM);
        for (int i = -5; i <= 5; i++) {
            int x = cx + i * (half / 5);
            int len = (i % 5 == 0) ? 7 : 4;
            g.fill(x, y, x + 1, y + len, HUD_DIM);
        }
        String label = ((int) Math.round(dist)) + "m";
        g.drawCenteredString(mc.font, label, cx, y + 10, HUD_GREEN);
    }

    /**
     * HUD 基准朝向：玩家视角朝向，按当前帧插值。
     *
     * 之前写死 partialTick=1.0f（等于"下一 tick"），快速转头时标记会超前漂移；
     * 这里取真实的帧插值因子。
     */
    /**
     * 取 HUD 基准朝向。
     *
     *  第一人称      → 相机 yaw（与画面严格同步）
     *  第三人称背后  → 玩家视角 yaw
     *  第三人称正面  → 玩家视角 yaw + 180（相机面向玩家，看到的是反方向）
     */
    private static float viewHeading(Minecraft mc) {
        net.minecraft.client.CameraType type = null;
        try {
            type = mc.options.getCameraType();
        } catch (Throwable ignored) {
            // 拿不到按第一人称处理
        }

        if (type == null || type.isFirstPerson()) {
            try {
                var cam = mc.gameRenderer.getMainCamera();
                if (cam != null && cam.isInitialized()) {
                    return cam.getYRot();
                }
            } catch (Throwable ignored) {
                // 相机未就绪时继续往下走
            }
        }

        float pt = 1.0f;
        try {
            pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
        } catch (Throwable ignored) {
            pt = 0.5f;
        }
        float yaw = mc.player.getViewYRot(pt);
        if (type != null && type.isMirrored()) {
            // 第三人称正面：相机面向玩家，看到的是反方向
            yaw += 180.0f;
        }
        return (float) normalizeDeg(yaw);
    }

    /** 兼容旧调用点：返回当前朝向（相机优先） */
    private static float headingFor(Minecraft mc) {
        return viewHeading(mc);
    }

    private static double normalizeDeg(double a) {
        return ((a % 360) + 540) % 360 - 180;
    }

    private static String shortDim(String dim) {
        if (dim == null || dim.isEmpty()) {
            return "?";
        }
        return switch (dim) {
            case "minecraft:overworld" -> "\u4E3B\u4E16\u754C";
            case "minecraft:the_nether" -> "\u4E0B\u754C";
            case "minecraft:the_end" -> "\u672B\u5730";
            default -> dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim;
        };
    }

    /**
     * 「距离 + 相对高差」标签的缓存。
     *
     * <p>原实现每帧对每个图标做 {@code (int)Math.round(dist) + "m" + "  " + ...} 的字符串拼接，
     * 结果只由 (整数米, 整数高差) 决定。这里按这对整数做一个上限 128 的小缓存：
     * 命中即零分配；超过上限整体清空，避免无界增长。
     */
    private static final java.util.HashMap<Long, String> DIST_LABEL_CACHE = new java.util.HashMap<>();

    private static String distLabel(double dist, int dy) {
        int meters = (int) Math.round(dist);
        long key = ((long) meters << 32) ^ (dy & 0xFFFFFFFFL);
        String cached = DIST_LABEL_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        StringBuilder sb = new StringBuilder(16).append(meters).append('m');
        if (Math.abs(dy) >= 1) {
            sb.append("  ");
            if (dy >= 0) {
                sb.append('+');
            }
            sb.append(dy);
        }
        String text = sb.toString();
        if (DIST_LABEL_CACHE.size() > 128) {
            DIST_LABEL_CACHE.clear();
        }
        DIST_LABEL_CACHE.put(key, text);
        return text;
    }
}
