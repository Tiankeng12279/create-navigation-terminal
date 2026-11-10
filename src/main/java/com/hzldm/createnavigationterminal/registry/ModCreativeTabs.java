package com.hzldm.createnavigationterminal.registry;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 创造模式物品栏 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateNavigationTerminal.MODID);

    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_navigation_terminal"))
                    .icon(() -> new ItemStack(ModItems.MOD_ICON.get()))
                    // 排在内置「刷怪蛋」之后，紧邻建筑方块类标签，不至于淹没在列表末尾
                    .withTabsBefore(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS)
                    .displayItems((params, output) -> {
                        output.accept(ModItems.NAVIGATION_TERMINAL.get());
                        output.accept(ModItems.WAYPOINT_CARD.get());
                    })
                    .build()
    );
}
