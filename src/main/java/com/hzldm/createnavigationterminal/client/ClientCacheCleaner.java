package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.integration.xaero.XaeroMapSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 客户端世界切换时的静态缓存清理。
 *
 * <p>退出存档 / 断开连接后，HUD 的路点缓存、去重键、Xaero 地形 region 缓存都是上一个世界的。
 * 不清的话，新存档里 HUD 可能一直显示旧路点、地图显示旧地形，或者干脆一片空白
 * —— 用户看到的就是「换存档后导航仪失效」。
 */
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID, value = Dist.CLIENT)
public final class ClientCacheCleaner {

    private ClientCacheCleaner() {
    }

    /** 断开连接（含退出存档）：清空 HUD 与地形缓存 */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear("logging-out");
    }

    /** 客户端维度卸载（切换维度 / 回主菜单）：同样清理 */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        clear("client-level-unload");
    }

    private static void clear(String reason) {
        NavigationHudOverlay.clearAllCaches();
        XaeroMapSource.invalidate();
        CreateNavigationTerminal.LOGGER.debug("[Nav] 客户端缓存已清理（{}）", reason);
    }
}
