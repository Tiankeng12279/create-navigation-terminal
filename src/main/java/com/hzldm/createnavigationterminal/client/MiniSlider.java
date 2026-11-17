package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 左侧图标列下方的小滑块（18×12）。
 *
 * <p>形态参照那一列图标按钮下方的滑块：一条细轨 + 一个方形手柄，
 * 手柄在左 = 关，在右 = 开；支持点击与拖动。
 */
public class MiniSlider extends AbstractWidget {

    private static final int FRAME = 0xFF1C1C1C;
    private static final int TRACK = 0xFF6E6E6E;
    private static final int TRACK_ON = 0xFFBD905A;
    private static final int KNOB = 0xFFD8D8D8;

    private boolean on;
    private final Consumer<Boolean> listener;

    public MiniSlider(int x, int y, boolean initial, String tooltip, Consumer<Boolean> listener) {
        super(x, y, 18, 12, Component.empty());
        this.on = initial;
        this.listener = listener;
        if (tooltip != null && !tooltip.isEmpty()) {
            setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
    }

    public boolean isOn() {
        return on;
    }

    private void setFromMouse(double mouseX) {
        boolean next = mouseX >= getX() + getWidth() / 2.0;
        if (next != on) {
            on = next;
            if (listener != null) {
                listener.accept(on);
            }
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // 轨道（居中细条）
        int trackY = y + h / 2 - 2;
        g.fill(x + 1, trackY, x + w - 1, trackY + 4, FRAME);
        g.fill(x + 2, trackY + 1, x + w - 2, trackY + 3, on ? TRACK_ON : TRACK);

        // 手柄（方形，随状态左右滑动）
        int knobW = 7;
        int knobX = on ? x + w - knobW - 1 : x + 1;
        g.fill(knobX, y + 2, knobX + knobW, y + h - 2, FRAME);
        g.fill(knobX + 1, y + 3, knobX + knobW - 1, y + h - 3,
                this.isHoveredOrFocused() ? 0xFFFFFFFF : KNOB);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setFromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        setFromMouse(mouseX);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
