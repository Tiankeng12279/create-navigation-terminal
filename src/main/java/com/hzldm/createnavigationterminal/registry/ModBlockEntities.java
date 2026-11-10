package com.hzldm.createnavigationterminal.registry;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** BlockEntity 注册 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateNavigationTerminal.MODID);

    public static final Supplier<BlockEntityType<NavigationTerminalBlockEntity>> NAVIGATION_TERMINAL =
            BLOCK_ENTITIES.register(
                    "navigation_terminal",
                    () -> BlockEntityType.Builder.of(
                            NavigationTerminalBlockEntity::new,
                            ModBlocks.NAVIGATION_TERMINAL.get()
                    ).build(null)
            );
}
