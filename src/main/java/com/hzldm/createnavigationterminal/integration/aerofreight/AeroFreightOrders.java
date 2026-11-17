package com.hzldm.createnavigationterminal.integration.aerofreight;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.item.WpTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取 AeroFreight 的「当前进行中货运订单」（服务端）。
 *
 * <p>数据来源：{@code DispatchStationBlock.activeOrders(ServerLevel, Player)}，
 * 该方法返回该玩家已接单的 {@code List<ActiveFreightOrder>}。整条链路用反射调用，
 * 因此：
 * <ul>
 *   <li>不需要把 AeroFreight 加进依赖；</li>
 *   <li>它没装、或内部改了签名 → 返回空列表并只记录一次日志，不影响其它功能。</li>
 * </ul>
 */
public final class AeroFreightOrders {

    private static final String DISPATCH_BLOCK =
            "cn.blockforge.generated.createareofreight.block.DispatchStationBlock";

    /** 反射失败只提示一次，避免刷屏 */
    private static boolean warned = false;

    private AeroFreightOrders() {
    }

    /** AeroFreight 是否已加载 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded("create_aerofreight");
    }

    /**
     * 取该玩家进行中的货运订单，转换成导航点。
     *
     * @return 订单目的地列表（可能为空）；任何异常都返回空列表
     */
    public static List<WpTarget> activeOrders(ServerLevel level, Player player) {
        if (level == null || player == null || !isLoaded()) {
            return List.of();
        }
        try {
            Class<?> cls = Class.forName(DISPATCH_BLOCK);
            Method m = cls.getMethod("activeOrders", ServerLevel.class, Player.class);
            Object raw = m.invoke(null, level, player);
            if (!(raw instanceof List<?> orders) || orders.isEmpty()) {
                return List.of();
            }
            List<WpTarget> out = new ArrayList<>(orders.size());
            for (Object order : orders) {
                BlockPos pos = (BlockPos) invoke(order, "destination");
                if (pos == null) {
                    continue;
                }
                Object nameObj = invoke(order, "destinationName");
                String name = nameObj == null ? "" : String.valueOf(nameObj);
                if (name.isBlank()) {
                    name = "货运订单";
                }
                Object dimObj = invoke(order, "orderDimension");
                String dim = dimObj instanceof ResourceLocation rl
                        ? rl.toString()
                        : level.dimension().location().toString();
                out.add(new WpTarget("货运订单 · " + name, pos.getX(), pos.getY(), pos.getZ(), dim));
            }
            return out;
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                CreateNavigationTerminal.LOGGER.warn(
                        "[AeroFreight] 读取订单失败（可能是版本差异）：{}", t.toString());
            }
            return List.of();
        }
    }

    private static Object invoke(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }
}
