package com.hzldm.createnavigationterminal.item;

import com.hzldm.createnavigationterminal.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 导航点卡：存一组纯数据导航点（WpTarget），由导航终端的「写 / 读」按钮存取。
 *
 * <p>提示框按 Create「列车时刻表」那种层次排版（标题 / 灰提示 / 黄色条目 / 灰色页脚），
 * 而不是一句话了事：
 * <pre>
 *   导航点卡
 *   放入终端左侧卡槽，用 写 / 读 存取航线
 *   -> 8 个导航点
 *   -> 船只  -1222, 34
 *   ...
 *   create_navigation_terminal:waypoint_card
 *   7 个组件
 * </pre>
 */
public class WaypointCardItem extends Item {

    /** 提示框里最多列出的导航点条数，超出用省略行 */
    private static final int SHOW_MAX = 3;

    public WaypointCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WaypointCardData data = stack.getOrDefault(ModDataComponents.WAYPOINT_CARD, WaypointCardData.EMPTY);
        int count = data.entries().size();

        // 名字（白）+ 只读标记（红）
        if (data.name() != null && !data.name().isBlank()) {
            tooltip.add(Component.literal("航线：" + data.name())
                    .withStyle(ChatFormatting.WHITE));
        }
        if (data.readOnly()) {
            tooltip.add(Component.literal("只读 · 不允许被当前航线覆盖")
                    .withStyle(ChatFormatting.RED));
        }
        if (data.dyed()) {
            net.minecraft.world.item.DyeColor dc = net.minecraft.world.item.DyeColor.byId(data.color());
            tooltip.add(Component.literal("分类：" + dyeName(dc))
                    .withStyle(dyeFormatting(dc)));
        }
        // 用法提示（灰）
        tooltip.add(Component.literal("放入终端左侧卡槽，用 写 / 读 存取航线")
                .withStyle(ChatFormatting.GRAY));

        if (count == 0) {
            tooltip.add(Component.literal("> 空卡，尚未存入导航点").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("> " + count + " 个导航点").withStyle(ChatFormatting.YELLOW));
            for (int i = 0; i < Math.min(SHOW_MAX, count); i++) {
                WpTarget wp = data.entries().get(i);
                tooltip.add(Component.literal("-> " + wp.name() + "   " + wp.x() + ", " + wp.z())
                        .withStyle(ChatFormatting.GOLD));
            }
            if (count > SHOW_MAX) {
                tooltip.add(Component.literal("-> … 还有 " + (count - SHOW_MAX) + " 个")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        // 页脚：物品 id + 组件数（与 Create 风格一致）
        tooltip.add(Component.literal("create_navigation_terminal:waypoint_card")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(Math.max(1, stack.getComponents().size()) + " 个组件")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 色号 → 中文名（提示框里显示分类颜色） */
    private static String dyeName(net.minecraft.world.item.DyeColor dc) {
        return switch (dc) {
            case WHITE -> "白";
            case ORANGE -> "橙";
            case MAGENTA -> "洋红";
            case LIGHT_BLUE -> "淡蓝";
            case YELLOW -> "黄";
            case LIME -> "黄绿";
            case PINK -> "粉";
            case GRAY -> "灰";
            case LIGHT_GRAY -> "淡灰";
            case CYAN -> "青";
            case PURPLE -> "紫";
            case BLUE -> "蓝";
            case BROWN -> "棕";
            case GREEN -> "绿";
            case RED -> "红";
            case BLACK -> "黑";
        };
    }

    private static ChatFormatting dyeFormatting(net.minecraft.world.item.DyeColor dc) {
        return switch (dc) {
            case WHITE, LIGHT_GRAY -> ChatFormatting.WHITE;
            case ORANGE -> ChatFormatting.GOLD;
            case MAGENTA, PINK -> ChatFormatting.LIGHT_PURPLE;
            case LIGHT_BLUE, CYAN -> ChatFormatting.AQUA;
            case YELLOW -> ChatFormatting.YELLOW;
            case LIME, GREEN -> ChatFormatting.GREEN;
            case GRAY -> ChatFormatting.GRAY;
            case PURPLE -> ChatFormatting.DARK_PURPLE;
            case BLUE -> ChatFormatting.BLUE;
            case BROWN -> ChatFormatting.DARK_RED;
            case RED -> ChatFormatting.RED;
            case BLACK -> ChatFormatting.DARK_GRAY;
        };
    }
}
