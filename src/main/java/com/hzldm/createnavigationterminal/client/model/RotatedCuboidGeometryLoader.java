package com.hzldm.createnavigationterminal.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.joml.Vector3f;

/**
 * {@code create_navigation_terminal:rotated_cuboids} 的读取器。
 *
 * <p>元素从 <b>{@code cn_elements}</b> 读，而不是标准的 {@code elements}：
 * NeoForge 的 {@code ExtendedBlockModelDeserializer} 会先调用 vanilla 的
 * {@code BlockModel.Deserializer#deserialize}，那一步用严格的 {@code BlockElement.Deserializer}
 * 解析 {@code elements}，遇到 15°/-17.5°/30° 直接抛 "Invalid rotation"。放在私有键里就绕开了，
 * 角度<b>原样</b>传给 {@link BlockElementRotation}。
 *
 * <pre>
 * {
 *   "loader": "create_navigation_terminal:rotated_cuboids",
 *   "textures": { "0": "create_navigation_terminal:block/navigation_terminal" },
 *   "cn_elements": [
 *     { "from": [3,0,0], "to": [13,0.5,11],
 *       "rotation": { "origin": [7,0,0], "axis": "y", "angle": 15, "rescale": false },
 *       "faces": { "north": { "uv": [7.25,1.75,9.75,1.875], "texture": "#0" } } }
 *   ]
 * }
 * </pre>
 */
public final class RotatedCuboidGeometryLoader
        implements IGeometryLoader<RotatedCuboidGeometry> {

    @Override
    public RotatedCuboidGeometry read(JsonObject json, JsonDeserializationContext ctx)
            throws JsonParseException {
        // 诊断开关：config/create_navigation_terminal_simple_model 存在时，把模型烘成一个纯立方体。
        // 用来区分「掉帧是模型几何开销」还是「飞行器/世界本身」——建/删这个文件 + F3+T 即可切换，
        // 不用换 jar、不用改 blockstate。
        if (SIMPLE_MODEL_MARKER.get()) {
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Model] 诊断模式：导航终端使用纯立方体模型（删除 config/create_navigation_terminal_simple_model 可恢复）");
            return cubeGeometry();
        }
        JsonArray arr = arr(json, "cn_elements");
        // cn_skip：按索引跳过元素。用于"美术导出里多出来的部件"临时剔除，
        // 改模型之前先靠它救急；写空数组即恢复全部元素。
        java.util.Set<Integer> skip = new java.util.HashSet<>();
        JsonArray skipArr = arr(json, "cn_skip");
        if (skipArr != null) {
            for (JsonElement s : skipArr) {
                skip.add(s.getAsInt());
            }
        }
        List<RotatedCuboidGeometry.ElemSpec> out = new ArrayList<>(arr == null ? 0 : arr.size());
        if (arr == null) {
            return new RotatedCuboidGeometry(out);
        }
        int index = -1;
        for (JsonElement el : arr) {
            index++;
            if (skip.contains(index)) {
                continue;
            }
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            Vector3f from = vec(o.get("from"), new Vector3f(0, 0, 0));
            Vector3f to = vec(o.get("to"), new Vector3f(16, 16, 16));
            BlockElementRotation rotation = rotation(obj(o, "rotation"));
            boolean shade = bool(o, "shade", true);

            List<RotatedCuboidGeometry.FaceSpec> faces = new ArrayList<>(6);
            JsonObject facesObj = obj(o, "faces");
            if (facesObj != null) {
                for (Direction dir : Direction.values()) {
                    JsonElement fe = facesObj.get(dir.getSerializedName());
                    if (fe == null || !fe.isJsonObject()) {
                        continue;
                    }
                    JsonObject f = fe.getAsJsonObject();
                    float[] uv = new float[]{0, 0, 16, 16};
                    JsonArray u = arr(f, "uv");
                    if (u != null && u.size() == 4) {
                        uv = new float[]{
                                u.get(0).getAsFloat(), u.get(1).getAsFloat(),
                                u.get(2).getAsFloat(), u.get(3).getAsFloat()};
                    }
                    String tex = str(f, "texture", "#0");
                    String cullName = str(f, "cullface", null);
                    Direction cull = cullName == null ? null : Direction.byName(cullName);
                    faces.add(new RotatedCuboidGeometry.FaceSpec(
                            dir, tex, uv, num(f, "rotation", 0), cull, num(f, "tintindex", -1)));
                }
            }
            if (!faces.isEmpty()) {
                out.add(new RotatedCuboidGeometry.ElemSpec(from, to, rotation, shade, faces));
            }
        }
        return new RotatedCuboidGeometry(cullCoveredFaces(out));
    }

    // ------------------------------------------------------------------
    // 隐藏面剔除
    //
    // 36 个元素里大量是"贴着另一块的薄板"（机箱内壁、屏幕背后的结构、边框内侧），
    // 这些面永远看不见，但会照常进顶点缓冲、走阴影 pass。
    // 这里做保守剔除：只处理**无旋转**元素；在面上取 5 个采样点，沿法线外移一点点，
    // 如果每个点都落在别的元素内部，就认为这个面被盖住、不烘。
    // 旋转元素一律保留（旋转后的盒子判定复杂，宁可不省）。
    // ------------------------------------------------------------------

    private static final float EPS = 0.02F;

    private static List<RotatedCuboidGeometry.ElemSpec> cullCoveredFaces(
            List<RotatedCuboidGeometry.ElemSpec> in) {
        List<RotatedCuboidGeometry.ElemSpec> out = new ArrayList<>(in.size());
        for (int i = 0; i < in.size(); i++) {
            RotatedCuboidGeometry.ElemSpec e = in.get(i);
            if (e.rotation() != null) {
                out.add(e);          // 保守：旋转元素不剔除
                continue;
            }
            List<RotatedCuboidGeometry.FaceSpec> keep = new ArrayList<>(e.faces().size());
            for (RotatedCuboidGeometry.FaceSpec f : e.faces()) {
                if (!coveredByOther(in, i, e, f.dir())) {
                    keep.add(f);
                }
            }
            if (!keep.isEmpty()) {
                out.add(new RotatedCuboidGeometry.ElemSpec(
                        e.from(), e.to(), null, e.shade(), keep));
            }
        }
        return out;
    }

    /** 该面是否被其它元素整面盖住 */
    private static boolean coveredByOther(List<RotatedCuboidGeometry.ElemSpec> all, int self,
                                          RotatedCuboidGeometry.ElemSpec e, Direction d) {
        Direction.Axis n = d.getAxis();
        Direction.Axis[] tangents = switch (n) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
        float plane = (d.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? axisMax(e.from(), e.to(), n)
                : axisMin(e.from(), e.to(), n)) + d.getStepX() * EPS + d.getStepY() * EPS + d.getStepZ() * EPS;

        float a0 = axisMin(e.from(), e.to(), tangents[0]);
        float a1 = axisMax(e.from(), e.to(), tangents[0]);
        float b0 = axisMin(e.from(), e.to(), tangents[1]);
        float b1 = axisMax(e.from(), e.to(), tangents[1]);

        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                float a = a0 + (a1 - a0) * (i / 4.0F);
                float b = b0 + (b1 - b0) * (j / 4.0F);
                float x = n == Direction.Axis.X ? plane : (tangents[0] == Direction.Axis.X ? a : b);
                float y = n == Direction.Axis.Y ? plane : (tangents[0] == Direction.Axis.Y ? a : b);
                float z = n == Direction.Axis.Z ? plane : (tangents[0] == Direction.Axis.Z ? a : b);
                if (!insideAny(all, self, x, y, z)) {
                    return false;      // 有采样点露在外面 → 不算被盖住
                }
            }
        }
        return true;
    }

    private static boolean insideAny(List<RotatedCuboidGeometry.ElemSpec> all, int self,
                                     float x, float y, float z) {
        for (int i = 0; i < all.size(); i++) {
            if (i == self) {
                continue;
            }
            RotatedCuboidGeometry.ElemSpec o = all.get(i);
            if (o.rotation() != null) {
                continue;            // 与剔除策略保持一致：不拿旋转元素当遮挡体
            }
            if (x > o.from().x() && x < o.to().x()
                    && y > o.from().y() && y < o.to().y()
                    && z > o.from().z() && z < o.to().z()) {
                return true;
            }
        }
        return false;
    }

    private static float axisMin(Vector3f f, Vector3f t, Direction.Axis a) {
        return Math.min(comp(f, a), comp(t, a));
    }

    private static float axisMax(Vector3f f, Vector3f t, Direction.Axis a) {
        return Math.max(comp(f, a), comp(t, a));
    }

    private static float comp(Vector3f v, Direction.Axis a) {
        return switch (a) {
            case X -> v.x();
            case Y -> v.y();
            case Z -> v.z();
        };
    }

    // ---- 极简 JSON 取值（避开 GsonHelper 各重载的版本差异） ----

    /** 诊断开关：文件存在 → 用纯立方体模型（每次烘焙时检查一次，F3+T 生效） */
    private static final java.util.function.Supplier<Boolean> SIMPLE_MODEL_MARKER = () -> {
        try {
            return java.nio.file.Files.exists(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
                    .resolve("create_navigation_terminal_simple_model"));
        } catch (Throwable ignored) {
            return false;
        }
    };

    /** 单个 0..16 的实心立方体（6 面都用 #0 贴图） */
    private static RotatedCuboidGeometry cubeGeometry() {
        float[] full = {0, 0, 16, 16};
        List<RotatedCuboidGeometry.FaceSpec> faces = new ArrayList<>(6);
        for (Direction d : Direction.values()) {
            faces.add(new RotatedCuboidGeometry.FaceSpec(d, "#0", full, 0, null, -1));
        }
        RotatedCuboidGeometry.ElemSpec cube = new RotatedCuboidGeometry.ElemSpec(
                new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), null, true, faces);
        return new RotatedCuboidGeometry(List.of(cube));
    }

    private static Vector3f vec(@Nullable JsonElement e, Vector3f fallback) {
        if (e == null || !e.isJsonArray()) {
            return fallback;
        }
        JsonArray a = e.getAsJsonArray();
        if (a.size() != 3) {
            return fallback;
        }
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    private static String str(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e == null ? def : e.getAsString();
    }

    private static int num(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        return e == null ? def : e.getAsInt();
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o.get(key);
        return e == null ? def : e.getAsBoolean();
    }

    /** 角度原样读取（不吸附到 22.5° 档位）；origin 必须 /16，见下方注释 */
    @Nullable
    private static BlockElementRotation rotation(@Nullable JsonObject r) {
        if (r == null) {
            return null;
        }
        JsonElement angEl = r.get("angle");
        float angle = angEl == null ? 0.0F : angEl.getAsFloat();
        if (Math.abs(angle) < 1.0e-6F) {
            return null;
        }
        // 关键：vanilla 的 BlockElement.Deserializer 里 origin 会乘 0.0625（即 /16），
        // 而 FaceBakery 的顶点坐标是 0..1 空间 —— 两者必须同一空间。
        // 这里漏了 /16 的话，每个元素都会绕一个 16 倍远的支点旋转，模型直接散架，
        // 四边形还会被甩到巨大体积上导致剔除失效、帧率崩掉。
        Vector3f origin = vec(r.get("origin"), new Vector3f(8, 8, 8));
        origin.mul(0.0625F);
        Direction.Axis axis = switch (str(r, "axis", "y").toLowerCase(Locale.ROOT)) {
            case "x" -> Direction.Axis.X;
            case "z" -> Direction.Axis.Z;
            default -> Direction.Axis.Y;
        };
        return new BlockElementRotation(origin, axis, angle, bool(r, "rescale", false));
    }
}
