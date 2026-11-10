package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.integration.sable.SableTransform;
import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import com.hzldm.createnavigationterminal.item.WaypointCardData;
import com.hzldm.createnavigationterminal.item.WaypointCardItem;
import com.hzldm.createnavigationterminal.item.WpTarget;
import com.hzldm.createnavigationterminal.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * 网络包注册与服务端处理
 *
 * 全部为 C2S（客户端→服务端）：导航点在客户端，玩家选完才上报。
 * 每个 handler 都做双重校验：方块确实是导航终端 + 玩家在交互距离内。
 * 缺了这层，本模组的包就等于「任意方块随意写数据」的入口。
 */
public final class ModPayloads {

    /** 交互距离平方（与菜单 stillValid 一致） */
    private static final double MAX_INTERACT_DISTANCE_SQR = 64.0;

    private ModPayloads() {
    }

    /** 注册入口：由主类在构造函数中通过 modEventBus.addListener 调用 */
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(CreateNavigationTerminal.MODID);
        registrar
                .playToServer(SetNavigationTargetPayload.TYPE, SetNavigationTargetPayload.STREAM_CODEC,
                        ModPayloads::handleSetTarget)
                .playToServer(SetNavigationSettingsPayload.TYPE, SetNavigationSettingsPayload.STREAM_CODEC,
                        ModPayloads::handleSetSettings)
                .playToServer(ResetArrivedPayload.TYPE, ResetArrivedPayload.STREAM_CODEC,
                        ModPayloads::handleResetArrived)
                .playToServer(SaveWaypointCardPayload.TYPE, SaveWaypointCardPayload.STREAM_CODEC,
                        ModPayloads::handleSaveCard)
                .playToServer(BindSeatPayload.TYPE, BindSeatPayload.STREAM_CODEC,
                        ModPayloads::handleBindSeat)
                .playToServer(AddRoutePayload.TYPE, AddRoutePayload.STREAM_CODEC,
                        ModPayloads::handleAddRoute)
                .playToServer(RouteEditPayload.TYPE, RouteEditPayload.STREAM_CODEC,
                        ModPayloads::handleRouteEdit)
                .playToServer(PresetPayload.TYPE, PresetPayload.STREAM_CODEC,
                        ModPayloads::handlePreset)
                .playToServer(RemoveRoutePayload.TYPE, RemoveRoutePayload.STREAM_CODEC,
                        ModPayloads::handleRemoveRoute)
                .playToServer(ReorderRoutePayload.TYPE, ReorderRoutePayload.STREAM_CODEC,
                        ModPayloads::handleReorderRoute)
                .playToServer(SetFleetChannelPayload.TYPE, SetFleetChannelPayload.STREAM_CODEC,
                        ModPayloads::handleSetFleetChannel)
                .playToServer(ExportRouteToCardPayload.TYPE, ExportRouteToCardPayload.STREAM_CODEC,
                        ModPayloads::handleExportRouteToCard)
                .playToServer(ImportRouteFromCardPayload.TYPE,
                        ImportRouteFromCardPayload.STREAM_CODEC,
                        ModPayloads::handleImportRouteFromCard)
                .playToServer(PushToFleetPayload.TYPE, PushToFleetPayload.STREAM_CODEC,
                        ModPayloads::handlePushToFleet)
                .playToServer(SetFaceSourcePayload.TYPE, SetFaceSourcePayload.STREAM_CODEC,
                        ModPayloads::handleSetFaceSource)
                .playToServer(ScrollFacePayload.TYPE, ScrollFacePayload.STREAM_CODEC,
                        ModPayloads::handleScrollFace)
                .playToServer(ToggleDriveModePayload.TYPE, ToggleDriveModePayload.STREAM_CODEC,
                        ModPayloads::handleToggleDriveMode)
                .playToServer(ToggleFlightStrategyPayload.TYPE,
                        ToggleFlightStrategyPayload.STREAM_CODEC,
                        ModPayloads::handleToggleFlightStrategy)
                .playToServer(ToggleDriveLockPayload.TYPE, ToggleDriveLockPayload.STREAM_CODEC,
                        ModPayloads::handleToggleDriveLock)
                .playToServer(CycleHeadingPayload.TYPE, CycleHeadingPayload.STREAM_CODEC,
                        ModPayloads::handleCycleHeading)
                .playToServer(ToggleSteerInvertPayload.TYPE, ToggleSteerInvertPayload.STREAM_CODEC,
                        ModPayloads::handleToggleSteerInvert)
                .playToServer(CycleHeadingOffsetPayload.TYPE,
                        CycleHeadingOffsetPayload.STREAM_CODEC,
                        ModPayloads::handleCycleHeadingOffset)
                .playToServer(SetHeadingOffsetPayload.TYPE,
                        SetHeadingOffsetPayload.STREAM_CODEC,
                        ModPayloads::handleSetHeadingOffset)
                .playToServer(SetPidPayload.TYPE, SetPidPayload.STREAM_CODEC,
                        ModPayloads::handleSetPid)
                // S2C：导航 HUD（必须在服务端也注册，否则握手失败）
                .playToClient(NavigationHudPayload.TYPE, NavigationHudPayload.STREAM_CODEC,
                        ModPayloads::handleHud);
        // 卡片名字 / 只读标记（同一 registrar 里续注册）
        event.registrar("1")
                .playToServer(SetCardSettingsPayload.TYPE, SetCardSettingsPayload.STREAM_CODEC,
                        ModPayloads::handleSetCardSettings);
    }

    /** S2C：把 HUD 数据交给客户端缓存（仅客户端执行） */
    private static void handleHud(NavigationHudPayload payload, IPayloadContext context) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist
                == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.hzldm.createnavigationterminal.client.NavigationHudOverlay.accept(payload);
        }
    }

    // ------------------------------------------------------------------
    // C2S 处理
    // ------------------------------------------------------------------

    /** 设置 / 清除导航目标（name 为空即清除） */
    private static void handleSetTarget(SetNavigationTargetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                // 关键诊断：客户端发包坐标在服务端找不到终端（坐标编解码不一致时就是这里挂）
                CreateNavigationTerminal.LOGGER.warn(
                        "[Nav] 收到目标但找不到终端！payload.pos={} 目标={} ({},{},{})",
                        payload.pos(), payload.name(), payload.x(), payload.y(), payload.z());
                return;
            }
            // 驾驶锁：自动模式下拒绝改目标，直接告诉玩家怎么解锁
            if (!terminal.canEditTarget()) {
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§c\u81ea\u52a8\u9a7e\u9a76\u4e2d\u5df2\u9501\u5b9a\u76ee\u6807"
                                            + "\uff0c\u5148\u5207\u5230\u624b\u52a8\u6216\u5173\u95ed\u9a7e\u9a76\u9501"),
                            true);
                }
                return;
            }
            String name = payload.name() == null ? "" : payload.name().trim();
            if (name.isEmpty()) {
                // 清目标同时清空航线，避免航线残留导致到达后自动跳回去
                terminal.clearRoute();
                return;
            }
            // 设单目标：覆盖航线
            terminal.clearRoute();
            int x = Mth.clamp(payload.x(), -30_000_000, 30_000_000);
            int y = Mth.clamp(payload.y(), -2_048, 2_048);
            int z = Mth.clamp(payload.z(), -30_000_000, 30_000_000);
            String dim = payload.dimension() == null || payload.dimension().isEmpty()
                    ? context.player().level().dimension().location().toString()
                    : payload.dimension();
            CreateNavigationTerminal.LOGGER.info("[Nav] 设置目标 pos={} -> {} ({},{},{})",
                    terminal.getBlockPos(), name, x, y, z);
            terminal.setTarget(name, x, y, z, dim);
        });
    }

    /** 修改输出参数（BE 内部二次 clamp） */
    private static void handleSetSettings(SetNavigationSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                return;
            }
            terminal.setSettings(payload.mode(), payload.maxPower(),
                    payload.maxRange(), payload.arriveRadius(), payload.arriveAction(),
                    payload.decelDistance(), payload.steerScaleDeg());
        });
    }

    /** 复位「已到达」锁存 */

    private static void handleResetArrived(ResetArrivedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.resetArrived();
            }
        });
    }

    /** 按坐标从航线移除 */
    private static void handleRemoveRoute(RemoveRoutePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.removeRouteAt(payload.x(), payload.y(), payload.z());
            }
        });
    }

    /** 修改卡片名字 / 只读标记 */
    private static void handleSetCardSettings(SetCardSettingsPayload payload,
                                              IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.updateCardSettings(payload.name(), payload.readOnly());
            }
        });
    }

    /** 把航线导出到槽里的卡 */
    private static void handleExportRouteToCard(ExportRouteToCardPayload payload,
                                                IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.exportRouteToCard();
            }
        });
    }

    /** 切换自动/手动驾驶 */
    private static void handleToggleDriveMode(ToggleDriveModePayload payload,
                                              IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.toggleDriveMode();
            }
        });
    }

    /** 设置 PID 参数 */
    private static void handleSetPid(SetPidPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null || !Double.isFinite(payload.value())) {
                return;
            }
            switch (payload.which()) {
                case 0 -> terminal.setKp(payload.value());
                case 1 -> terminal.setKi(payload.value());
                case 2 -> terminal.setKd(payload.value());
                default -> {
                }
            }
        });
    }

    /** 直接设置航向校准 */
    private static void handleSetHeadingOffset(SetHeadingOffsetPayload payload,
                                               IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null && Double.isFinite(payload.degrees())) {
                terminal.setHeadingOffsetDeg(payload.degrees());
            }
        });
    }

    /** 调整航向校准 */
    private static void handleCycleHeadingOffset(CycleHeadingOffsetPayload payload,
                                                 IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.cycleHeadingOffset(payload.delta());
            }
        });
    }

    /** 切换转向反向 */
    private static void handleToggleSteerInvert(ToggleSteerInvertPayload payload,
                                                IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.toggleSteerInverted();
            }
        });
    }

    /** 切换机头朝向校准 */
    private static void handleCycleHeading(CycleHeadingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.cycleHeadingOverride(payload.delta());
            }
        });
    }

    /** 切换驾驶锁 */
    private static void handleToggleDriveLock(ToggleDriveLockPayload payload,
                                              IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.toggleDriveLock();
            }
        });
    }

    /** 切换飞行策略 */
    private static void handleToggleFlightStrategy(ToggleFlightStrategyPayload payload,
                                                   IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.toggleFlightStrategy();
            }
        });
    }

    /** 滚轮切换选中面的通道 */
    private static void handleScrollFace(ScrollFacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                return;
            }
            int f = terminal.scrollEditingFace(payload.delta());
            if (f >= 0) {
                String faceCn = switch (f) {
                    case 0 -> "\u4e0b";
                    case 1 -> "\u4e0a";
                    case 2 -> "\u5317";
                    case 3 -> "\u5357";
                    case 4 -> "\u897f";
                    case 5 -> "\u4e1c";
                    default -> "?";
                };
                String srcCn = NavigationTerminalBlockEntity.sourceName(terminal.getFaceSource(f));
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§e" + faceCn + "\u9762 §7\u2192 §f" + srcCn), true);
                }
            }
        });
    }

    /** 设置某个面的红石信号源 */
    private static void handleSetFaceSource(SetFaceSourcePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.setFaceSource(payload.face(), payload.source());
            }
        });
    }

    /** 推送目标/航线到同频道编队 */
    private static void handlePushToFleet(PushToFleetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                return;
            }
            if (payload.action() == PushToFleetPayload.PUSH_TARGET) {
                terminal.pushTargetToFleet();
            } else if (payload.action() == PushToFleetPayload.PUSH_ROUTE) {
                terminal.pushRouteToFleet();
            }
        });
    }

    /** 从导航点卡读取航线 */
    private static void handleImportRouteFromCard(ImportRouteFromCardPayload payload,
                                                  IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.importRouteFromCard();
            }
        });
    }

    /** 设置编队频道 */
    private static void handleSetFleetChannel(SetFleetChannelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.setFleetChannel(payload.channel());
            }
        });
    }

    /** 重排航线：把 from 位置的站移到 to 位置 */
    private static void handleReorderRoute(ReorderRoutePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal != null) {
                terminal.moveRoute(payload.from(), payload.to());
            }
        });
    }

    /** 航线编辑：上一站 / 下一站 / 删除当前站 */
    private static void handleRouteEdit(RouteEditPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                return;
            }
            switch (payload.action()) {
                case RouteEditPayload.PREV -> terminal.prevRouteTarget();
                case RouteEditPayload.NEXT -> terminal.nextRouteTarget();
                case RouteEditPayload.REMOVE -> terminal.removeCurrentRoute();
                case RouteEditPayload.TOGGLE_LOOP -> terminal.toggleRouteLoop();
                default -> {
                }
            }
        });
    }

    /** 预设：应用 / 保存 */
    private static void handlePreset(PresetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                return;
            }
            if (payload.action() == PresetPayload.SAVE) {
                terminal.savePreset(payload.slot());
                CreateNavigationTerminal.LOGGER.info("[Nav] 预设 {} 已保存", payload.slot() + 1);
            } else {
                terminal.applyPreset(payload.slot());
            }
        });
    }

    /** 追加一个点到航线 */
    private static void handleAddRoute(AddRoutePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                CreateNavigationTerminal.LOGGER.warn("[Nav] 加入航线失败：找不到终端 {}", payload.pos());
                return;
            }
            // 驾驶锁：自动模式下拒绝改目标，直接告诉玩家怎么解锁
            if (!terminal.canEditTarget()) {
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§c\u81ea\u52a8\u9a7e\u9a76\u4e2d\u5df2\u9501\u5b9a\u76ee\u6807"
                                            + "\uff0c\u5148\u5207\u5230\u624b\u52a8\u6216\u5173\u95ed\u9a7e\u9a76\u9501"),
                            true);
                }
                return;
            }
            String name = payload.name() == null ? "" : payload.name().trim();
            if (name.isEmpty()) {
                return;
            }
            terminal.addRouteTarget(new WpTarget(
                    name.length() > 64 ? name.substring(0, 64) : name,
                    Mth.clamp(payload.x(), -30_000_000, 30_000_000),
                    Mth.clamp(payload.y(), -2_048, 2_048),
                    Mth.clamp(payload.z(), -30_000_000, 30_000_000),
                    payload.dimension() == null || payload.dimension().isEmpty()
                            ? "minecraft:overworld" : payload.dimension()));
            CreateNavigationTerminal.LOGGER.info("[Nav] 航线新增 {}（当前 {} 站）",
                    name, terminal.getRoute().size());
        });
    }

    /** 在终端界面上绑定当前座椅 */
    private static void handleBindSeat(BindSeatPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            NavigationTerminalBlockEntity terminal = resolveTerminal(payload.pos(), context);
            if (terminal == null) {
                CreateNavigationTerminal.LOGGER.warn("[Nav] 绑定座椅失败：找不到终端 {}", payload.pos());
                return;
            }
            var vehicle = sp.getVehicle();
            if (vehicle == null) {
                sp.displayClientMessage(
                        Component.translatable("create_navigation_terminal.binder.need_seat"), true);
                return;
            }
            String uuid = vehicle.getUUID().toString();
            terminal.setBoundSeatUuid(uuid);
            terminal.setWorldRef(sp.getX(), sp.getY(), sp.getZ());
            sp.displayClientMessage(
                    Component.translatable("create_navigation_terminal.binder.bound", uuid.substring(0, 8)), true);
            CreateNavigationTerminal.LOGGER.info("[Nav] 界面绑定 vehicle={} -> terminal={}（真实坐标 {},{},{}）",
                    uuid, terminal.getBlockPos(), (int) sp.getX(), (int) sp.getY(), (int) sp.getZ());
        });
    }

    /** 把勾选的导航点写入手持的导航点卡 */
    private static void handleSaveCard(SaveWaypointCardPayload payload, IPayloadContext context) {        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!(stack.getItem() instanceof WaypointCardItem)) {
                return;
            }
            List<WpTarget> clean = payload.entries().stream()
                    .limit(WaypointCardData.MAX_ENTRIES)
                    .filter(t -> t.name() != null && !t.name().isBlank())
                    .map(t -> new WpTarget(
                            t.name().length() > 64 ? t.name().substring(0, 64) : t.name(),
                            Mth.clamp(t.x(), -30_000_000, 30_000_000),
                            Mth.clamp(t.y(), -2_048, 2_048),
                            Mth.clamp(t.z(), -30_000_000, 30_000_000),
                            t.dimension() == null || t.dimension().isEmpty()
                                    ? "minecraft:overworld" : t.dimension()))
                    .toList();
            stack.set(ModDataComponents.WAYPOINT_CARD, new WaypointCardData(clean));
        });
    }

    // ------------------------------------------------------------------
    // 校验
    // ------------------------------------------------------------------

    /** 方块类型 + 距离双重校验，失败返回 null（伪造包 / 过期包静默丢弃） */
    private static NavigationTerminalBlockEntity resolveTerminal(BlockPos pos, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || pos == null) {
            return null;
        }

        // ① 优先玩家所在 level（普通方块就在这一层）
        NavigationTerminalBlockEntity terminal = null;
        if (player.level().getBlockEntity(pos) instanceof NavigationTerminalBlockEntity t) {
            terminal = t;
        }

        // ② 找不到就扫其它已加载 level —— 终端装在 Sable SubLevel（载具内部）时，
        //    玩家可能站在主世界里点界面，此时 pos 是 SubLevel 局部坐标，第 ① 步必然落空。
        if (terminal == null && player.getServer() != null) {
            for (ServerLevel lvl : player.getServer().getAllLevels()) {
                if (lvl == player.level()) {
                    continue;
                }
                if (lvl.getBlockEntity(pos) instanceof NavigationTerminalBlockEntity t) {
                    terminal = t;
                    break;
                }
            }
        }
        if (terminal == null) {
            return null;
        }

        // ③ 距离校验必须用「世界坐标」——SubLevel 里 terminal.getBlockPos() 是局部坐标
        //    （实测 20481032,128,20481032），拿它跟玩家世界坐标相减会得到天文数字，
        //    结果所有点击都被判成“太远”而静默丢弃（设目标 / 加航线 / 加当前位置全部失效）。
        double dsq;
        if (SableTransform.isAvailable() && SableTransform.isInSubLevel(terminal)) {
            dsq = player.distanceToSqr(SableTransform.worldCenter(terminal));
        } else if (terminal.getLevel() != player.level()) {
            return null;      // 跨 level 但不是 SubLevel：不放行
        } else {
            dsq = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }

        if (dsq > MAX_INTERACT_DISTANCE_SQR) {
            CreateNavigationTerminal.LOGGER.warn(
                    "[Nav] 交互被拒：距离平方={} > {}（terminal={} level={}）",
                    String.format(java.util.Locale.ROOT, "%.1f", dsq),
                    MAX_INTERACT_DISTANCE_SQR, pos, terminal.getLevel().dimension().location());
            return null;
        }
        return terminal;
    }
}
