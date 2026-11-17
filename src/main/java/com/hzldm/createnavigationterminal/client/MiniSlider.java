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

    /** 原版滑杆贴图（与选项界面同源，风格一致） */
    private static final net.minecraft.resources.ResourceLocation TRACK =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/slider");
    private static final net.minecraft.resources.ResourceLocation TRACK_HL =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/slider_highlighted");
    private static final net.minecraft.resources.ResourceLocation HANDLE =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/slider_handle");
    private static final net.minecraft.resources.ResourceLocation HANDLE_HL =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");

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
        boolean hover = this.isHoveredOrFocused();
        // 轨道：原版 200x20 贴图，按控件尺寸缩放绘制
        g.blit(on ? TRACK_HL : TRACK, getX(), getY(), getWidth(), getHeight(),
                0, 0, 200, 20, 200, 20);
        // 手柄：8x20 贴图，按状态贴左/右
        int handleW = Math.min(8, getWidth());
        int handleX = on ? getX() + getWidth() - handleW : getX();
        g.blit(hover ? HANDLE_HL : HANDLE, handleX, getY(), handleW, getHeight(),
                0, 0, 8, 20, 8, 20);
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
