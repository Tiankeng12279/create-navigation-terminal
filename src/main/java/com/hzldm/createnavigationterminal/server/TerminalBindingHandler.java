package com.hzldm.createnavigationterminal.server;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 服务端：扳手两步绑定（仿 create-fire-control 的绑定会话）
 *
 *   ① 扳手右键导航终端 → 记录终端（30 秒有效）
 *   ② 扳手右键座椅/载具 → 完成绑定
 *
 * 同时监听「右键实体」和「右键方块」：
 * Create 的座椅有的挂在实体上、有的带方块底座，两路都要能接住。
 * 优先级 HIGHEST + setCanceled，抢在「右键坐下」之前处理。
 */
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID)
public final class TerminalBindingHandler {

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> WRENCH_TAG =
            net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    private TerminalBindingHandler() {
    }

    private static boolean isWrench(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            if (stack.is(WRENCH_TAG)) {
                return true;
            }
        } catch (Throwable ignored) {
            // 标签缺失时用 id 兜底
        }
        return stack.getItem().builtInRegistryHolder().key().location().getPath().contains("wrench");
    }

    // ------------------------------------------------------------------
    // ② 右键实体（create:seat 等）
    // ------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        boolean wrench = isWrench(stack);
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !wrench) {
            return;
        }

        BlockPos pending = BindingTracker.getPending(player);
        CreateNavigationTerminal.LOGGER.info(
                "[Nav] 右键实体 {} | 扳手={} | 待绑定终端={}",
                event.getTarget() == null ? "null" : event.getTarget().getType().toString(),
                wrench, pending);
        if (pending == null) {
            return;
        }

        Entity target = event.getTarget();
        if (target == null) {
            return;
        }
        finishBind(player, pending, target.getUUID().toString(), target.getType().toString());
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------
    // ② 右键方块（带底座的座椅/载具方块）
    // ------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isWrench(event.getItemStack())) {
            return;
        }

        BlockPos pending = BindingTracker.getPending(player);
        if (pending == null) {
            return;
        }

        BlockPos clicked = event.getPos();
        if (clicked.equals(pending)) {
            // 再次右键终端 = 取消本次绑定
            BindingTracker.remove(player);
            player.displayClientMessage(Component.literal("§7已取消绑定"), true);
            event.setCanceled(true);
            return;
        }

        // 绑「座椅方块」的坐标 —— 和 fire-control 显示的一致（[20481032,128,20481049] 这种）。
        // 实体 UUID 每次上下车都会变，实体的 blockPosition 也会随坐姿浮动，
        // 只有方块坐标是稳定的。
        String uuid = "seatpos:" + clicked.asLong();
        String label = "座椅方块 " + clicked.toShortString();

        CreateNavigationTerminal.LOGGER.info(
                "[Nav] 右键方块 {} | 待绑定终端={} | 目标={}", clicked, pending, label);
        finishBind(player, pending, uuid, label);
        event.setCanceled(true);
    }

    /**
     * 在终端附近找 Aeronautics 的铭牌方块并读取名字。
     *
     * 铭牌是方块（simulated:nameplate），由 NameplateBlockEntity 承载，
     * 有公开的 getName()。铭牌和终端一般在同一艘载具（同一 SubLevel）里，
     * 按方块坐标就近扫描即可。
     */
    private static String findNearbyNameplate(net.minecraft.world.level.Level level, BlockPos center) {
        if (level == null || center == null) {
            return null;
        }
        int r = 12;
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-r, -r, -r), center.offset(r, r, r))) {
            var be = level.getBlockEntity(p);
            if (be == null) {
                continue;
            }
            if (!be.getClass().getName().endsWith(".NameplateBlockEntity")) {
                continue;
            }
            try {
                var m = be.getClass().getMethod("getName");
                Object v = m.invoke(be);
                if (v instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (Throwable ignored) {
                // 反射失败跳过
            }
        }
        return null;
    }

    /** 完成绑定并提示 */
    private static void finishBind(ServerPlayer player, BlockPos terminalPos,
                                   String uuid, String label) {
        if (!(player.level().getBlockEntity(terminalPos) instanceof NavigationTerminalBlockEntity terminal)) {
            BindingTracker.remove(player);
            player.displayClientMessage(
                    Component.literal("§c选中的终端已经不存在了，请重新选择"), true);
            return;
        }

        // 绑座椅位置：seat 实体太短命，位置才稳定
        Entity riding = player.getVehicle();
        if (riding == null) {
            player.displayClientMessage(
                    Component.literal("§e提示：你现在没坐在座椅上，坐上去后 HUD 会自动亮起"), false);
        }

        terminal.setBoundSeatUuid(uuid);
        terminal.setWorldRef(player.getX(), player.getY(), player.getZ());

        // 顺带读取载具上的 Aeronautics 铭牌（simulated:nameplate 方块）
        String plate = findNearbyNameplate(player.level(), terminalPos);
        if (plate != null && !plate.isBlank()) {
            terminal.setTerminalName(plate);
        }

        BindingTracker.remove(player);

        player.displayClientMessage(
                Component.literal("§a已绑定 §f" + label.replace("minecraft:", "")
                        + (plate != null && !plate.isBlank() ? " §7· 铭牌 §f" + plate : "")
                        + " §7(" + uuid.substring(0, Math.min(8, uuid.length())) + ")"),
                true);

        if (riding == null) {
            player.displayClientMessage(
                    Component.literal("§e提示：你现在没坐在座椅上，坐上去后若 HUD 不亮请重新绑定"),
                    false);
        }

        CreateNavigationTerminal.LOGGER.info(
                "[Nav] 绑定完成 entity={} -> terminal={}（真实坐标 {},{},{}）",
                uuid, terminalPos, (int) player.getX(), (int) player.getY(), (int) player.getZ());
    }
}
