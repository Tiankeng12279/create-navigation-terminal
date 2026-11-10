package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.network.SetCardSettingsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 给导航点卡里的航线起名：居中弹出的小对话框（钢灰风，与主界面同一套 {@link NavGuiStyle}）。
 *
 * <p>比把输入框塞进左栏更好用：空间足够、有明确的「确定 / 取消」，回车确认、Esc 取消。
 * 关闭后自动回到打开它的那一层界面（通常是导航终端）。
 */
public class CardNameScreen extends Screen {

    private static final int W = 200;
    private static final int H = 88;

    private final Screen parent;
    private final BlockPos pos;
    private final String initialName;
    private final boolean readOnly;
    private EditBox box;

    public CardNameScreen(Screen parent, BlockPos pos, String initialName, boolean readOnly) {
        super(Component.literal("\u547d\u540d\u822a\u7ebf"));
        this.parent = parent;
        this.pos = pos;
        this.initialName = initialName == null ? "" : initialName;
        this.readOnly = readOnly;
    }

    @Override
    protected void init() {
        int x0 = (this.width - W) / 2;
        int y0 = (this.height - H) / 2;

        this.box = new EditBox(this.font, x0 + 20, y0 + 34, W - 40, 16,
                Component.literal("\u822a\u7ebf\u540d"));
        this.box.setMaxLength(32);
        this.box.setValue(this.initialName);
        this.box.setFocused(true);
        this.addRenderableWidget(this.box);
        this.setFocused(this.box);

        this.addRenderableWidget(NavButton.of(Component.literal("\u786e\u5b9a"), b -> this.confirm())
                .bounds(x0 + 20, y0 + 60, 76, 16).build());
        this.addRenderableWidget(NavButton.of(Component.literal("\u53d6\u6d88"), b -> this.onClose())
                .bounds(x0 + W - 96, y0 + 60, 76, 16).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x0 = (this.width - W) / 2;
        int y0 = (this.height - H) / 2;
        NavGuiStyle.body(g, x0, y0, W, H);
        NavGuiStyle.header(g, x0 + 1, y0 + 1, W - 2, 16,
                "\u547d\u540d\u822a\u7ebf", this.font);
        NavGuiStyle.insetPanel(g, x0 + 6, y0 + 20, W - 12, H - 26);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x0 = (this.width - W) / 2;
        int y0 = (this.height - H) / 2;
        g.drawString(this.font, "\u8f93\u5165\u540d\u5b57\u540e\u56de\u8f66\u6216\u70b9\u786e\u5b9a",
                x0 + 20, y0 + 22, NavGuiStyle.MUTED, false);
        if (this.readOnly) {
            g.drawString(this.font, "\uff08\u5f53\u524d\u4e3a\u53ea\u8bfb\uff09",
                    x0 + W - 78, y0 + 22, NavGuiStyle.AMBER, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.confirm();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        String value = this.box == null || this.box.getValue() == null ? "" : this.box.getValue().trim();
        PacketDistributor.sendToServer(new SetCardSettingsPayload(this.pos, value, this.readOnly));
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[GUI] 提交卡片名：'{}'", value);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal(value.isEmpty()
                            ? "\u5361\u7247\u540d\u5df2\u6e05\u7a7a"
                            : "\u5361\u7247\u540d\u5df2\u4fdd\u5b58\uff1a" + value), true);
        }
        this.onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
