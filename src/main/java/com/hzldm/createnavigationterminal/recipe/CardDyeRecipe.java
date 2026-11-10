package com.hzldm.createnavigationterminal.recipe;

import com.hzldm.createnavigationterminal.item.WaypointCardData;
import com.hzldm.createnavigationterminal.registry.ModDataComponents;
import com.hzldm.createnavigationterminal.registry.ModItems;
import com.hzldm.createnavigationterminal.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 合成台上给导航点卡上色：一张卡 + 一种染料 → 同色的卡。
 *
 * <p>用 {@link CustomRecipe} 而不是普通 json 配方，是因为卡片里存着航线数据：
 * 普通"结果物"会生成一张全新的空卡，把手写进去的航线冲掉。
 * 这里直接复制输入卡的组件，只改 {@code color} 字段，航线、名字、只读标记全部保留。
 *
 * <p>染料种类 → 色号 = {@link DyeColor#getId()}；用水桶不能洗（合成台里没有"洗"的语义）。
 */
public class CardDyeRecipe extends CustomRecipe {

    public CardDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack card = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.WAYPOINT_CARD.get())) {
                if (!card.isEmpty()) {
                    return false;
                }
                card = stack;
            } else if (stack.getItem() instanceof DyeItem) {
                if (!dye.isEmpty()) {
                    return false;       // 只允许一种染料，避免混色歧义
                }
                dye = stack;
            } else {
                return false;
            }
        }
        return !card.isEmpty() && !dye.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack card = ItemStack.EMPTY;
        int color = WaypointCardData.NO_COLOR;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.WAYPOINT_CARD.get())) {
                card = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem) {
                color = dyeItem.getDyeColor().getId();
            }
        }
        if (card.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = card.copyWithCount(1);
        WaypointCardData data = out.getOrDefault(
                ModDataComponents.WAYPOINT_CARD, WaypointCardData.EMPTY);
        out.set(ModDataComponents.WAYPOINT_CARD, data.withColor(color));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CARD_DYE.get();
    }
}
