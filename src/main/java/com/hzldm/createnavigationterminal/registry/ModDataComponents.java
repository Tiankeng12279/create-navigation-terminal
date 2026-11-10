package com.hzldm.createnavigationterminal.registry;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.item.WaypointCardData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * DataComponent 注册
 *
 * 1.21.1 物品数据用 DataComponent（不再手写 NBT），
 * persistent Codec 负责存档、networkSynchronized StreamCodec 负责网络同步。
 */
public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateNavigationTerminal.MODID);

    /** 导航点卡的内容：若干 WpTarget */
    public static final Supplier<DataComponentType<WaypointCardData>> WAYPOINT_CARD =
            DATA_COMPONENTS.register("waypoint_card",
                    () -> DataComponentType.<WaypointCardData>builder()
                            .persistent(WaypointCardData.CODEC)
                            .networkSynchronized(WaypointCardData.STREAM_CODEC)
                            .build());
}
