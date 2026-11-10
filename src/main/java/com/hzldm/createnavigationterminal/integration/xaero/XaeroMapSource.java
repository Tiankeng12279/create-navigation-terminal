package com.hzldm.createnavigationterminal.integration.xaero;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 Xaero 的离线地图缓存里按方块坐标取颜色。
 *
 * 定位策略：扫描 {@code <gameDir>/xaero/world-map/*} 下所有 cache 目录，
 * 按 region 文件名在整个候选集合里查找 —— 一个世界有多个维度目录
 * （mw$default/cache_1、cm$1/cache/1 …），只锁定单个目录会漏掉大部分 region。
 *
 * region 边长固定 512 方块 = 512 像素（8×8 个 64×64 贴图），
 * 文件名形如 {@code <rx>_<rz>.xwmc}。
 */
public final class XaeroMapSource {

    /** region 边长（方块 = 像素）。 */
    public static final int REGION = 512;

    private static boolean located = false;
    private static boolean available = false;

    /** 所有候选 cache 目录（按修改时间降序）。 */
    private static final List<File> CACHE_DIRS = new ArrayList<>();
    /** 全部 cache 目录（含洞穴层），仅作回退。 */
    private static final List<File> ALL_CACHE_DIRS = new ArrayList<>();

    /** region 图片缓存：key = "rx,rz"。 */
    private static final Map<String, int[]> REGION_CACHE = new HashMap<>();
    /** 读取失败的时间戳：Xaero 可能正在写盘，过几秒要允许重试。 */
    private static final Map<String, Long> MISS_TIME = new HashMap<>();
    private static final long MISS_RETRY_MS = 5000L;
    private static final int MAX_CACHED = 24;
    private static int dbgLookup = 0;

    private XaeroMapSource() {
    }

    public static boolean isAvailable() {
        ensureLocated();
        return available;
    }

    private static void ensureLocated() {
        if (located) {
            return;
        }
        located = true;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameDirectory == null) {
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.warn(
                        "[Xaero] no Minecraft instance or gameDirectory");
                return;
            }
            File root = new File(mc.gameDirectory, "xaero/world-map");
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Xaero] gameDir={} root={} isDir={}",
                    mc.gameDirectory, root.getAbsolutePath(), root.isDirectory());
            if (!root.isDirectory()) {
                return;
            }
            File[] worlds = root.listFiles(File::isDirectory);
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Xaero] worlds={}", worlds == null ? 0 : worlds.length);
            if (worlds == null || worlds.length == 0) {
                return;
            }

            List<File> all = new ArrayList<>();
            for (File w : worlds) {
                collectCacheDirs(w, all, 0);
            }
            ALL_CACHE_DIRS.addAll(all);
            List<File> candidates = new ArrayList<>();
            for (File d : all) {
                if (isSurfaceLayer(d)) {
                    candidates.add(d);
                }
            }
            if (candidates.isEmpty()) {
                candidates.addAll(all);
            }
            if (candidates.isEmpty()) {
                return;
            }
            candidates.sort(Comparator.comparingLong(File::lastModified).reversed());
            CACHE_DIRS.addAll(candidates);
            available = true;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(6, candidates.size()); i++) {
                File d = candidates.get(i);
                File[] fs = d.listFiles((dir, name) -> name.endsWith(".xwmc"));
                sb.append("\n    ").append(d.getAbsolutePath())
                  .append("  files=").append(fs == null ? 0 : fs.length);
            }
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Xaero] cacheDirs={} 前几个:{}", candidates.size(), sb);
        } catch (Throwable ignored) {
            available = false;
        }
    }

    private static void collectCacheDirs(File dir, List<File> out, int depth) {
        if (depth > 4 || dir == null) {
            return;
        }
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) {
            return;
        }
        for (File sub : subs) {
            String n = sub.getName();
            if (n.equals("cache") || n.startsWith("cache_")) {
                // 洞穴层（cm$）画出来是浅色岩层，会盖住地表，必须排除
                if (!isSurfaceLayer(sub)) {
                    continue;
                }
                out.add(sub);
            } else {
                collectCacheDirs(sub, out, depth + 1);
            }
        }
    }

    /**
     * 是否是地表层缓存。
     *
     * Xaero 的目录结构：{@code <world>/null/<layer>/cache_N}，
     * 地表层是 {@code mw$default}、{@code mw$xxx}，洞穴层是 {@code cm$1} 等。
     * 洞穴层的缓存画出来是浅色岩层，混进地表会导致大片白色块。
     */
    private static boolean isSurfaceLayer(File cacheDir) {
        for (File p = cacheDir; p != null; p = p.getParentFile()) {
            String name = p.getName();
            if (name.startsWith("cm$")) {
                return false;
            }
            if (name.startsWith("mw$")) {
                return true;
            }
        }
        // 路径里没有明确层级标记时保留（有些存档布局不同）
        return true;
    }

    /** 取某个方块坐标的地图颜色（0xFFRRGGBB）；没有数据返回 0。 */
    public static int colorAt(int bx, int bz) {
        ensureLocated();
        if (!available) {
            return 0;
        }
        int rx = Math.floorDiv(bx, REGION);
        int rz = Math.floorDiv(bz, REGION);
        int[] img = getRegion(rx, rz);
        if (img == null) {
            return 0;
        }
        int lx = Math.floorMod(bx, REGION);
        int lz = Math.floorMod(bz, REGION);
        int c = img[lz * REGION + lx];
        // 近黑视为「该格无数据」：未探索区域在缓存里就是全 0，
        // 直接画出来会变成难看的黑块，交给上层画网格更清楚
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        if (r < 16 && g < 16 && b < 16) {
            return 0;
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 取某个 region 的 512×512 颜色数组（供批量采样直接索引）。
     *
     * 与 {@link #colorAt} 共用同一份缓存，避免逐像素走除法+取模+Map 查找。
     *
     * @return 未命中返回 null
     */
    public static int[] getRegionArray(int rx, int rz) {
        ensureLocated();
        if (!available) {
            return null;
        }
        return getRegion(rx, rz);
    }

    /** 在所有候选目录里找该 region 的文件（带缓存）。 */
    private static int[] getRegion(int rx, int rz) {
        String key = rx + "," + rz;
        // 命中缓存
        if (REGION_CACHE.containsKey(key)) {
            return REGION_CACHE.get(key);
        }
        // 之前失败过：5 秒内不重试（避免每帧反复读半写的文件），过期后重新尝试
        Long missAt = MISS_TIME.get(key);
        if (missAt != null && System.currentTimeMillis() - missAt < MISS_RETRY_MS) {
            return null;
        }
        String name = rx + "_" + rz + ".xwmc";

        // 第一轮：只在已筛过的地表层目录里找
        for (File dir : CACHE_DIRS) {
            File f = new File(dir, name);
            if (!f.isFile()) {
                continue;
            }
            XaeroCacheReader.RegionImage img = XaeroCacheReader.read(f);
            if (img != null) {
                if (REGION_CACHE.size() >= MAX_CACHED) {
                    REGION_CACHE.clear();
                }
                REGION_CACHE.put(key, img.rgb());
                MISS_TIME.remove(key);
                STAT_HIT++;
                reportStats();
                return img.rgb();
            }
            // 有文件但解析失败
            STAT_FAIL++;
        }

        // 第二轮：地表层没有时，回退到所有 cache 目录（包括 cm$ 等），
        // 总比整片空白好；上层已按近黑/无数据过滤，不会画出突兀色块。
        for (File dir : ALL_CACHE_DIRS) {
            if (CACHE_DIRS.contains(dir)) {
                continue;
            }
            File f = new File(dir, name);
            if (!f.isFile()) {
                continue;
            }
            XaeroCacheReader.RegionImage img = XaeroCacheReader.read(f);
            if (img != null) {
                if (REGION_CACHE.size() >= MAX_CACHED) {
                    REGION_CACHE.clear();
                }
                REGION_CACHE.put(key, img.rgb());
                STAT_HIT++;
                reportStats();
                return img.rgb();
            }
        }

        // 记下失败时间：5 秒内不再重扫（否则每帧都要遍历上百个目录，非常卡），
        // 5 秒后允许重试（Xaero 可能刚写完盘）。
        // 注意不能 REGION_CACHE.put(key, null)：那样 containsKey 会一直命中，
        // 重试逻辑永远走不到，那块地图就永久空白。
        STAT_MISS++;
        reportStats();
        MISS_TIME.put(key, System.currentTimeMillis());
        return null;
    }

    private static int STAT_HIT = 0;
    private static int STAT_MISS = 0;
    private static int STAT_FAIL = 0;

    /** 上次打印时间：日志必须限流，否则 miss 成千上万时会把渲染线程写死 */
    private static long LAST_REPORT_MS = 0L;

    private static void reportStats() {
        int total = STAT_HIT + STAT_MISS + STAT_FAIL;
        long now = System.currentTimeMillis();
        // 5 秒最多一行；连续 miss 时不再每 50 次打一次
        if (total > 0 && (total % 5000 == 0 || now - LAST_REPORT_MS > 5000L)) {
            LAST_REPORT_MS = now;
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Xaero] region stats: hit={} miss={} fail={} dirs={}",
                    STAT_HIT, STAT_MISS, STAT_FAIL, CACHE_DIRS.size());
        }
    }

    public static void invalidate() {
        REGION_CACHE.clear();
        CACHE_DIRS.clear();
        ALL_CACHE_DIRS.clear();
        MISS_TIME.clear();
        located = false;
        available = false;
    }
}
