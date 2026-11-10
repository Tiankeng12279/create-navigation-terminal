package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 导航 HUD 配置 / 校准界面（F9 打开）
 *
 * 分三页：校准 / 布局 / 显示。所有改动实时生效，
 * 点「保存并关闭」写入配置文件。
 */
public class HudCalibrationScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int TOP_Y = 56;

    private static final String[] PAGE_NAMES = {"校准", "布局", "显示", "路点", "编队"};

    private static final String[] STYLES = {"box", "diamond", "cross", "bracket", "dot", "chev"};
    private static final String[] STYLE_VALUES = {"box", "diamond", "cross", "bracket", "dot", "chevron"};
    private static final String[] MAIN_COLORS =
            {"3CFF5A", "00E5FF", "FFD24A", "FF6A3C", "FFFFFF", "B45CFF"};
    private static final String[] DIM_COLORS =
            {"28C040", "0088AA", "AA8822", "AA4422", "AAAAAA", "7733AA"};

    private static final class NumRow {
        final String label;
        final java.util.function.DoubleSupplier getter;
        final java.util.function.DoubleConsumer setter;
        final double step;
        final double min;
        final double max;
        final String format;

        NumRow(String label, java.util.function.DoubleSupplier getter,
               java.util.function.DoubleConsumer setter,
               double step, double min, double max, String format) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.step = step;
            this.min = min;
            this.max = max;
            this.format = format;
        }

        void adjust(int dir) {
            double v = getter.getAsDouble() + dir * step;
            v = Math.max(min, Math.min(max, v));
            setter.accept(v);
        }

        String display() {
            return String.format(format, getter.getAsDouble());
        }
    }

    private final List<NumRow> nums = new ArrayList<>();
    private int page = 0;

    public HudCalibrationScreen() {
        super(Component.literal("导航 HUD 配置"));
    }

    private int rowY(int index) {
        return TOP_Y + index * ROW_H;
    }

    @Override
    protected void init() {
        NavigationHudOverlay.beginCalibration();
        rebuild();
    }

    private void buildRows() {
        nums.clear();
        if (page == 0) {
            nums.add(new NumRow("左右校准 (度)", NavigationHudOverlay::getRelOffset,
                    NavigationHudOverlay::setRelOffset, 0.5, -180, 180, "%.1f"));
            nums.add(new NumRow("水平视角 (度)", NavigationHudOverlay::getFovHalf,
                    NavigationHudOverlay::setFovHalf, 2.0, 20, 170, "%.0f"));
            nums.add(new NumRow("平滑", NavigationHudOverlay::getSmoothing,
                    NavigationHudOverlay::setSmoothing, 0.05, 0, 0.95, "%.2f"));
            nums.add(new NumRow("死区 (px)", NavigationHudOverlay::getDeadZone,
                    NavigationHudOverlay::setDeadZone, 0.5, 0, 30, "%.1f"));
            nums.add(new NumRow("准星尺寸", NavigationHudOverlay::getCrosshairSize,
                    NavigationHudOverlay::setCrosshairSize, 1, 3, 60, "%.0f"));
        } else if (page == 1) {
            nums.add(new NumRow("刻度 Y", NavigationHudOverlay::getTapeY,
                    NavigationHudOverlay::setTapeY, 4, 0, 400, "%.0f"));
            nums.add(new NumRow("距离游标", NavigationHudOverlay::getDistScaleBottom,
                    NavigationHudOverlay::setDistScaleBottom, 4, 0, 400, "%.0f"));
            nums.add(new NumRow("标记偏移 Y", NavigationHudOverlay::getMarkerYOffset,
                    NavigationHudOverlay::setMarkerYOffset, 4, -400, 400, "%.0f"));
        }
    }

    private void rebuild() {
        this.clearWidgets();
        buildRows();

        int cx = this.width / 2;

        // 页签
        int tabW = 72;
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            final int target = i;
            boolean active = (page == i);
            addRenderableWidget(NavButton.of(
                    Component.literal(active ? "[" + PAGE_NAMES[i] + "]" : PAGE_NAMES[i]),
                    b -> {
                        page = target;
                        rebuild();
                    }).bounds(cx - (tabW * PAGE_NAMES.length) / 2 + i * tabW, 28, tabW - 2, 18).build());
        }

        // 数值行
        for (int i = 0; i < nums.size(); i++) {
            final NumRow row = nums.get(i);
            int ry = rowY(i) - 3;
            addRenderableWidget(NavButton.of(Component.literal("-"), b -> row.adjust(-1))
                    .bounds(cx + 44, ry, 20, 18).build());
            addRenderableWidget(NavButton.of(Component.literal("+"), b -> row.adjust(1))
                    .bounds(cx + 136, ry, 20, 18).build());
        }

        if (page == 2) {
            buildDisplayPage(cx);
        } else if (page == 3) {
            buildWaypointPage(cx);
        } else if (page == 4) {
            buildFleetPage(cx);
        }
        buildColorPicker(cx);

        // 底部按钮
        int by = this.height - 34;
        addRenderableWidget(NavButton.of(Component.literal("保存并关闭"), b -> {
            NavigationHudOverlay.saveCalibration();
            NavigationHudOverlay.endCalibration();
            this.onClose();
        }).bounds(cx - 158, by, 100, 20).build());

        addRenderableWidget(NavButton.of(Component.literal("恢复默认"), b -> {
            resetAll();
            rebuild();
        }).bounds(cx - 50, by, 100, 20).build());

        if (page == 3) {
            addRenderableWidget(NavButton.of(Component.literal("取消全部隐藏"), b -> {
                NavigationHudOverlay.setHiddenWaypoints("");
                rebuild();
            }).bounds(cx + 58, by, 100, 20).build());
        }
    }

    private void buildDisplayPage(int cx) {
        // 标记样式：下拉栏（点一下展开/循环选择）
        String curStyle = NavigationHudOverlay.getMarkerStyle();
        String initial = "box";
        for (String v : STYLE_VALUES) {
            if (v.equalsIgnoreCase(curStyle)) {
                initial = v;
                break;
            }
        }
        addRenderableWidget(
                net.minecraft.client.gui.components.CycleButton
                        .<String>builder(v -> net.minecraft.network.chat.Component.literal(v))
                        .withValues(java.util.List.of(STYLE_VALUES))
                        .withInitialValue(initial)
                        .create(cx + 20, rowY(0) - 3, 120, 18,
                                net.minecraft.network.chat.Component.literal("标记样式"),
                                (btn, value) -> NavigationHudOverlay.setMarkerStyle(value)));

        addToggle(cx, rowY(1), NavigationHudOverlay::isShowSpatialMarker,
                NavigationHudOverlay::setShowSpatialMarker);
        addToggle(cx, rowY(2), NavigationHudOverlay::isShowRadar,
                NavigationHudOverlay::setShowRadar);
        addToggle(cx, rowY(3), NavigationHudOverlay::isShowDebug,
                NavigationHudOverlay::setShowDebug);
        addToggle(cx, rowY(4), NavigationHudOverlay::isShowOtherWaypoints,
                NavigationHudOverlay::setShowOtherWaypoints);

        // 颜色：点按钮弹出取色器
        addColorButton(cx, rowY(5), "main");
        addColorButton(cx, rowY(6), "dim");
    }

    /** 颜色行按钮：点开该颜色的取色器 */
    private void addColorButton(int cx, int y, String target) {
        addRenderableWidget(NavButton.of(Component.literal("选色"), b -> {
            pickerTarget = target;
            rebuild();
        }).bounds(cx + 76, y - 3, 44, 18).build());
    }

    /** 当前打开的取色器目标（null = 未打开） */
    private String pickerTarget = null;

    /** 打开取色器浮层 */
    private void buildColorPicker(int cx) {
        if (pickerTarget == null) {
            return;
        }
        int initRgb;
        java.util.function.IntConsumer apply;
        switch (pickerTarget) {
            case "main" -> {
                initRgb = Integer.parseInt(NavigationHudOverlay.getColorMainHex(), 16);
                apply = rgb -> NavigationHudOverlay.setColorMainHex(String.format("%06X", rgb));
            }
            case "dim" -> {
                initRgb = Integer.parseInt(NavigationHudOverlay.getColorDimHex(), 16);
                apply = rgb -> NavigationHudOverlay.setColorDimHex(String.format("%06X", rgb));
            }
            case "other" -> {
                initRgb = Integer.parseInt(NavigationHudOverlay.getColorOtherHex(), 16);
                apply = rgb -> NavigationHudOverlay.setColorOtherHex(String.format("%06X", rgb));
            }
            case "fleet" -> {
                initRgb = Integer.parseInt(NavigationHudOverlay.getColorFleetHex(), 16);
                apply = rgb -> NavigationHudOverlay.setColorFleetHex(String.format("%06X", rgb));
            }
            case "beacon" -> {
                initRgb = Integer.parseInt(NavigationHudOverlay.getColorBeaconHex(), 16);
                apply = rgb -> NavigationHudOverlay.setColorBeaconHex(String.format("%06X", rgb));
            }
            default -> {
                return;
            }
        }

        int pw = 180;
        int ph = 120;
        int px = cx - pw / 2;
        int py = this.height / 2 - ph / 2;
        addRenderableWidget(new ColorPickerWidget(px, py, pw, ph, initRgb, apply));
        addRenderableWidget(NavButton.of(Component.literal("完成"), b -> {
            pickerTarget = null;
            rebuild();
        }).bounds(cx - 20, py + ph + 6, 60, 18).build());
    }

    /** 第 5 页：编队与信标 */
    private void buildFleetPage(int cx) {
        addToggle(cx, rowY(0), NavigationHudOverlay::isShowFleet,
                NavigationHudOverlay::setShowFleet);
        addToggle(cx, rowY(1), NavigationHudOverlay::isShowBeacons,
                NavigationHudOverlay::setShowBeacons);

        addRenderableWidget(NavButton.of(Component.literal("选色"), b -> {
            pickerTarget = "fleet";
            rebuild();
        }).bounds(cx + 76, rowY(2) - 3, 44, 18).build());

        addRenderableWidget(NavButton.of(Component.literal("选色"), b -> {
            pickerTarget = "beacon";
            rebuild();
        }).bounds(cx + 76, rowY(3) - 3, 44, 18).build());
    }

    /** 第 4 页：路点显示 */
    private void buildWaypointPage(int cx) {
        NavigationHudOverlay.refreshWaypointsNow();
        listTop = rowY(5);

        addToggle(cx, rowY(0), NavigationHudOverlay::isShowAllWaypoints,
                NavigationHudOverlay::setShowAllWaypoints);
        addToggle(cx, rowY(1), NavigationHudOverlay::isShowOtherWaypoints,
                NavigationHudOverlay::setShowOtherWaypoints);
        addToggle(cx, rowY(2), NavigationHudOverlay::isShowHorizon,
                NavigationHudOverlay::setShowHorizon);

        addRenderableWidget(NavButton.of(Component.literal("选色"), b -> {
            pickerTarget = "other";
            rebuild();
        }).bounds(cx + 76, rowY(3) - 3, 44, 18).build());
    }

    /** 切换某个路点的隐藏状态 */
    private void toggleHidden(String name) {
        String cur = NavigationHudOverlay.getHiddenWaypoints();
        java.util.List<String> parts = new java.util.ArrayList<>();
        boolean found = false;
        if (cur != null && !cur.isBlank()) {
            for (String s : cur.split(",")) {
                String t = s.trim();
                if (t.isEmpty()) {
                    continue;
                }
                if (t.equalsIgnoreCase(name)) {
                    found = true;
                    continue;
                }
                parts.add(t);
            }
        }
        if (!found) {
            parts.add(name);
        }
        NavigationHudOverlay.setHiddenWaypoints(String.join(",", parts));
    }

    private int listTop = 0;

    private void addToggle(int cx, int y,
                           java.util.function.BooleanSupplier getter,
                           java.util.function.Consumer<Boolean> setter) {        addRenderableWidget(NavButton.of(
                Component.literal(getter.getAsBoolean() ? "开" : "关"),
                b -> {
                    setter.accept(!getter.getAsBoolean());
                    rebuild();
                }).bounds(cx + 76, y - 3, 44, 18).build());
    }

    private void resetAll() {
        NavigationHudOverlay.setRelOffset(0);
        NavigationHudOverlay.setFovHalf(100);
        NavigationHudOverlay.setSmoothing(0.6);
        NavigationHudOverlay.setDeadZone(2.0);
        NavigationHudOverlay.setTapeY(46);
        NavigationHudOverlay.setDistScaleBottom(82);
        NavigationHudOverlay.setMarkerYOffset(-6);
        NavigationHudOverlay.setCrosshairSize(10);
        NavigationHudOverlay.setMarkerStyle("box");
        NavigationHudOverlay.setShowSpatialMarker(true);
        NavigationHudOverlay.setShowRadar(true);
        NavigationHudOverlay.setShowDebug(false);
        NavigationHudOverlay.setShowOtherWaypoints(true);
        NavigationHudOverlay.setColorMainHex("3CFF5A");
        NavigationHudOverlay.setColorDimHex("28C040");
    }

    /**
     * 背景（含我们自己的卡片）必须画在这里，不能画在 render() 里：
     * {@code Screen#render} 会再次调用 renderBackground，把先前画的东西盖掉 ——
     * 之前米色内容区看起来发灰就是这个原因。
     */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);

        int cxCard = this.width / 2;
        int cardW = 372;
        int cardX = cxCard - cardW / 2;
        int cardY = 2;
        int cardH = this.height - 4;
        NavGuiStyle.body(g, cardX, cardY, cardW, cardH);
        if (this.title != null) {
            NavGuiStyle.header(g, cardX + 2, cardY + 2, cardW - 4, 16,
                    this.title.getString(), this.font);
        }
        if (cardH > 30) {
            NavGuiStyle.insetPanel(g, cardX + 6, cardY + 21, cardW - 12, cardH - 27);
        }
        // 页签行下方分隔线
        g.fill(cardX + 6, cardY + 50, cardX + cardW - 6, cardY + 51, 0xFF8A8A8A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int cx = this.width / 2;

        // 数值凹槽：让数字像是填在输入框里（各行 y 由 rowY 决定，越界的跳过）
        for (int i = 0; i < 10; i++) {
            int ry = rowY(i);
            if (ry < 40 || ry > this.height - 70) {
                continue;
            }
            NavGuiStyle.insetPanel(g, cx + 62, ry - 4, 74, 15);
        }
        // 底栏分隔线：按钮区与内容区分开
        g.fill(cx - 180, this.height - 56, cx + 180, this.height - 55, 0xFF8A8A8A);
        g.fill(cx - 180, this.height - 55, cx + 180, this.height - 54, NavGuiStyle.BEVEL);
        // 标题已经画在标题条里，这里不再重复（原来那行白色文字在米色面板上会看不清）

        if (page == 2) {
            g.drawString(this.font, "标记样式", cx - 160, rowY(0), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "显示方框", cx - 160, rowY(1), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "显示雷达", cx - 160, rowY(2), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "调试行", cx - 160, rowY(3), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "其他导航点", cx - 160, rowY(4), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "主色", cx - 160, rowY(5), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "#" + NavigationHudOverlay.getColorMainHex(),
                    cx + 20, rowY(5), NavGuiStyle.GREEN, false);
            g.drawString(this.font, "次色", cx - 160, rowY(6), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "#" + NavigationHudOverlay.getColorDimHex(),
                    cx + 20, rowY(6), NavGuiStyle.GREEN, false);
        } else if (page == 4) {
            g.drawString(this.font, "编队显示", cx - 160, rowY(0), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "信标显示", cx - 160, rowY(1), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "编队颜色", cx - 160, rowY(2), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "#" + NavigationHudOverlay.getColorFleetHex(),
                    cx + 20, rowY(2), 0x4FA3FF, false);
            g.drawString(this.font, "信标颜色", cx - 160, rowY(3), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "#" + NavigationHudOverlay.getColorBeaconHex(),
                    cx + 20, rowY(3), 0x00E5FF, false);

            g.drawString(this.font, "绿色=同频道  蓝色=异频道  青色=信标",
                    cx - 160, rowY(5), NavGuiStyle.MUTED, false);
        } else if (page == 3) {
            g.drawString(this.font, "显示全部路点", cx - 160, rowY(0), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "显示航线站点", cx - 160, rowY(1), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "海平面线", cx - 160, rowY(2), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "其他路点色", cx - 160, rowY(3), NavGuiStyle.TEXT, false);
            g.drawString(this.font, "#" + NavigationHudOverlay.getColorOtherHex(),
                    cx + 20, rowY(3), 0xFFA500, false);

            // 路点列表：右键切换隐藏
            var list = NavigationHudOverlay.getCachedWaypoints();
            g.drawString(this.font, "路点列表（右键隐藏/恢复）", cx - 160, listTop - 12, NavGuiStyle.MUTED, false);
            if (list.isEmpty()) {
                g.drawString(this.font, "（没有读到 Xaero 路点）", cx - 160, listTop, NavGuiStyle.MUTED, false);
            } else {
                int maxRows = Math.max(1, (this.height - listTop - 46) / ROW_H);
                for (int i = 0; i < list.size() && i < maxRows; i++) {
                    var wp = list.get(i);
                    boolean hidden = NavigationHudOverlay.isHiddenWaypoint(wp.name());
                    int color = hidden ? NavGuiStyle.DISABLED : NavGuiStyle.TEXT;
                    String line = wp.name() + "   (" + wp.x() + ", " + wp.z() + ")";
                    g.drawString(this.font, line, cx - 160, listTop + i * ROW_H, color, false);
                    if (hidden) {
                        g.drawString(this.font, "[已隐藏]", cx + 100, listTop + i * ROW_H, NavGuiStyle.ACCENT, false);
                    }
                }
            }
        } else {
            for (int i = 0; i < nums.size(); i++) {
                NumRow row = nums.get(i);
                g.drawString(this.font, row.label, cx - 160, rowY(i), NavGuiStyle.TEXT, false);
                g.drawString(this.font, row.display(), cx + 70, rowY(i), NavGuiStyle.GREEN, false);
            }
        }

        int footY = this.height - 42;
        g.drawCenteredString(this.font, "改动实时生效，点「保存并关闭」写入配置", cx, footY, NavGuiStyle.MUTED);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (page == 3 && my >= listTop && button == 1) {
            var list = NavigationHudOverlay.getCachedWaypoints();
            int idx = (int) ((my - listTop) / ROW_H);
            if (idx >= 0 && idx < list.size()) {
                toggleHidden(list.get(idx).name());
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        NavigationHudOverlay.endCalibration();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
