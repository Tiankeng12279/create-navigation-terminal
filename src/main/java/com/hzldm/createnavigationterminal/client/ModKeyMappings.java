package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.network.SetNavigationTargetPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键
 *   F9 —— 打开 HUD 配置 / 校准界面
 *   K  —— 把准星正对的路点设为导航目标
 */
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {

    public static final KeyMapping OPEN_CALIBRATION = new KeyMapping(
            "key.create_navigation_terminal.calibrate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "key.categories.create_navigation_terminal");

    /** 把准星正对的路点设为导航目标 */
    public static final KeyMapping LOCK_TARGET = new KeyMapping(
            "key.create_navigation_terminal.lock_target",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.create_navigation_terminal");

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CALIBRATION);
        event.register(LOCK_TARGET);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            // 消耗掉排队按键，避免关闭界面后立刻又被触发
            while (OPEN_CALIBRATION.consumeClick()) {
                // ignore
            }
            while (LOCK_TARGET.consumeClick()) {
                // ignore
            }
            return;
        }

        while (OPEN_CALIBRATION.consumeClick()) {
            mc.setScreen(new HudCalibrationScreen());
        }

        while (LOCK_TARGET.consumeClick()) {
            lockAimedWaypoint(mc);
        }
    }

    /** 把准星正对的路点设为导航目标 */
    private static void lockAimedWaypoint(Minecraft mc) {
        var data = NavigationHudOverlay.getCurrent();
        if (data == null || mc.player == null || mc.level == null) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§7HUD 未激活（需坐在已绑定的座椅上）"), true);
            return;
        }
        var wp = NavigationHudOverlay.getAimedWaypoint();
        if (wp == null) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§7准星 12° 内没有可选路点"), true);
            return;
        }

        String dim = mc.level.dimension().location().toString();
        PacketDistributor.sendToServer(new SetNavigationTargetPayload(
                data.terminalPos(), wp.name(), wp.x(), wp.y(), wp.z(), dim));

        mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a目标已切换为：" + wp.name()), true);
        CreateNavigationTerminal.LOGGER.info(
                "[HUD] 按键锁定目标：{} ({},{},{}) @ {}", wp.name(), wp.x(), wp.y(), wp.z(), dim);
    }
}
