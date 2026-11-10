package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * GUI 调色板与绘制助手 —— Create「列车时刻表」钢灰风。
 *
 * <p>取色来自参考截图逐点采样：外框 {@code #C6C6C6}、内区 {@code #303030}、
 * 深边框 {@code #404040}、强调线 {@code #BD905A}、绿 {@code #218756}、灰按钮 {@code #8B8B8B}。
 *
 * <p>为了让"像"变成"细节到位"，这里补了参考图里有、之前缺的四层：
 * <ol>
 *   <li><b>机身 3D 边</b>：外 1px 深描边 → 上/左白高光 → 下/右暗边；</li>
 *   <li><b>内嵌面板</b>：深灰底 + 上/左内阴影 + 下/右亮边 + <b>竖向细纹</b>（模拟时刻表的底纹）；</li>
 *   <li><b>左侧强调脊线</b>：2px 铜橙竖线，对应时刻表中间那条时刻线；</li>
 *   <li><b>控件</b>：按钮 2px 立体边 + 内面高光 + 焦点框；槽位按原版（内阴影 + 外高光）。</li>
 * </ol>
 */
public final class NavGuiStyle {

    // ---- 机身与边框 ----
    public static final int BODY = 0xFFC6C6C6;
    public static final int BODY_SHADE = 0xFF8A8A8A;
    public static final int BORDER = 0xFF373737;
    public static final int BEVEL = 0xFFFFFFFF;

    // ---- 面板 ----
    public static final int PANEL = 0xFF303030;
    public static final int PANEL_ALT = 0xFF3A3A3A;
    public static final int PANEL_HOVER = 0xFF474747;
    public static final int PANEL_GRAIN = 0xFF343434;   // 竖向细纹
    public static final int INNER = 0xFF1C1C1C;
    public static final int GRID = 0xFF4C4C4C;

    // ---- 文字 ----
    public static final int TEXT = 0xFFE6E6E6;
    public static final int MUTED = 0xFF9C9C9C;
    public static final int BODY_TEXT = 0xFF2E2E2E;

    // ---- 强调与状态 ----
    public static final int ACCENT = 0xFFBD905A;
    public static final int ACCENT_BRIGHT = 0xFFD6A970;
    public static final int GREEN = 0xFF218756;
    public static final int AMBER = 0xFFC08A3E;
    public static final int DISABLED = 0xFF6E6E6E;

    // ---- 控件 ----
    public static final int SLOT_BG = 0xFF8B8B8B;
    public static final int BUTTON_FACE = 0xFF8B8B8B;
    public static final int BUTTON_FACE_HOVER = 0xFFA0A0A0;
    public static final int WASH_CURRENT = 0xFF4A3A24;
    public static final int WASH_INPUT = 0xFF33452F;
    public static final int WASH_OUTPUT = 0xFF4A3A24;

    private NavGuiStyle() {
    }

    // ================= 基础 =================

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** 竖向细纹：参考图内区那种若隐若现的底纹，1px 线每 8px 一条 */
    public static void grain(GuiGraphics g, int x, int y, int w, int h) {
        for (int gx = x + 4; gx < x + w - 2; gx += 8) {
            g.fill(gx, y + 2, gx + 1, y + h - 2, PANEL_GRAIN);
        }
    }

    /** 面板左侧铜橙脊线（对应时刻表的时刻线） */
    public static void accentSpine(GuiGraphics g, int x, int y, int h) {
        g.fill(x, y, x + 2, y + h, ACCENT);
        g.fill(x, y, x + 1, y + h, ACCENT_BRIGHT);
    }

    // ================= 大件 =================

    /** 整个 GUI 的机身：投影 + 浅钢灰 + 深描边 + 白高光 + 暗边 */
    public static void body(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x66000000);
        g.fill(x, y, x + w, y + h, BODY);
        frame(g, x, y, w, h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, BEVEL);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, BEVEL);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BODY_SHADE);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BODY_SHADE);
    }

    /**
     * 深灰内嵌面板（可选底纹 + 左侧强调脊线）。
     *
     * @param spine 是否画左侧铜橙脊线
     */
    public static void insetPanel(GuiGraphics g, int x, int y, int w, int h, boolean spine) {
        g.fill(x, y, x + w, y + h, PANEL);
        if (w > 8 && h > 8) {
            grain(g, x, y, w, h);
        }
        // 上/左内阴影，下/右亮边 → 凹陷感
        g.fill(x, y, x + w, y + 1, INNER);
        g.fill(x, y, x + 1, y + h, INNER);
        g.fill(x, y + h - 1, x + w, y + h, GRID);
        g.fill(x + w - 1, y, x + w, y + h, GRID);
        if (spine && h > 6) {
            accentSpine(g, x + 1, y + 1, h - 2);
        }
    }

    public static void insetPanel(GuiGraphics g, int x, int y, int w, int h) {
        insetPanel(g, x, y, w, h, false);
    }

    /** 凸起面板：浅色块 + 上/左亮、下/右暗 */
    public static void raisedPanel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, GRID);
        g.fill(x, y, x + 1, y + h, GRID);
        g.fill(x, y + h - 1, x + w, y + h, INNER);
        g.fill(x + w - 1, y, x + w, y + h, INNER);
    }

    /** 原版风格物品槽：灰面 + 上/左内阴影 + 下/右外高光 */
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT_BG);
        g.fill(x, y, x + 18, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 18, 0xFF373737);
        g.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 2, 0xFF9E9E9E);
        g.fill(x + 1, y + 1, x + 2, y + 17, 0xFF9E9E9E);
    }

    /** 标题栏：浅灰、居中深色标题、顶部白高光、底部深分隔线 */
    public static void header(GuiGraphics g, int x, int y, int w, int h, String text, Font font) {
        g.fill(x, y, x + w, y + h, BODY);
        g.fill(x, y, x + w, y + 1, BEVEL);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        if (text != null && !text.isEmpty()) {
            int tw = font.width(text);
            g.drawString(font, text, x + (w - tw) / 2, y + (h - 8) / 2, BODY_TEXT, false);
        }
    }

    /** 面板内的小标题：浅色字 + 下面一条分隔线（替代裸文字标签） */
    public static void sectionTitle(GuiGraphics g, Font font, int x, int y, int w, String text) {
        int tw = font.width(text);
        g.drawString(font, text, x + (w - tw) / 2, y, TEXT, false);
        g.fill(x + 2, y + 11, x + w - 2, y + 12, GRID);
    }

    public static void divider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, GRID);
    }

    /** 竖向滚动条：深轨道 + 铜橙滑块 + 滑块高光 */
    public static void scrollbar(GuiGraphics g, int x, int y, int h, int thumbY, int thumbH) {
        g.fill(x, y, x + 3, y + h, INNER);
        g.fill(x + 1, thumbY, x + 3, thumbY + thumbH, ACCENT);
        g.fill(x + 1, thumbY, x + 2, thumbY + thumbH, ACCENT_BRIGHT);
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
