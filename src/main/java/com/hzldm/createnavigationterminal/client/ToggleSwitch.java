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

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hover = this.isHoveredOrFocused();
        // 背景槽
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), NavGuiStyle.INNER);
        // 轨道
        int trackX = getX() + 3;
        int trackY = getY() + 5;
        int trackW = getWidth() - 6;
        g.fill(trackX, trackY, trackX + trackW, trackY + 6,
                on ? NavGuiStyle.ACCENT : NavGuiStyle.PANEL);
        // 旋钮
        int knobW = 6;
        int knobX = trackX + (on ? trackW - knobW : 0);
        int knobColor = hover ? NavGuiStyle.TEXT : NavGuiStyle.MUTED;
        g.fill(knobX, trackY - 2, knobX + knobW, trackY + 8, knobColor);
        // 边框
        g.fill(getX(), getY(), getX() + getWidth(), getY() + 1, NavGuiStyle.PANEL);
        g.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), NavGuiStyle.PANEL);
        g.fill(getX(), getY(), getX() + 1, getY() + getHeight(), NavGuiStyle.PANEL);
        g.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), NavGuiStyle.PANEL);
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
