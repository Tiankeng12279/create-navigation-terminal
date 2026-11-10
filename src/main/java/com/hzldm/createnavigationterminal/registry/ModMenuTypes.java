package com.hzldm.createnavigationterminal.registry;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * MenuType 注册
 *
 * NeoForge 1.21.1 用 IMenuTypeExtension.create，
 * 可经 RegistryFriendlyByteBuf 传递 BlockPos 等额外数据。
 */
public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreateNavigationTerminal.MODID);

    /** 导航终端配置页（无槽位；目标与参数都存 BlockEntity） */
    public static final Supplier<MenuType<NavigationTerminalMenu>> NAVIGATION_TERMINAL =
            MENUS.register("navigation_terminal",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                        BlockPos pos = data.readBlockPos();
                        return new NavigationTerminalMenu(windowId, inv, pos);
                    }));
}
