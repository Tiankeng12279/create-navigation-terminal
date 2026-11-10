package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 界面图标：全部用 {@code GuiGraphics.fill} 逐像素画出来（不依赖贴图，避免额外资源与 mipmap 问题）。
 *
 * <p>尺寸统一按 14×14 设计，调用方给左上角坐标，内部自行居中。
 * 线宽 1px、留 1~2px 内边距，配合 {@link NavGuiStyle} 的钢灰配色显得"拟物"。
 */
public final class NavIcons {

    public static final int SIZE = 14;

    private NavIcons() {
    }

    /** 圆角方框：四角各削掉 1px，模仿参考图那种圆角面板 */
    private static void roundedFrame(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c);            // 上
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);    // 下
        g.fill(x, y + 2, x + 1, y + h - 2, c);            // 左
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, c);    // 右
        g.fill(x + 1, y + 1, x + 2, y + 2, c);            // 四角补丁
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, c);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, c);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, c);
    }

    /** 三横杠（设置 / 菜单）：只画横杠，不带外框（外框由按钮本身提供） */
    public static void menu(GuiGraphics g, int x, int y, int c) {
        int bx = x + 2;
        int bw = SIZE - 4;
        g.fill(bx, y + 3, bx + bw, y + 5, c);
        g.fill(bx, y + 6, bx + bw, y + 8, c);
        g.fill(bx, y + 9, bx + bw, y + 11, c);
    }

    /** 向下箭头（写入 = 存进卡里） */
    public static void arrowDown(GuiGraphics g, int x, int y, int c) {
        int cx = x + SIZE / 2 - 1;      // 2px 宽箭杆
        g.fill(cx, y + 2, cx + 2, y + 8, c);
        g.fill(cx - 3, y + 8, cx + 5, y + 9, c);
        g.fill(cx - 2, y + 9, cx + 4, y + 10, c);
        g.fill(cx - 1, y + 10, cx + 3, y + 11, c);
        // 底部托盘
        g.fill(x + 2, y + 12, x + SIZE - 2, y + 13, c);
    }

    /** 向上箭头（读取 = 从卡里取出） */
    public static void arrowUp(GuiGraphics g, int x, int y, int c) {
        int cx = x + SIZE / 2 - 1;
        g.fill(cx, y + 5, cx + 2, y + 11, c);
        g.fill(cx - 3, y + 5, cx + 5, y + 6, c);
        g.fill(cx - 2, y + 4, cx + 4, y + 5, c);
        g.fill(cx - 1, y + 3, cx + 3, y + 4, c);
        // 底部托盘
        g.fill(x + 2, y + 12, x + SIZE - 2, y + 13, c);
    }

    /** 铅笔（改名） */
    public static void pencil(GuiGraphics g, int x, int y, int c) {
        for (int i = 0; i < 8; i++) {
            g.fill(x + 2 + i, y + 10 - i, x + 4 + i, y + 12 - i, c);
        }
        // 笔尖
        g.fill(x + 1, y + 11, x + 3, y + 13, c);
    }

    /** 锁（只读）：锁体 + 锁梁 */
    public static void lock(GuiGraphics g, int x, int y, int c) {
        int bx = x + 3;
        int by = y + 6;
        // 锁梁（开口朝下的方框上半）
        g.fill(bx + 1, y + 2, bx + 6, y + 3, c);
        g.fill(bx + 1, y + 3, bx + 2, y + 6, c);
        g.fill(bx + 5, y + 3, bx + 6, y + 6, c);
        // 锁体
        g.fill(bx, by, bx + 8, y + 12, c);
        // 钥匙孔（用背景色抠）
        g.fill(bx + 3, by + 2, bx + 5, by + 5, 0xFF000000);
        g.fill(bx + 3, by + 4, bx + 4, by + 6, 0xFF000000);
    }

    /** 对勾（确认） */
    public static void check(GuiGraphics g, int x, int y, int c) {
        for (int i = 0; i < 4; i++) {
            g.fill(x + 2 + i, y + 7 - i, x + 4 + i, y + 9 - i, c);
        }
        for (int i = 0; i < 7; i++) {
            g.fill(x + 6 + i, y + 4 + i, x + 8 + i, y + 6 + i, c);
        }
    }

    /** 叉（取消） */
    public static void cross(GuiGraphics g, int x, int y, int c) {
        for (int i = 0; i < 8; i++) {
            g.fill(x + 3 + i, y + 3 + i, x + 5 + i, y + 5 + i, c);
            g.fill(x + 10 - i, y + 3 + i, x + 12 - i, y + 5 + i, c);
        }
    }
}
