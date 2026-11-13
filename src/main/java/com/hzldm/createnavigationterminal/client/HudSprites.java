package com.hzldm.createnavigationterminal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.resources.Resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HUD 标记贴图。
 *
 * <p>约定：
 * <ul>
 *   <li>贴图放在 {@code assets/create_navigation_terminal/textures/gui/markers/}，
 *       资源包覆盖同名文件即可替换外观；</li>
 *   <li>贴图必须是<b>白色 + 透明度</b>，颜色由 {@link RenderSystem#setShaderColor} 乘算；</li>
 *   <li>贴图缺失时 {@link #drawCentered} 返回 false，调用方回落到原来的 {@code fill()} 画法。</li>
 * </ul>
 */
public final class HudSprites {

    private static final String DIR = "textures/gui/markers/";

    public static final ResourceLocation CROSSHAIR = id("crosshair");
    public static final ResourceLocation TARGET_DIAMOND = id("target_diamond");
    public static final ResourceLocation TARGET_BOX = id("target_box");
    public static final ResourceLocation TARGET_CROSS = id("target_cross");
    public static final ResourceLocation TARGET_BRACKET = id("target_bracket");
    public static final ResourceLocation TARGET_DOT = id("target_dot");
    public static final ResourceLocation NEXT = id("next");
    public static final ResourceLocation FLEET = id("fleet");
    public static final ResourceLocation BEACON = id("beacon");
    public static final ResourceLocation OTHER = id("other");

    /** 加载失败过的贴图，避免每帧重试抛异常 */
    private static final Set<ResourceLocation> MISSING = new HashSet<>();
    /** 贴图真实边长缓存（支持资源包换成 64×64 / 128×128 等高清版） */
    private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

    private HudSprites() {
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("create_navigation_terminal", DIR + name + ".png");
    }

    /** 资源重载后调用：清掉尺寸与“缺失”缓存，让新贴图重新尝试加载 */
    public static void invalidate() {
        MISSING.clear();
        SIZES.clear();
    }

    /**
     * 贴图的真实边长（读取 png 头，缓存）。
     *
     * <p>资源包可以把同名贴图换成 64×64、128×128 等高清版，这里如实返回它们的尺寸，
     * 绘制时按真实尺寸计算 UV，因此<b>不需要改代码</b>。
     *
     * @return 边长（正方形）；贴图缺失或读取失败返回 -1
     */
    public static int sourceSize(ResourceLocation tex) {
        if (tex == null || MISSING.contains(tex)) {
            return -1;
        }
        int[] cached = SIZES.get(tex);
        if (cached != null) {
            return cached[0];
        }
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            Resource res = rm.getResource(tex).orElse(null);
            if (res == null) {
                MISSING.add(tex);
                return -1;
            }
            try (NativeImage img = NativeImage.read(res.open())) {
                int w = img.getWidth();
                SIZES.put(tex, new int[]{w, img.getHeight()});
                return w;
            }
        } catch (Throwable t) {
            MISSING.add(tex);
            return -1;
        }
    }

    /**
     * 按 markerStyle 选目标标记贴图。
     *
     * <p>样式名不区分大小写（配置里常见 {@code "Box"}）。没有对应贴图的样式
     * （例如 {@code chevron}）返回 {@code null}，调用方据此回落到 {@code fill()} 画法，
     * 而不是硬塞成 box。
     */
    public static ResourceLocation forStyle(String style) {
        if (style == null || style.isBlank()) {
            return TARGET_BOX;
        }
        return switch (style.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "diamond" -> TARGET_DIAMOND;
            case "cross" -> TARGET_CROSS;
            case "bracket" -> TARGET_BRACKET;
            case "dot" -> TARGET_DOT;
            case "box" -> TARGET_BOX;
            default -> null;      // chevron 等：交给原来的图元绘制
        };
    }

    /** 贴图是否可用（文件存在且能被纹理管理器加载） */
    public static boolean available(ResourceLocation tex) {
        if (sourceSize(tex) <= 0) {
            return false;
        }
        try {
            Minecraft.getInstance().getTextureManager().getTexture(tex);
            return true;
        } catch (Throwable t) {
            MISSING.add(tex);
            return false;
        }
    }

    /**
     * 以 (cx, cy) 为中心画一张标记贴图。
     *
     * @param size 目标边长（像素）；贴图会从原始尺寸缩放到这个尺寸
     * @param argb 颜色（含 alpha）
     * @return true = 已画；false = 贴图不可用，调用方应回落到 fill 画法
     */
    public static boolean drawCentered(GuiGraphics g, ResourceLocation tex,
                                       int cx, int cy, int size, int argb) {
        if (!available(tex)) {
            return false;
        }
        int src = sourceSize(tex);
        if (src <= 0) {
            return false;
        }
        int half = size / 2;
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float gg = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, gg, b, a);
        try {
            g.blit(tex, cx - half, cy - half, size, size, 0f, 0f, src, src, src, src);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        return true;
    }
}
