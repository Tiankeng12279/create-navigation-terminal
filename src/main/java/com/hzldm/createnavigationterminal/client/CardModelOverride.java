package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.item.WaypointCardData;
import com.hzldm.createnavigationterminal.registry.ModDataComponents;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/**
 * 按卡片颜色挑选模型：16 种染料各有一套贴图与模型（{@code item/waypoint_card_<色>}）。
 *
 * <p>两个事件配合：
 * <ol>
 *   <li>{@link ModelEvent.RegisterAdditional}：把这 16 个"只被引用、不作为物品模型"的模型
 *       显式排进烘焙队列 —— 否则它们不会出现在烘焙结果里，取不到。</li>
 *   <li>{@link ModelEvent.ModifyBakingResult}：把基础模型包一层，覆写 overrides，
 *       渲染时按卡片组件的 {@code color} 换成对应颜色的模型。</li>
 * </ol>
 */
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID, value = Dist.CLIENT)
public final class CardModelOverride {

    private static final String[] NAMES = {
            "white", "orange", "magenta", "light_blue",
            "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black"
    };

    /** 物品 id（物品模型在烘焙表里就是用它做键） */
    private static final ResourceLocation CARD_ID =
            ResourceLocation.fromNamespaceAndPath(CreateNavigationTerminal.MODID, "waypoint_card");

    private CardModelOverride() {
    }

    private static ResourceLocation colorModelId(int dyeId) {
        return ResourceLocation.fromNamespaceAndPath(
                CreateNavigationTerminal.MODID, "item/waypoint_card_" + NAMES[dyeId]);
    }

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for (int i = 0; i < NAMES.length; i++) {
            event.register(ModelResourceLocation.standalone(colorModelId(i)));
        }
    }

    @SubscribeEvent
    public static void onBake(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

        // 按 id 建索引，方便同时找到物品模型与颜色模型
        Map<ResourceLocation, BakedModel> byId = new HashMap<>();
        for (Map.Entry<ModelResourceLocation, BakedModel> e : models.entrySet()) {
            byId.put(e.getKey().id(), e.getValue());
        }

        BakedModel original = byId.get(CARD_ID);
        ModelResourceLocation baseKey = ModelResourceLocation.inventory(CARD_ID);
        if (original == null) {
            original = models.get(baseKey);
        }
        if (original == null) {
            CreateNavigationTerminal.LOGGER.warn("[Card] 找不到基础模型 {}，跳过颜色模型替换", CARD_ID);
            return;
        }
        if (!models.containsKey(baseKey)) {
            baseKey = null;      // 只存在于索引里，那就按索引值替换
        }

        CardModel wrapper = new CardModel(original, byId);
        if (baseKey != null) {
            models.put(baseKey, wrapper);
        } else {
            CreateNavigationTerminal.LOGGER.info("[Card] 基础模型按 id 命中（无 inventory 键），使用索引替换");
        }
        // 无论哪种情况都要把原来位置上的值换掉
        for (Map.Entry<ModelResourceLocation, BakedModel> e : models.entrySet()) {
            if (e.getValue() == original) {
                e.setValue(wrapper);
            }
        }
    }

    /** 包一层，只覆写 item overrides */
    private static final class CardModel extends BakedModelWrapper<BakedModel> {

        private final BakedModel base;
        private final Map<ResourceLocation, BakedModel> byId;
        private ItemOverrides overrides;

        private CardModel(BakedModel original, Map<ResourceLocation, BakedModel> byId) {
            super(original);
            this.base = original;
            this.byId = byId;
        }

        @Override
        public ItemOverrides getOverrides() {
            if (overrides == null) {
                overrides = new ItemOverrides() {
                    @Override
                    public BakedModel resolve(BakedModel model, ItemStack stack,
                                              ClientLevel level, LivingEntity entity, int seed) {
                        WaypointCardData data = stack.getOrDefault(
                                ModDataComponents.WAYPOINT_CARD, WaypointCardData.EMPTY);
                        if (!data.dyed()) {
                            return base;
                        }
                        int id = Math.max(0, Math.min(15, data.color()));
                        BakedModel picked = byId.get(colorModelId(id));
                        return picked == null ? base : picked;
                    }
                };
            }
            return overrides;
        }
    }
}
