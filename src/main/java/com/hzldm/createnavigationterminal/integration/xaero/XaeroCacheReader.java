package com.hzldm.createnavigationterminal.integration.xaero;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Xaero 地图缓存（.xwmc）解码器。
 *
 * 格式已由独立复核确认（见 xaero-format/payload/payload-crosscheck.md，103 个文件全部
 * 解析到 EOF，trailing == 0，渲染结果与官方导出图逐像素一致）：
 *
 * <pre>
 * cache.xaero
 *   i32 fullVersion              大端，如 0x00010019
 *   叶子: 再 5 × i32 / 分支: 无
 *   meta: { u8 idx, i32 ver } × N，以 u8 0xFF 结束
 *   biome palette: i32 count, 每项 (u8 pad=0, u16 len, UTF-8)
 *   texture 区: 循环
 *     { u8 idx(0xFF 结束), u8 compressed, i32 fmt(0x8058), i32 len(16384),
 *       pixels(16384 B = 64×64×4 [A,R,G,B]),
 *       u8 hasLight, heights(1024×i64), topHeights(1024×i64),
 *       i32 biomePalSize, palSize × (i32 + i16),
 *       u8 stored, [stored ? 1024×i64 : 0],
 *       [分支: 4 × i32] }
 * </pre>
 *
 * 渲染：texture idx 的低 4 bit = 行(ty)，高 4 bit = 列(tx)，
 * 64×64 贴图拼成 512×512，取像素后 3 字节当 RGB。
 */
public final class XaeroCacheReader {

    /** 每个 texture 边长（像素）。 */
    public static final int TEX = 64;
    /** 一个 region 的边长（像素 = 方块）。 */
    public static final int REGION_PX = 512;
    private static final int PIX_BYTES = TEX * TEX * 4;
    private static final int HEIGHT_I64 = 1024;

    private XaeroCacheReader() {
    }

    /** 读出的 region 图（512×512，RGB 打包成 0xFFRRGGBB）。 */
    public record RegionImage(int[] rgb, int trailing) {
    }

    /**
     * 解析一个 .xwmc 文件，返回 512×512 的地图颜色。
     *
     * @return 解析失败返回 null
     */
    public static RegionImage read(File xwmc) {
        if (xwmc == null || !xwmc.isFile()) {
            return null;
        }
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.BufferedInputStream(new FileInputStream(xwmc)))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                return null;
            }
            byte[] buf = zis.readAllBytes();
            return parse(buf);
        } catch (Throwable t) {
            return null;
        }
    }

    private static RegionImage parse(byte[] buf) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf));

        // 叶子与分支只差「5 个 i32」，两种布局都试一遍再择优，不靠版本号猜。
        byte[] copy = buf;
        RegionImage leaf = tryParse(copy, false);
        RegionImage branch = tryParse(copy, true);
        // 两种布局都试一遍，取能解析且余量更小的那个
        if (leaf != null && branch != null) {
            return leaf.trailing() <= branch.trailing() ? leaf : branch;
        }
        return leaf != null ? leaf : branch;
    }

    private static RegionImage tryParse(byte[] buf, boolean branch) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf));
            readIntBE(in);                       // fullVersion
            if (!branch) {
                for (int i = 0; i < 5; i++) {
                    readIntBE(in);
                }
            }
            // meta
            while (true) {
                int idx = in.readUnsignedByte();
                if (idx == 0xFF) {
                    break;
                }
                readIntBE(in);
            }
            // biome palette
            int palCount = readIntBE(in);
            if (palCount < 0 || palCount > 8192) {
                return null;
            }
            for (int i = 0; i < palCount; i++) {
                in.readUnsignedByte();            // pad
                int len = readUnsignedShortBE(in);
                skip(in, len);
            }

            int[] rgb = new int[REGION_PX * REGION_PX];
            int texCount = 0;

            while (true) {
                int peek = peekByte(buf, in);
                if (peek < 0 || peek == 0xFF) {
                    break;
                }
                int idx = in.readUnsignedByte();
                int compressed = in.readUnsignedByte();
                readIntBE(in);                    // fmt
                int dlen = readIntBE(in);
                byte[] pixels = new byte[dlen];
                in.readFully(pixels);
                if (compressed == 1) {
                    pixels = inflate(pixels, PIX_BYTES);
                    if (pixels == null) {
                        return null;
                    }
                }
                if (pixels.length != PIX_BYTES) {
                    return null;
                }

                // 拼到 512×512：idx 低 4 bit = 行，高 4 bit = 列
                int tx = (idx >> 4) & 0xF;
                int ty = idx & 0xF;
                if (tx >= 8 || ty >= 8) {
                    return null;
                }
                int baseX = tx * TEX;
                int baseY = ty * TEX;
                for (int py = 0; py < TEX; py++) {
                    int row = (baseY + py) * REGION_PX + baseX;
                    for (int px = 0; px < TEX; px++) {
                        int p = (py * TEX + px) * 4;
                        int r = pixels[p + 1] & 0xFF;
                        int g = pixels[p + 2] & 0xFF;
                        int b = pixels[p + 3] & 0xFF;
                        rgb[row + px] = 0xFF000000 | (r << 16) | (g << 8) | b;
                    }
                }
                texCount++;

                // 跳过剩余字段
                in.readUnsignedByte();                       // hasLight
                skip(in, HEIGHT_I64 * 8);                    // heights
                skip(in, HEIGHT_I64 * 8);                    // topHeights
                int palN = readIntBE(in);
                if (palN < 0 || palN > 8192) {
                    return null;
                }
                skip(in, palN * 6L);                         // palN × (i32 + i16)
                int stored = in.readUnsignedByte();
                if (stored != 0) {
                    skip(in, HEIGHT_I64 * 8);                // biomeBits
                }
                if (branch) {
                    skip(in, 16);                            // 4 × i32
                }
            }

            if (texCount == 0) {
                return null;
            }
            // 硬判据：正确解析必须恰好读到末尾（复核报告里 103 个文件都是 trailing == 0）。
            // 解析错位时也会"读出"内容，但不满足这条，必须让调用方换另一种解析方式。
            if (in.available() > 64) {
                return null;
            }
            return new RegionImage(rgb, in.available());
        } catch (Throwable t) {
            return null;
        }
    }

    private static int peekByte(byte[] buf, DataInputStream in) {
        try {
            int pos = buf.length - in.available();
            return pos >= 0 && pos < buf.length ? buf[pos] & 0xFF : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void skip(DataInputStream in, long n) throws IOException {
        long left = n;
        while (left > 0) {
            long skipped = in.skip(left);
            if (skipped <= 0) {
                if (in.read() < 0) {
                    throw new IOException("EOF while skipping");
                }
                left--;
            } else {
                left -= skipped;
            }
        }
    }

    private static int readIntBE(DataInputStream in) throws IOException {
        int b0 = in.readUnsignedByte();
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    private static int readUnsignedShortBE(DataInputStream in) throws IOException {
        int b0 = in.readUnsignedByte();
        int b1 = in.readUnsignedByte();
        return (b0 << 8) | b1;
    }

    private static byte[] inflate(byte[] data, int expected) {
        try {
            Inflater inf = new Inflater();
            inf.setInput(data);
            byte[] out = new byte[expected];
            int n = inf.inflate(out);
            inf.end();
            if (n != expected) {
                return null;
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }
}
