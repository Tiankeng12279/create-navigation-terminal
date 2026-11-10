package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 与 {@link NavGuiStyle} 配套的按钮：米色底 + 深棕描边 + 顶部高光，
 * 悬停变亮、禁用变灰，文字居中。外观与参考项目（Create 行情牌风格）一致。
 */
public class NavButton extends Button {

    /** 图标绘制器：给左上角坐标与颜色，自己画 14×14 以内 */
    public interface IconPainter {
        void paint(GuiGraphics g, int x, int y, int color);
    }

    private IconPainter icon;
    /** 开关态：true 时按钮变绿、图标反白（例如"设置面板已打开"） */
    private boolean toggled;

    /** 用图标代替文字（图标会在按钮内居中） */
    public NavButton withIcon(IconPainter painter) {
        this.icon = painter;
        return this;
    }

    public NavButton withToggled(boolean value) {
        this.toggled = value;
        return this;
    }

    protected NavButton(int x, int y, int w, int h, Component message, OnPress onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
    }

    /** 替换 {@code Button.builder(...).bounds(x,y,w,h).build()} 的简写 */
    public static NavButton make(Component message, OnPress onPress, int x, int y, int w, int h) {
        return new NavButton(x, y, w, h, message, onPress);
    }

    /** 链式入口（不能用 builder 这个名字：会与 Button.builder 的静态签名冲突） */
    public static Builder of(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    /** 只实现本项目用到的链式方法（pos/size/width/height/bounds/tooltip） */
    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private net.minecraft.client.gui.components.Tooltip tooltip;

        private Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            return pos(x, y).size(width, height);
        }

        public Builder tooltip(net.minecraft.client.gui.components.Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public NavButton build() {
            NavButton button = new NavButton(x, y, width, height, message, onPress);
            if (tooltip != null) {
                button.setTooltip(tooltip);
            }
            return button;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        boolean hover = this.isHoveredOrFocused();
        int x = getX();
        int y = getY();
        int w = this.width;
        int h = this.height;

        // 底面（2px 立体边）：外描边 → 亮边（上/左）→ 暗边（下/右）→ 面色
        int face = !this.active ? NavGuiStyle.DISABLED
                : this.toggled ? NavGuiStyle.GREEN
                : hover ? NavGuiStyle.BUTTON_FACE_HOVER : NavGuiStyle.BUTTON_FACE;
        int ring = this.toggled ? 0xFF14523A : NavGuiStyle.BORDER;
        int ringLight = this.toggled ? 0xFF4FC48A : 0xFFFFFFFF;
        int ringDark = this.toggled ? 0xFF0C3A28 : 0xFF6A6A6A;
        g.fill(x, y, x + w, y + h, ring);
        if (w > 4 && h > 4) {
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, ringLight);
            g.fill(x + 2, y + 2, x + w - 1, y + h - 1, ringDark);
            g.fill(x + 2, y + 2, x + w - 2, y + h - 2, face);
            // 内面高光，模拟参考图按钮那种上亮下暗的渐变
            g.fill(x + 2, y + 2, x + w - 2, y + 3, 0xFFC4C4C4);
        }
        int color = !this.active ? 0xFFA8A8A8
                : this.toggled ? 0xFFFFFFFF : NavGuiStyle.BODY_TEXT;
        if (this.icon != null) {
            int iw = Math.min(NavIcons.SIZE, w - 2);
            int ih = Math.min(NavIcons.SIZE, h - 2);
            int ix = x + (w - iw) / 2;
            int iy = y + (h - ih) / 2;
            this.icon.paint(g, ix, iy, color);
        } else {
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    x + w / 2, y + (h - 8) / 2, color);
        }
    }
}
