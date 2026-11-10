package com.hzldm.createnavigationterminal.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置（config/create_navigation_terminal-client.toml）
 *
 * HUD 采用「水平角度映射 + 垂直固定 + 上下虚线」方案：
 * 水平位置由相对方位角换算，垂直位置固定，高度差用上/下虚线表示。
 * 因此只保留真正会用到的那几项。
 */
public final class ClientConfig {

    public static final ModConfigSpec SPEC;

    /** 水平方位校准（度） */
    public static final ModConfigSpec.DoubleValue REL_OFFSET_DEG;
    /** 水平映射半视角（度）：越小标记越靠外 */
    public static final ModConfigSpec.DoubleValue FOV_HALF_DEG;
    /** 主色 RRGGBB */
    public static final ModConfigSpec.ConfigValue<String> COLOR_MAIN;
    /** 次色 RRGGBB */
    public static final ModConfigSpec.ConfigValue<String> COLOR_DIM;
    /** 是否显示调试行 */
    public static final ModConfigSpec.BooleanValue SHOW_DEBUG;
    /** 是否显示雷达 */
    public static final ModConfigSpec.BooleanValue SHOW_RADAR;
    /** 是否显示空间标记（方框） */
    public static final ModConfigSpec.BooleanValue SHOW_SPATIAL_MARKER;
    /** 是否显示航线其他站点 */
    public static final ModConfigSpec.BooleanValue SHOW_OTHER_WAYPOINTS;
    /** 是否显示 Xaero 全部路点 */
    public static final ModConfigSpec.BooleanValue SHOW_ALL_WAYPOINTS;
    /** 其他路点颜色 RRGGBB */
    public static final ModConfigSpec.ConfigValue<String> COLOR_OTHER;
    /** 隐藏的路点名（逗号分隔） */
    public static final ModConfigSpec.ConfigValue<String> HIDDEN_WAYPOINTS;
    /** 是否显示海平面（地平线）指示线 */
    public static final ModConfigSpec.BooleanValue SHOW_HORIZON;
    /** 是否显示编队标记 */
    public static final ModConfigSpec.BooleanValue SHOW_FLEET;
    /** 编队标记颜色 RRGGBB */
    public static final ModConfigSpec.ConfigValue<String> COLOR_FLEET;
    /** 是否显示信标（只有铭牌无终端） */
    public static final ModConfigSpec.BooleanValue SHOW_BEACONS;
    /** 信标颜色 RRGGBB */
    public static final ModConfigSpec.ConfigValue<String> COLOR_BEACON;
    /** 顶部航向刻度 Y */
    public static final ModConfigSpec.IntValue TAPE_Y;
    /** 距离游标距底部 */
    public static final ModConfigSpec.IntValue DIST_SCALE_BOTTOM;
    /** 标记相对屏幕中心的 Y 偏移 */
    public static final ModConfigSpec.IntValue MARKER_Y_OFFSET;
    /** 准星尺寸（半长像素） */
    public static final ModConfigSpec.IntValue CROSSHAIR_SIZE;
    /** 标记样式 */
    public static final ModConfigSpec.ConfigValue<String> MARKER_STYLE;
    /** 标记平滑强度 */
    public static final ModConfigSpec.DoubleValue SMOOTHING;
    /** 死区像素 */
    public static final ModConfigSpec.DoubleValue DEAD_ZONE;
    /** 是否要求佩戴护目镜 */
    public static final ModConfigSpec.BooleanValue REQUIRE_GOGGLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Navigation HUD settings").push("hud");

        COLOR_MAIN = b
                .comment("HUD main color, RRGGBB hex without '#'.")
                .define("colorMain", "3CFF5A");

        COLOR_DIM = b
                .comment("HUD secondary color, RRGGBB hex without '#'.")
                .define("colorDim", "28C040");

        MARKER_STYLE = b
                .comment("Target marker shape.",
                        "box / diamond / cross / bracket / dot / chevron")
                .define("markerStyle", "box");

        SHOW_SPATIAL_MARKER = b
                .comment("Show the box marker in front of you.",
                        "Turn off to keep only the radar and heading tape.")
                .define("showSpatialMarker", true);

        SHOW_OTHER_WAYPOINTS = b
                .comment("Show the other waypoints of the route in the secondary color.")
                .define("showOtherWaypoints", true);

        SHOW_ALL_WAYPOINTS = b
                .comment("Show every Xaero waypoint in the HUD (not just the ones on the route).")
                .define("showAllWaypoints", true);

        COLOR_OTHER = b
                .comment("Color for other waypoints, RRGGBB hex without '#'.")
                .define("colorOther", "FFA500");

        HIDDEN_WAYPOINTS = b
                .comment("Comma-separated waypoint names to hide from the HUD.",
                        "Example: home,base,mining")
                .define("hiddenWaypoints", "");

        SHOW_HORIZON = b
                .comment("Draw a dashed horizon (sea level) line that moves as you look up/down.")
                .define("showHorizon", true);

        SHOW_FLEET = b
                .comment("Show fleet markers for other vehicles carrying a navigation terminal.")
                .define("showFleet", true);

        COLOR_FLEET = b
                .comment("Fleet marker color, RRGGBB hex without '#'.")
                .define("colorFleet", "4FA3FF");

        SHOW_BEACONS = b
                .comment("Show beacons for contraptions that only have a nameplate (no terminal).")
                .define("showBeacons", true);

        COLOR_BEACON = b
                .comment("Beacon marker color, RRGGBB hex without '#'.")
                .define("colorBeacon", "00E5FF");

        REQUIRE_GOGGLES = b
                .comment("Only show the HUD when wearing goggles (Create goggles, Curios, etc.).")
                .define("requireGoggles", true);

        SHOW_RADAR = b
                .comment("Show the 2D radar under the crosshair.")
                .define("showRadar", true);

        SHOW_DEBUG = b
                .comment("Show the yellow debug line.")
                .define("showDebug", false);

        REL_OFFSET_DEG = b
                .comment("Horizontal calibration in degrees.",
                        "Marker too far left -> positive; too far right -> negative.")
                .defineInRange("relOffsetDeg", 0.0, -180.0, 180.0);

        FOV_HALF_DEG = b
                .comment("Half field of view used to place the marker horizontally (degrees).",
                        "Larger = marker stays closer to center; smaller = moves out faster.")
                .defineInRange("fovHalfDeg", 100.0, 20.0, 170.0);

        SMOOTHING = b
                .comment("Marker smoothing: 0 = raw, higher = smoother but laggier.")
                .defineInRange("smoothing", 0.6, 0.0, 0.95);

        DEAD_ZONE = b
                .comment("Dead zone in pixels: marker stays still for smaller deviations.")
                .defineInRange("deadZone", 2.0, 0.0, 30.0);

        TAPE_Y = b
                .comment("Y position of the heading tape.")
                .defineInRange("headingTapeY", 46, 0, 400);

        DIST_SCALE_BOTTOM = b
                .comment("Distance scale offset from the bottom of the screen.")
                .defineInRange("distScaleBottom", 82, 0, 400);

        MARKER_Y_OFFSET = b
                .comment("Marker vertical offset from screen center.",
                        "Negative = above center.")
                .defineInRange("markerYOffset", -6, -400, 400);

        CROSSHAIR_SIZE = b
                .comment("Crosshair half-length in pixels.")
                .defineInRange("crosshairSize", 10, 3, 60);

        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
