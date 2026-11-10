package com.hzldm.createnavigationterminal.integration.xaero;

/** 无 Xaero 构建：没有缓存可读，地图退回实时采样 / 示意网格。 */
public final class XaeroMapSource {

    public static final int REGION = 512;

    private XaeroMapSource() {
    }

    public static boolean isAvailable() {
        return false;
    }

    public static int colorAt(int bx, int bz) {
        return 0;
    }

    public static int[] getRegionArray(int rx, int rz) {
        return null;
    }

    public static void invalidate() {
        // no-op
    }
}
