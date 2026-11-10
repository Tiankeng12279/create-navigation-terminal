package com.hzldm.createnavigationterminal.client.model;

import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.joml.Vector3f;

/**
 * 支持<b>任意角度</b>元素旋转的立方体几何。
 *
 * <p>为什么需要它：MC 的 {@code BlockElement.Deserializer} 把 {@code rotation.angle} 锁死在
 * {@code -45 / -22.5 / 0 / 22.5 / 45}（见 {@code BlockElement#getAngle}），
 * Blockbench 直接导出的 15°、-17.5°、30° 会让整个模型解析失败、方块退化成缺失模型。
 *
 * <p>而 {@code BlockElementRotation} 本身只是普通 record、{@code FaceBakery} 也按 float 角度做旋转 ——
 * 限制只在反序列化那一层。所以这里绕开反序列化：自己读 JSON、自己 new {@link BlockElement}，
 * 角度原样传进 {@link BlockElementRotation}，再交给
 * {@link UnbakedGeometryHelper#bakeElements} 走原版烘焙管线。
 *
 * <p>配套 loader 见 {@link RotatedCuboidGeometryLoader}；元素写在 JSON 的 {@code cn_elements} 里，
 * 避免被 vanilla 那一层先解析掉。
 */
public final class RotatedCuboidGeometry extends SimpleUnbakedGeometry<RotatedCuboidGeometry> {

    /** 一个面的规格（角度无关） */
    public record FaceSpec(Direction dir, String texture, float[] uv, int uvRotation,
                           @Nullable Direction cullface, int tintIndex) {
    }

    /** 一个元素的规格 */
    public record ElemSpec(Vector3f from, Vector3f to,
                           @Nullable BlockElementRotation rotation,
                           boolean shade, List<FaceSpec> faces) {
    }

    private final List<ElemSpec> elements;

    public RotatedCuboidGeometry(List<ElemSpec> elements) {
        this.elements = List.copyOf(elements);
    }

    @Override
    protected void addQuads(IGeometryBakingContext context, IModelBuilder<?> builder,
                            ModelBaker baker,
                            java.util.function.Function<Material, TextureAtlasSprite> spriteGetter,
                            ModelState modelState) {
        List<BlockElement> baked = new ArrayList<>(elements.size());
        for (ElemSpec spec : elements) {
            Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
            for (FaceSpec f : spec.faces()) {
                faces.put(f.dir(), new BlockElementFace(
                        f.cullface(), f.tintIndex(), resolveTexture(context, f.texture()),
                        new BlockFaceUV(f.uv(), f.uvRotation())));
            }
            if (faces.isEmpty()) {
                continue;
            }
            baked.add(new BlockElement(spec.from(), spec.to(), faces, spec.rotation(), spec.shade()));
        }
        UnbakedGeometryHelper.bakeElements(builder, baked, spriteGetter, modelState);
    }

    /** {@code "#0"} → context 里的材质路径；也接受直接写全 id */
    private static String resolveTexture(IGeometryBakingContext context, String ref) {
        if (ref != null && ref.startsWith("#")) {
            String key = ref.substring(1);
            if (context.hasMaterial(key)) {
                return context.getMaterial(key).texture().toString();
            }
            return "minecraft:missingno";
        }
        return ResourceLocation.parse(ref == null ? "minecraft:missingno" : ref).toString();
    }
}
