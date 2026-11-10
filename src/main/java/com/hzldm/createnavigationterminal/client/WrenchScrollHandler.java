package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import com.hzldm.createnavigationterminal.network.ScrollFacePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 用扳手选中某个面之后，滚轮切换该面的红石输出通道。
 *
 * 交互流程：
 * <pre>
 * 1. 潜行 + 扳手右键终端的某个面 → 该面进入「选中」状态（10 秒有效）
 * 2. 准星指着该终端时滚轮 → 切换这个面的通道，聊天栏显示结果
 * </pre>
 */
@EventBusSubscriber(modid = "create_navigation_terminal", value = Dist.CLIENT)
public final class WrenchScrollHandler {

    private WrenchScrollHandler() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult bhr)) {
            return;
        }
        if (!(mc.level.getBlockEntity(bhr.getBlockPos())
                instanceof NavigationTerminalBlockEntity terminal)) {
            return;
        }
        if (terminal.getEditingFace() < 0) {
            return;
        }
        double dy = event.getScrollDeltaY();
        if (dy == 0.0) {
            return;
        }
        int delta = dy > 0 ? 1 : -1;
        PacketDistributor.sendToServer(
                new ScrollFacePayload(bhr.getBlockPos(), delta));
        // 本地也立刻算一遍，让聊天栏那次反馈即时（服务端会再发正式值）
        event.setCanceled(true);
    }
}
