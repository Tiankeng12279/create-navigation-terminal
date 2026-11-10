package com.hzldm.createnavigationterminal.registry;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.block.NavigationTerminalBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 方块注册
 *
 * 导航终端为固定设备：硬度 -1（不可破坏）、高爆炸抗性，
 * 与 Create 系列机器方块手感一致，同时省掉 loot table。
 */
public class ModBlocks {

    private static final float EXPLODE_RESISTANCE = 3600000.0F;

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CreateNavigationTerminal.MODID);

    private static BlockBehaviour.Properties terminalProps(MapColor color) {
        return BlockBehaviour.Properties.of()
                .strength(-1.0F, EXPLODE_RESISTANCE)
                .mapColor(color);
    }

    /** 导航终端：读 Xaero 标记点 → 按距离/到达状态输出可调红石信号 */
    public static final Supplier<NavigationTerminalBlock> NAVIGATION_TERMINAL = BLOCKS.register(
            "navigation_terminal",
            () -> new NavigationTerminalBlock(terminalProps(MapColor.METAL))
    );
}
