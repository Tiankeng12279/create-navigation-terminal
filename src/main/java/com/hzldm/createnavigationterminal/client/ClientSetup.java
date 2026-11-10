package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.client.model.RotatedCuboidGeometryLoader;
import com.hzldm.createnavigationterminal.registry.ModMenuTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端初始化：MenuType → Screen 映射、自定义模型 loader 注册
 *
 * RegisterMenuScreensEvent / ModelEvent.RegisterGeometryLoaders 都在 MOD bus 上发送，
 * 必须用 Bus.MOD + Dist.CLIENT。
 */
@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    /** 支持任意角度元素旋转的 loader id（见 RotatedCuboidGeometryLoader） */
    public static final ResourceLocation ROTATED_CUBOIDS =
            ResourceLocation.fromNamespaceAndPath(CreateNavigationTerminal.MODID, "rotated_cuboids");

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.NAVIGATION_TERMINAL.get(), NavigationTerminalScreen::new);
    }

    /** 客户端初始化：装了 Ponderer 才安装内置思索场景 */
    @SubscribeEvent
    public static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(PonderSceneInstaller::install);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ROTATED_CUBOIDS, new RotatedCuboidGeometryLoader());
    }
}
