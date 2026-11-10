package com.hzldm.createnavigationterminal.server;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import com.hzldm.createnavigationterminal.fleet.FleetRegistry;
import com.hzldm.createnavigationterminal.network.NavigationHudService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 世界切换时的静态状态清理。
 *
 * <p>本模组有多处 static 缓存（编队注册表、信标扫描结果、已加载终端集合、日志去重集合等）。
 * 它们只在「终端加载 / 卸载」时增量维护，**退出存档时不会自动归零**；
 * 换存档后旧数据仍在（同一维度键下甚至能命中旧坐标），表现为编队推送失效、
 * 信标不刷新、日志不再打印等「导航仪像坏了一样」的症状。
 *
 * <p>这里在服务端关闭 / 维度卸载时统一清一遍，保证每个存档都从干净状态开始。
 */
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID)
public final class WorldStateCleaner {

    private WorldStateCleaner() {
    }

    /** 服务端停止（退出存档 / 关服）：全部静态状态归零 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clearAll("server-stopped");
    }

    /** 维度卸载（切换维度 / 重载存档）：同样清理，避免残留指向已卸载的世界 */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            return;      // 客户端侧由 ClientCacheCleaner 处理（那里还要清 HUD 与 Xaero 缓存）
        }
        clearAll("level-unload");
    }

    private static void clearAll(String reason) {
        FleetRegistry.clear();
        NavigationHudService.clearCaches();
        NavigationTerminalBlockEntity.clearActive();
        NavigationTerminalBlockEntity.clearLogDedup();
        CreateNavigationTerminal.LOGGER.debug("[Nav] 静态状态已清理（{}）", reason);
    }
}
