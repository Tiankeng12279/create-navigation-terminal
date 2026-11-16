package com.hzldm.createnavigationterminal.integration.aerofreight;

import net.minecraft.world.item.ItemStack;

/**
 * Create: AeroFreight 兼容（只读）。
 *
 * <p>AeroFreight 会把货运站点与订单写成 Xaero 路径点，名字带固定前缀：
 * <ul>
 *   <li>{@code generated_mod_} —— 站点</li>
 *   <li>{@code generated_mod_order_} —— 订单目的地</li>
 * </ul>
 * 本类只做「识别前缀 + 判定清单物品」，不依赖其 jar，也不读写它的任何类。
 *
 * <p>当玩家手持（或终端卡槽放入）AeroFreight 的「货运清单」时，导航终端的候选列表
 * 会切换成这些货运点，方便直接把货运目的地设为导航目标。
 */
public final class FreightWaypoints {

    /** 站点路径点前缀 */
    public static final String STATION_PREFIX = "generated_mod_";
    /** 订单路径点前缀 */
    public static final String ORDER_PREFIX = "generated_mod_order_";

    /** 货运清单物品 id */
    private static final String MANIFEST_ITEM = "create_aerofreight:freight_manifest";

    private FreightWaypoints() {
    }

    /** 该物品是不是 AeroFreight 的货运清单 */
    public static boolean isManifest(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            return stack.getItem().builtInRegistryHolder().key().location().toString().equals(MANIFEST_ITEM);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 名字是否来自 AeroFreight（站点或订单） */
    public static boolean isFreightName(String name) {
        return name != null && (name.startsWith(STATION_PREFIX) || name.startsWith(ORDER_PREFIX));
    }

    /** 是否为订单目的地 */
    public static boolean isOrder(String name) {
        return name != null && name.startsWith(ORDER_PREFIX);
    }

    /** 去掉前缀，得到可读名字 */
    public static String cleanName(String name) {
        if (name == null) {
            return "";
        }
        String n = name;
        if (n.startsWith(ORDER_PREFIX)) {
            n = n.substring(ORDER_PREFIX.length());
        } else if (n.startsWith(STATION_PREFIX)) {
            n = n.substring(STATION_PREFIX.length());
        }
        n = n.replace('_', ' ').trim();
        return n.isEmpty() ? name : n;
    }

    /** 给候选列表加的显示标签 */
    public static String label(String name) {
        String clean = cleanName(name);
        return isOrder(name) ? ("货运订单 · " + clean) : ("货运站 · " + clean);
    }
}
