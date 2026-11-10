package com.hzldm.createnavigationterminal.registry;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.item.WaypointCardData;
import com.hzldm.createnavigationterminal.item.WaypointCardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 物品注册 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CreateNavigationTerminal.MODID);

    public static final Supplier<Item> NAVIGATION_TERMINAL = ITEMS.register(
            "navigation_terminal",
            () -> new BlockItem(ModBlocks.NAVIGATION_TERMINAL.get(), new Item.Properties())
    );

    /**
     * 模组图标物品：只用于创造模式标签页的图标，不进任何物品栏；
     * 贴图就是模组图标本身（assets/.../textures/item/mod_icon.png）。
     */
    public static final Supplier<Item> MOD_ICON = ITEMS.register(
            "mod_icon",
            () -> new Item(new Item.Properties())
    );

    /** 导航点卡：从 Xaero 勾选标记点存入，供导航台读取 */
    public static final Supplier<Item> WAYPOINT_CARD = ITEMS.register(
            "waypoint_card",
            () -> new WaypointCardItem(new Item.Properties()
                    .stacksTo(1)
                    .component(ModDataComponents.WAYPOINT_CARD, WaypointCardData.EMPTY))
    );
}
