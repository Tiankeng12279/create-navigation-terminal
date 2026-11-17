package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 18×16 的小滑块开关（左关右开），用于左侧图标列的下方。
 *
 * <p>自绘，不依赖原版按钮贴图：轨道用面板色，开时整条高亮、旋钮停在右侧。
 */
public class ToggleSwitch extends AbstractWidget {

    private boolean on;
    private final Consumer<Boolean> listener;

    public ToggleSwitch(int x, int y, boolean initial, String tooltip, Consumer<Boolean> listener) {
        super(x, y, 18, 16, Component.empty());
        this.on = initial;
        this.listener = listener;
        if (tooltip != null && !tooltip.isEmpty()) {
            setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean v) {
        this.on = v;
    }

    /** 与左侧图标按钮同一套观感：黑框 + 灰底 + 黑字形 */
    private static final int FRAME = 0xFF1C1C1C;
    private static final int FACE = 0xFF9C9C9C;
    private static final int FACE_HOVER = 0xFFB6B6B6;

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hover = this.isHoveredOrFocused();

        // 外框（黑） + 内面（灰），与上面几个图标按钮一致
        g.fill(x, y, x + w, y + h, FRAME);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? FACE_HOVER : FACE);

        // 字形：轨道 + 旋钮，全部用黑色，尺寸按 18×16 排版
        int trackX = x + 3;
        int trackY = y + 7;
        int trackW = w - 6;          // 12
        g.fill(trackX, trackY, trackX + trackW, trackY + 2, FRAME);   // 轨道底线
        g.fill(trackX, trackY - 1, trackX + 1, trackY + 3, FRAME);    // 左端档
        g.fill(trackX + trackW - 1, trackY - 1, trackX + trackW, trackY + 3, FRAME); // 右端档
        int knobW = 5;
        int knobX = on ? trackX + trackW - knobW : trackX;
        g.fill(knobX, trackY - 3, knobX + knobW, trackY + 5, FRAME);  // 旋钮
        if (!on) {
            // 关：旋钮内部挖空一点，和开状态区分更明显
            g.fill(knobX + 1, trackY - 2, knobX + knobW - 1, trackY + 4, FACE);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        on = !on;
        if (listener != null) {
            listener.accept(on);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
