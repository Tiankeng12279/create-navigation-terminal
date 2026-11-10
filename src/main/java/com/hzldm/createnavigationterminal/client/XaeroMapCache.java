package com.hzldm.createnavigationterminal.client;

import net.minecraft.client.Minecraft;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Xaero 世界地图缓存读取器（只读，零依赖）。
 *
 * 数据来源：<gameDir>/xaero/world-map/<存档名>/null/cache/1/<regionX>_<regionZ>.xwmc
 * 文件是 ZIP，内含单条目 cache.xaero，格式为一个 region（512×512 方块）的
 * 8×8 个 64×64 RGBA 纹理（每纹理 16384 字节，1 像素 = 1 方块）。
 *
 * 解析逻辑照 Xaero 缓存格式实现（version 19-25），
 * 失败时静默返回 0，调用方回退到方块采样。
 */
public final class XaeroMapCache {

    /** 一个 region 覆盖的方块数 */
    private static final int REGION_SIZE = 512;
    /** 单个纹理边长（像素 = 方块） */
    private static final int TILE = 64;
    /** 每个 region 的纹理网格 8×8 */
    private static final int GRID = 8;
    /** 单个纹理字节数：64*64*4 */
    private static final int TEXTURE_BYTES = TILE * TILE * 4;
    /** 单次读取上限 */
    private static final int MAX_BYTES = 16 * 1024 * 1024;

    private static final Map<Long, byte[][]> CACHE = new HashMap<>();
    private static final Set<Long> MISSING = new HashSet<>();

    private static boolean dirResolved;
    private static Path cacheDir;

    private XaeroMapCache() {
    }

    /** 采样世界坐标处的地图颜色（0xAARRGGBB），无缓存返回 0 */
    public static int sample(Minecraft mc, int blockX, int blockZ) {
        Path dir = resolveCacheDir(mc);
        if (dir == null) {
            return 0;
        }
        int rx = Math.floorDiv(blockX, REGION_SIZE);
        int rz = Math.floorDiv(blockZ, REGION_SIZE);
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);

        byte[][] textures = CACHE.get(key);
        if (textures == null) {
            if (MISSING.contains(key)) {
                return 0;
            }
            Path file = dir.resolve(rx + "_" + rz + ".xwmc");
            if (!Files.isRegularFile(file)) {
                MISSING.add(key);
                return 0;
            }
            textures = readRegion(file);
            if (textures == null) {
                MISSING.add(key);
                return 0;
            }
            CACHE.put(key, textures);
        }

        int lx = Math.floorMod(blockX, REGION_SIZE);
        int lz = Math.floorMod(blockZ, REGION_SIZE);
        int layer = (lx / TILE) * GRID + (lz / TILE);
        if (layer < 0 || layer >= GRID * GRID) {
            return 0;
        }
        byte[] tex = textures[layer];
        if (tex == null || tex.length < TEXTURE_BYTES) {
            return 0;
        }
        int px = lx % TILE;
        int py = lz % TILE;
        int off = (py * TILE + px) * 4;
        int r = tex[off] & 0xFF;
        int g = tex[off + 1] & 0xFF;
        int b = tex[off + 2] & 0xFF;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** 清空缓存（切换世界/维度时调用） */
    public static void invalidate() {
        CACHE.clear();
        MISSING.clear();
        dirResolved = false;
        cacheDir = null;
    }

    // ------------------------------------------------------------------
    // 目录定位
    // ------------------------------------------------------------------

    private static Path resolveCacheDir(Minecraft mc) {
        if (dirResolved) {
            return cacheDir;
        }
        dirResolved = true;
        try {
            String worldName = null;
            var server = mc.getSingleplayerServer();
            if (server != null) {
                Path root = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                worldName = root.getParent() != null
                        ? root.getParent().getFileName().toString()
                        : root.getFileName().toString();
            }
            if (worldName == null || worldName.isEmpty()) {
                return null;
            }
            Path base = mc.gameDirectory.toPath()
                    .resolve("xaero").resolve("world-map").resolve(worldName);
            if (!Files.isDirectory(base)) {
                return null;
            }
            // 单机存档通常是 base/null/cache/1
            Path direct = base.resolve("null").resolve("cache").resolve("1");
            if (Files.isDirectory(direct)) {
                cacheDir = direct;
                return cacheDir;
            }
            // 兜底：任意 <x>/cache/1
            try (var walk = Files.walk(base, 3)) {
                var found = walk.filter(p -> p.getFileName() != null
                                && p.getFileName().toString().equals("1")
                                && p.getParent() != null
                                && p.getParent().getFileName().toString().equals("cache"))
                        .findFirst();
                if (found.isPresent()) {
                    cacheDir = found.get();
                    return cacheDir;
                }
            }
        } catch (Throwable ignored) {
            // 缓存不可用时静默降级
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 解析
    // ------------------------------------------------------------------

    private static byte[][] readRegion(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry("cache.xaero");
            if (entry == null) {
                var en = zip.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    if (!e.isDirectory()) {
                        entry = e;
                        break;
                    }
                }
            }
            if (entry == null) {
                return null;
            }
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(zip.getInputStream(entry)))) {
                return parse(in);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[][] parse(DataInputStream in) throws IOException {
        int header = in.readInt();
        int version = header & 0xFFFF;
        int flags = (header >>> 16) & 0xFFFF;
        if (version < 19 || version > 25) {
            throw new IOException("unsupported xaero cache version " + version);
        }
        if (version >= 9) {
            in.readInt();
        }
        if (version >= 11) {
            in.readInt();
        }
        if (version >= 18) {
            in.readInt();
        }
        if (version >= 23) {
            in.readInt();
        }
        if (version >= 24) {
            in.readInt();
        }
        // 未知列表（255 结尾）
        while (in.readUnsignedByte() != 255) {
            in.readInt();
        }
        skipBiomePalette(in, version);

        byte[][] textures = new byte[GRID * GRID][];
        while (true) {
            int b = in.readUnsignedByte();
            if (b == 255) {
                break;
            }
            int layerX = b >>> 4;
            int layerY = b & 0x0F;
            boolean compressed = in.readUnsignedByte() == 1;
            int format = in.readInt();
            int length = in.readInt();
            if (length < 0 || length > MAX_BYTES) {
                throw new IOException("bad texture length " + length);
            }
            byte[] data = in.readNBytes(length);
            if (data.length != length) {
                throw new EOFException("truncated xaero texture data");
            }
            // 32856 = GL_RGBA8
            if (!compressed && format == 32856 && length >= TEXTURE_BYTES
                    && layerX < GRID && layerY < GRID) {
                textures[layerX * GRID + layerY] = data;
            }
            // 尾部附加数据
            if (version >= 14) {
                in.readBoolean();
            } else if (version > 2) {
                skipChecked(in, in.readInt());
            }
            if (version >= 13) {
                int n = (flags != 0) ? 1024 : 586;
                skipChecked(in, (long) n * 8);
                if (version >= 17) {
                    skipChecked(in, (long) n * 8);
                }
            }
            skipBiomeIndexStorage(in, version);
            if (version == 16) {
                skipChecked(in, 512);
            }
        }
        return textures;
    }

    private static void skipBiomePalette(DataInputStream in, int version) throws IOException {
        if (version < 19) {
            return;
        }
        int count = in.readInt();
        if (count < 0 || count > 65536) {
            throw new IOException("bad biome palette size " + count);
        }
        for (int i = 0; i < count; i++) {
            int b = in.readUnsignedByte();
            if (b == 255) {
                break;
            }
            if (b == 0) {
                in.readUTF();
            } else {
                in.readInt();
                if (version <= 20) {
                    in.readShort();
                }
            }
        }
    }

    private static void skipBiomeIndexStorage(DataInputStream in, int version) throws IOException {
        if (version < 19) {
            return;
        }
        int count = in.readInt();
        if (count < 0 || count > 4096) {
            throw new IOException("bad biome index size " + count);
        }
        if (count == 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int v = in.readInt();
            if (v != -1) {
                in.readShort();
            }
        }
        if (version != 19 && in.readUnsignedByte() != 1) {
            return;
        }
        skipChecked(in, 8192);
    }

    private static void skipChecked(DataInputStream in, long len) throws IOException {
        if (len < 0 || len > MAX_BYTES) {
            throw new IOException("bad skip length " + len);
        }
        in.skipNBytes(len);
    }
}
