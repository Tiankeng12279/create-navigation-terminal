package com.hzldm.createnavigationterminal.registry;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.recipe.CardDyeRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 配方序列化器注册 */
public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateNavigationTerminal.MODID);

    /**
     * 卡片染色（合成台：卡 + 染料）。
     *
     * <p>只带 category 一个字段，所以 codec 很轻：
     * json 里写 {@code {"type": "create_navigation_terminal:card_dye", "category": "misc"}} 即可。
     */
    public static final Supplier<RecipeSerializer<CardDyeRecipe>> CARD_DYE = SERIALIZERS.register(
            "card_dye",
            () -> new RecipeSerializer<CardDyeRecipe>() {
                private final MapCodec<CardDyeRecipe> codec =
                        net.minecraft.world.item.crafting.CraftingBookCategory.CODEC
                                .fieldOf("category")
                                .xmap(CardDyeRecipe::new, CardDyeRecipe::category);

                @Override
                public MapCodec<CardDyeRecipe> codec() {
                    return codec;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, CardDyeRecipe> streamCodec() {
                    return StreamCodec.composite(
                            net.minecraft.world.item.crafting.CraftingBookCategory.STREAM_CODEC,
                            CardDyeRecipe::category,
                            CardDyeRecipe::new);
                }
            }
    );
}
