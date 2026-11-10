package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.neoforged.fml.loading.FMLPaths;

/**
 * 把随模组一起发布的思索场景（{@code assets/create_navigation_terminal/ponder_scenes/*.json}）
 * 自动安装到 Ponderer 的脚本目录。
 *
 * <p>Ponderer 只认 {@code config/ponderer/scripts/} 与 {@code resourcepacks/} 里的场景包，
 * 不会扫描模组 jar 内的 data 目录，所以只能由模组在启动时把这些 JSON 落到它的目录里。
 *
 * <p>版本策略：写入一个 {@code .cn_terminal_scene_version} 标记；
 * 模组版本变化时覆盖全部场景（避免旧版残留），版本一致时只补缺失文件（不覆盖玩家手改）。
 */
public final class PonderSceneInstaller {

    /** 随包发布的场景文件（与 assets/.../ponder_scenes 下同名） */
    private static final String[] SCENES = {
            "navigation_terminal_nav_basic",
            "nav_route",
            "nav_params",
            "nav_fleet",
            "card_basic",
            "card_meta"
    };

    private static final String RES_ROOT = "/assets/create_navigation_terminal/ponder_scenes/";
    private static final String MARKER = ".cn_terminal_scene_version";

    private PonderSceneInstaller() {
    }

    /** 在客户端初始化时调用一次（已包了 enqueueWork，可安全触碰文件系统） */
    public static void install() {
        // 只在装了 Ponderer 时安装：没有它，这些脚本没有加载者
        if (!net.neoforged.fml.ModList.get().isLoaded("ponderer")) {
            return;
        }
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("ponderer").resolve("scripts");
            Files.createDirectories(dir);

            String current = modVersion();
            Path marker = dir.resolve(MARKER);
            boolean versionChanged = !Files.exists(marker)
                    || !current.equals(Files.readString(marker, StandardCharsets.UTF_8).trim());

            int written = 0;
            for (String name : SCENES) {
                Path target = dir.resolve(name + ".json");
                // 版本没变且玩家已有该文件 → 不动它（可能是手改过的）
                if (!versionChanged && Files.exists(target)) {
                    continue;
                }
                try (InputStream in = PonderSceneInstaller.class.getResourceAsStream(RES_ROOT + name + ".json")) {
                    if (in == null) {
                        CreateNavigationTerminal.LOGGER.warn("[Ponder] 场景资源缺失: {}", name);
                        continue;
                    }
                    Path tmp = dir.resolve(name + ".json.tmp");
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                    written++;
                }
            }
            if (versionChanged) {
                Files.writeString(marker, current, StandardCharsets.UTF_8);
            }
            CreateNavigationTerminal.LOGGER.info(
                    "[Ponder] 场景安装完成：写入 {} 个（版本 {}，目录 {}）", written, current, dir);
        } catch (IOException e) {
            CreateNavigationTerminal.LOGGER.warn("[Ponder] 场景安装失败", e);
        }
    }

    private static String modVersion() {
        try {
            var container = net.neoforged.fml.ModList.get()
                    .getModContainerById(CreateNavigationTerminal.MODID).orElse(null);
            return container == null ? "unknown" : container.getModInfo().getVersion().toString();
        } catch (Throwable t) {
            return "unknown";
        }
    }
}
