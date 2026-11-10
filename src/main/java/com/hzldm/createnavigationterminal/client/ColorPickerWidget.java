package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * 简易 HSV 取色器
 *
 * 左：饱和度/明度面板（横向=饱和度，纵向=明度，从下往上变亮）
 * 右：色相竖条
 * 点哪选哪，也可拖动。
 */
public class ColorPickerWidget extends AbstractWidget {

    private static final int HUE_W = 14;

    private float hue;      // 0..1
    private float sat;      // 0..1
    private float val;      // 0..1

    private final IntConsumer onChange;
    private boolean dragSV;
    private boolean dragHue;

    public ColorPickerWidget(int x, int y, int w, int h, int initialRgb, IntConsumer onChange) {
        super(x, y, w, h, Component.literal("color picker"));
        this.onChange = onChange;
        setFromRgb(initialRgb);
    }

    public void setFromRgb(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float max = Math.max(r, Math.max(g, b)) / 255f;
        float min = Math.min(r, Math.min(g, b)) / 255f;
        float d = max - min;
        this.val = max;
        this.sat = max <= 0f ? 0f : d / max;
        if (d <= 0f) {
            this.hue = 0f;
        } else if (max == r / 255f) {
            this.hue = ((g - b) / 255f / d) % 6f / 6f;
        } else if (max == g / 255f) {
            this.hue = ((b - r) / 255f / d + 2f) / 6f;
        } else {
            this.hue = ((r - g) / 255f / d + 4f) / 6f;
        }
        if (this.hue < 0) {
            this.hue += 1f;
        }
    }

    public int currentRgb() {
        return hsvToRgb(hue, sat, val);
    }

    private int svWidth() {
        return this.width - HUE_W - 4;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int svW = svWidth();
        int svH = this.height;

        // 饱和度/明度面板：逐行渐变
        for (int py = 0; py < svH; py++) {
            float v = 1f - (float) py / Math.max(1, svH - 1);
            for (int px = 0; px < svW; px += 2) {
                float s = (float) px / Math.max(1, svW - 1);
                int col = 0xFF000000 | hsvToRgb(hue, s, v);
                g.fill(getX() + px, getY() + py, getX() + Math.min(px + 2, svW), getY() + py + 1, col);
            }
        }

        // 色相条
        int hueX = getX() + svW + 4;
        for (int py = 0; py < svH; py++) {
            float h = (float) py / Math.max(1, svH - 1);
            int col = 0xFF000000 | hsvToRgb(h, 1f, 1f);
            g.fill(hueX, getY() + py, hueX + HUE_W, getY() + py + 1, col);
        }

        // SV 光标
        int cx = getX() + (int) (sat * (svW - 1));
        int cy = getY() + (int) ((1f - val) * (svH - 1));
        g.fill(cx - 3, cy, cx + 4, cy + 1, 0xFF000000);
        g.fill(cx, cy - 3, cx + 1, cy + 4, 0xFF000000);
        g.fill(cx - 2, cy + 1, cx + 3, cy + 2, 0xFFFFFFFF);
        g.fill(cx + 1, cy - 2, cx + 2, cy + 3, 0xFFFFFFFF);

        // 色相光标
        int hy = getY() + (int) (hue * (svH - 1));
        g.fill(hueX - 1, hy - 1, hueX + HUE_W + 1, hy + 2, 0xFFFFFFFF);

        // 边框
        g.renderOutline(getX() - 1, getY() - 1, this.width + 2, this.height + 2, 0xFF808080);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        int svW = svWidth();
        int hueX = getX() + svW + 4;

        if (mx >= getX() && mx < getX() + svW && my >= getY() && my < getY() + this.height) {
            dragSV = true;
            updateSV(mx, my, svW);
            return true;
        }
        if (mx >= hueX && mx < hueX + HUE_W && my >= getY() && my < getY() + this.height) {
            dragHue = true;
            updateHue(my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        int svW = svWidth();
        if (dragSV) {
            updateSV(mx, my, svW);
            return true;
        }
        if (dragHue) {
            updateHue(my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragSV = false;
        dragHue = false;
        return false;
    }

    private void updateSV(double mx, double my, int svW) {
        sat = clamp01((float) ((mx - getX()) / Math.max(1, svW - 1)));
        val = clamp01(1f - (float) ((my - getY()) / Math.max(1, this.height - 1)));
        if (onChange != null) {
            onChange.accept(currentRgb());
        }
    }

    private void updateHue(double my) {
        hue = clamp01((float) ((my - getY()) / Math.max(1, this.height - 1)));
        if (onChange != null) {
            onChange.accept(currentRgb());
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** HSV → RGB（返回 0xRRGGBB，不含 alpha） */
    public static int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6f) % 6;
        float f = h * 6f - (int) (h * 6f);
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);
        float r;
        float gg;
        float b;
        switch (i) {
            case 0 -> {
                r = v;
                gg = t;
                b = p;
            }
            case 1 -> {
                r = q;
                gg = v;
                b = p;
            }
            case 2 -> {
                r = p;
                gg = v;
                b = t;
            }
            case 3 -> {
                r = p;
                gg = q;
                b = v;
            }
            case 4 -> {
                r = t;
                gg = p;
                b = v;
            }
            default -> {
                r = v;
                gg = p;
                b = q;
            }
        }
        return ((int) (r * 255) << 16) | ((int) (gg * 255) << 8) | (int) (b * 255);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
