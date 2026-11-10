package com.hzldm.createnavigationterminal;

import com.hzldm.createnavigationterminal.network.ModPayloads;
import com.hzldm.createnavigationterminal.registry.ModBlockEntities;
import com.hzldm.createnavigationterminal.registry.ModBlocks;
import com.hzldm.createnavigationterminal.registry.ModCreativeTabs;
import com.hzldm.createnavigationterminal.registry.ModDataComponents;
import com.hzldm.createnavigationterminal.registry.ModItems;
import com.hzldm.createnavigationterminal.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create: Navigation Terminal（机械动力：导航终端）
 *
 * 功能：
 *  - 导航终端方块：读取客户端本地 Xaero 地图标记点作为导航目标
 *  - 按「方块到目标的距离」输出 0-15 可调红石信号
 *  - 进入目标指定半径后按配置停止导航（静默断电 / 自动清除目标 / 保持输出）
 *
 * 设计要点：
 *  - 标记点在客户端（Xaero 数据存于 .minecraft/XaeroWaypoints），服务端不可见，
 *    因此「选点」是客户端行为，服务端只接收结果并校验
 *  - Xaero 无官方 API，通过反射访问；缺失或版本漂移时静默降级，不影响方块本身
 */
@Mod(CreateNavigationTerminal.MODID)
public class CreateNavigationTerminal {

    public static final String MODID = "create_navigation_terminal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public CreateNavigationTerminal(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        com.hzldm.createnavigationterminal.registry.ModRecipes.SERIALIZERS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        // 网络 payload 注册（必须，否则菜单打开/参数同步会抛异常）
        modEventBus.addListener(ModPayloads::register);

        // 客户端配置：config/create_navigation_terminal-client.toml
        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.CLIENT,
                com.hzldm.createnavigationterminal.config.ClientConfig.SPEC);
    }
}
