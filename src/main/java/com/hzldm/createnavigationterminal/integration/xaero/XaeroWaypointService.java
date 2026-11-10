package com.hzldm.createnavigationterminal.integration.xaero;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Xaero 路点服务（客户端）
 *
 * 约定：所有方法必须在客户端主线程调用（内部用 Minecraft#execute 排队）。
 * Xaero 不存在或反射漂移时返回空列表，由界面降级为提示文案。
 */
public final class XaeroWaypointService {

    private static final Logger LOGGER = LoggerFactory.getLogger("NavTerminal-Xaero");

    private XaeroWaypointService() {
    }

    /**
     * 读取玩家当前维度的全部路点
     *
     * @param callback 主线程回调，参数为当前维度路点列表（可能为空）
     */
    public static void listWaypointsOnClient(Consumer<List<XaeroReflect.RawWp>> callback) {
        Minecraft.getInstance().execute(() -> {
            // 先查 mod 列表（不触碰 xaero.* 类型），确认装了再触发 XaeroWaypointApi 加载，
            // 否则 JVM 解析到 xaero.* 会抛 NoClassDefFoundError
            if (!XaeroWaypointApi.isAvailable()) {
                LOGGER.debug("[NavTerminal] Xaero 未安装或不在世界内，导航点列表返回空");
                callback.accept(List.of());
                return;
            }
            callback.accept(XaeroWaypointApi.listWaypoints());
        });
    }
}
