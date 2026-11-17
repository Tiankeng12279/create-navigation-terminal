package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 到达后模式滑块（原版滑杆样式）。
 *
 * <p>左端 = 自动（到达即切下一站），右端 = 手动确认（按 R 前进）。
 */
public class AdvanceModeSlider extends AbstractSliderButton {

    private final Consumer<Boolean> onChange;

    public AdvanceModeSlider(int x, int y, int width, int height, boolean manual,
                             Consumer<Boolean> onChange) {
        super(x, y, width, height, Component.empty(), manual ? 1.0 : 0.0);
        this.onChange = onChange;
        updateMessage();
    }

    public boolean isManual() {
        return this.value >= 0.5;
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(isManual()
                ? "\u5230\u8fbe\u540e\uff1a\u624b\u52a8\u786e\u8ba4"
                : "\u5230\u8fbe\u540e\uff1a\u81ea\u52a8"));
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(isManual());
        }
    }
}
