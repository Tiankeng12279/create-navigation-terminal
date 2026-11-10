package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导航 HUD 服务端推送
 *
 * 每 10 tick 检查一次在线玩家：
 *  - 玩家骑着的载具 UUID == 某个导航终端的 boundSeatUuid，且该终端有目标 → 推 active=true
 *  - 否则 → 推 active=false（客户端清空 HUD）
 *
 * 这样 HUD 只会在「绑定的座椅」上出现。
 */
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID)
public final class NavigationHudService {

    /** 推送间隔（tick）：2 = 0.1 秒，兼顾流畅度与带宽 */
    private static final int INTERVAL = 2;

    /** 上一次的 HUD 状态（用于只在变化时打日志，避免刷屏） */
    private static final Map<UUID, String> LAST_STATE = new ConcurrentHashMap<>();

    private NavigationHudService() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            NavigationTerminalBlockEntity match = findMatchedTerminal(player);
            logStateChange(player, match);

            if (match != null) {
                // 玩家正坐在绑定载具上：用他的真实世界坐标刷新参考点
                // （SubLevel 内的方块坐标是虚拟的，不能直接用来算距离）
                match.setWorldRef(player.getX(), player.getY(), player.getZ());

                // 航线其他站点（当前目标另外标注，用不同颜色显示）
                java.util.List<int[]> others = new java.util.ArrayList<>();
                java.util.List<com.hzldm.createnavigationterminal.item.WpTarget> route = match.getRoute();
                int curIdx = match.getRouteIndex();
                for (int i = 0; i < route.size() && others.size() < 32; i++) {
                    var wp = route.get(i);
                    others.add(new int[]{wp.x(), wp.y(), wp.z(), i == curIdx ? 1 : 0});
                }

                // 编队：附近其他「已绑定座椅」的导航终端
                java.util.List<int[]> fleetPos = new java.util.ArrayList<>();
                java.util.List<String> fleetNames = new java.util.ArrayList<>();
                java.util.List<Boolean> fleetFlags = new java.util.ArrayList<>();
                java.util.List<int[]> beaconPos = new java.util.ArrayList<>();
                java.util.List<String> beaconNames = new java.util.ArrayList<>();

                for (var other : NavigationTerminalBlockEntity.active()) {
                    if (other == match) {
                        continue;
                    }
                    if (other.getLevel() != player.level()) {
                        continue;
                    }
                    if (!other.hasWorldRef()) {
                        continue;
                    }
                    String seat = other.getBoundSeatUuid();
                    boolean bound = seat != null && !seat.isEmpty();
                    if (!bound) {
                        continue;
                    }
                    double ox = other.refXOrPos();
                    double oy = other.refYOrPos();
                    double oz = other.refZOrPos();
                    if (player.distanceToSqr(ox, oy, oz) > 256.0 * 256.0) {
                        continue;
                    }
                    if (fleetPos.size() >= 16) {
                        continue;
                    }
                    fleetPos.add(new int[]{(int) ox, (int) oy, (int) oz});
                    fleetNames.add(other.getTerminalName());
                    fleetFlags.add(other.getFleetChannel() == match.getFleetChannel()
                            && other.isInFleet() && match.isInFleet());
                }

                // 信标：只带铭牌、没有导航终端的载具（含 SubLevel 内部）
                try {
                    scanNameplates(player, beaconPos, beaconNames, 6);
                } catch (Throwable ignored) {
                    // 扫描失败忽略
                }

                // 同频道终端总数（不受「绑定座椅」和距离限制），供界面显示编队规模
                int fleetPeerCount = 0;
                if (match.isInFleet()) {
                    fleetPeerCount = com.hzldm.createnavigationterminal.fleet.FleetRegistry
                            .sameChannel(match.getLevel().dimension(),
                                    match.getFleetChannel(), match.getBlockPos(), 0)
                            .size();
                }

                PacketDistributor.sendToPlayer(player, new NavigationHudPayload(
                        true,
                        match.getBlockPos(),
                        match.getTargetName(),
                        match.getTargetX(),
                        match.getTargetY(),
                        match.getTargetZ(),
                        match.getTargetDimension(),
                        match.isArrived(),
                        match.getArriveRadius(),
                        match.getRouteIndex(),
                        match.getRoute().size(),
                        match.isRouteLoop(),
                        others,
                        fleetNames,
                        fleetPos,
                        fleetFlags,
                        fleetPeerCount,
                        match.getFleetChannel(),
                        beaconNames,
                        beaconPos));
            } else {
                PacketDistributor.sendToPlayer(player, NavigationHudPayload.inactive());
            }
        }
    }

    /** 只在状态变化时打一条日志，便于排查「HUD 不出现」 */
    private static void logStateChange(ServerPlayer player, NavigationTerminalBlockEntity match) {
        String now;
        if (match != null) {
            now = "ACTIVE@" + match.getBlockPos().toShortString();
        } else {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                now = "no-vehicle";
            } else {
                String uuid = vehicle.getUUID().toString();
                // 骑在载具上但没匹配到：把「玩家坐的」和「终端绑的」都打出来对比
                StringBuilder sb = new StringBuilder("unmatched(坐=")
                        .append(shortUuid(uuid))
                        .append(", 载具=").append(vehicle.getType().toString().replace("minecraft:", ""))
                        .append(")");
                for (NavigationTerminalBlockEntity be : NavigationTerminalBlockEntity.active()) {
                    if (be.getLevel() != player.level()) {
                        continue;
                    }
                    sb.append(" [终端").append(be.getBlockPos().toShortString())
                            .append("绑=").append(shortUuid(be.getBoundSeatUuid()))
                            .append("]");
                }
                now = sb.toString();
            }
        }
        String prev = LAST_STATE.put(player.getUUID(), now);
        if (!now.equals(prev)) {
            CreateNavigationTerminal.LOGGER.info("[HUD] {} 状态: {} -> {}（已加载终端 {} 个）",
                    player.getName().getString(), prev, now, NavigationTerminalBlockEntity.active().size());
        }
    }

    /**
     * 扫描玩家附近区块里的 Aeronautics 铭牌方块。
     *
     * 只把「该位置没有导航终端」的铭牌当作信标 —— 带终端的已经由编队标记负责。
     * 每 2 秒缓存一次，避免每 tick 遍历区块。
     */
    private static long beaconScanAt = 0L;
    private static final java.util.List<int[]> BEACON_POS_CACHE = new java.util.ArrayList<>();
    private static final java.util.List<String> BEACON_NAME_CACHE = new java.util.ArrayList<>();

    private static void scanNameplates(ServerPlayer player,
                                       java.util.List<int[]> outPos,
                                       java.util.List<String> outNames,
                                       int chunkRadius) {
        long now = System.currentTimeMillis();
        if (now - beaconScanAt < 2000L) {
            outPos.addAll(BEACON_POS_CACHE);
            outNames.addAll(BEACON_NAME_CACHE);
            return;
        }
        beaconScanAt = now;
        BEACON_POS_CACHE.clear();
        BEACON_NAME_CACHE.clear();

        var level = player.level();
        int pcx = player.chunkPosition().x;
        int pcz = player.chunkPosition().z;
        outer:
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (BEACON_POS_CACHE.size() >= 24) {
                    break outer;
                }
                var chunk = level.getChunk(pcx + dx, pcz + dz);
                for (var be : chunk.getBlockEntities().values()) {
                    if (BEACON_POS_CACHE.size() >= 24) {
                        break outer;
                    }
                    if (!be.getClass().getName().endsWith(".NameplateBlockEntity")) {
                        continue;
                    }
                    BlockPos bp = be.getBlockPos();
                    String nm = null;
                    try {
                        var m = be.getClass().getMethod("getName");
                        Object v = m.invoke(be);
                        if (v instanceof String s && !s.isBlank()) {
                            nm = s;
                        }
                    } catch (Throwable ignored) {
                        // 反射失败跳过
                    }
                    if (nm == null) {
                        continue;
                    }
                    if (level.getBlockEntity(bp) instanceof NavigationTerminalBlockEntity) {
                        continue;
                    }
                    BEACON_POS_CACHE.add(new int[]{bp.getX(), bp.getY(), bp.getZ()});
                    BEACON_NAME_CACHE.add(nm);
                }
            }
        }

        outPos.addAll(BEACON_POS_CACHE);
        outNames.addAll(BEACON_NAME_CACHE);

        // ② Sable SubLevel：载具内部的铭牌（主世界扫不到，必须进每个 SubLevel 的 Level）
        scanSubLevelNameplates(player, outPos, outNames);

        if (BEACON_POS_CACHE.size() != lastBeaconCount) {
            lastBeaconCount = BEACON_POS_CACHE.size();
            CreateNavigationTerminal.LOGGER.info(
                    "[Nav] 信标扫描：{} 个（含 SubLevel）", BEACON_POS_CACHE.size());
        }
    }

    /**
     * 遍历所有 Sable SubLevel，扫它们各自 Level 里的铭牌方块。
     *
     * 用 LevelPlot.getCenterBlock() 定位每个 SubLevel 的本地中心，
     * 再按区块遍历读 blockEntities —— 比逐格 getBlockEntity 快几十倍。
     *
     * 全程反射，Sable 不在也不报错。
     */
    private static void scanSubLevelNameplates(ServerPlayer player,
                                               java.util.List<int[]> outPos,
                                               java.util.List<String> outNames) {
        try {
            Class<?> cc = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Object container = cc
                    .getMethod("getContainer", net.minecraft.server.level.ServerLevel.class)
                    .invoke(null, player.level());
            if (container == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            var subs = (java.util.Collection<Object>) cc.getMethod("getAllSubLevels").invoke(container);
            if (subs == null) {
                return;
            }

            for (Object sub : subs) {
                if (BEACON_POS_CACHE.size() >= 32) {
                    return;
                }
                Object plot;
                net.minecraft.core.BlockPos center;
                net.minecraft.world.level.Level sl;
                try {
                    plot = sub.getClass().getMethod("getPlot").invoke(sub);
                    center = (net.minecraft.core.BlockPos)
                            plot.getClass().getMethod("getCenterBlock").invoke(plot);
                    sl = (net.minecraft.world.level.Level)
                            sub.getClass().getMethod("getLevel").invoke(sub);
                } catch (Throwable t) {
                    continue;
                }
                if (center == null || sl == null) {
                    continue;
                }

                int r = 40;
                int cx0 = (center.getX() - r) >> 4;
                int cx1 = (center.getX() + r) >> 4;
                int cz0 = (center.getZ() - r) >> 4;
                int cz1 = (center.getZ() + r) >> 4;

                // 取该 SubLevel 的世界位姿，用于把本地坐标转成世界坐标
                Object pose = null;
                try {
                    pose = sub.getClass().getMethod("logicalPose").invoke(sub);
                } catch (Throwable ignored) {
                    // 拿不到位姿就用本地坐标
                }

                // 先判断这艘 SubLevel 上有没有导航终端 —— 有就整船跳过，
                // 因为它是编队成员，不该同时当信标
                boolean hasTerminal = false;
                for (int cx = cx0; cx <= cx1 && !hasTerminal; cx++) {
                    for (int cz = cz0; cz <= cz1 && !hasTerminal; cz++) {
                        try {
                            var chk = sl.getChunk(cx, cz);
                            for (var b : chk.getBlockEntities().values()) {
                                if (b instanceof NavigationTerminalBlockEntity) {
                                    hasTerminal = true;
                                    break;
                                }
                            }
                        } catch (Throwable ignored) {
                            // 区块没加载，跳过
                        }
                    }
                }
                if (hasTerminal) {
                    continue;
                }

                for (int cx = cx0; cx <= cx1; cx++) {
                    for (int cz = cz0; cz <= cz1; cz++) {
                        if (BEACON_POS_CACHE.size() >= 32) {
                            return;
                        }
                        net.minecraft.world.level.chunk.LevelChunk chunk;
                        try {
                            chunk = sl.getChunk(cx, cz);
                        } catch (Throwable t) {
                            continue;
                        }
                        for (var be : chunk.getBlockEntities().values()) {
                            if (BEACON_POS_CACHE.size() >= 32) {
                                return;
                            }
                            if (!be.getClass().getName().endsWith(".NameplateBlockEntity")) {
                                continue;
                            }
                            // 铭牌是多方块结构：只保留 controller，其余分片跳过，
                            // 否则同一块牌子会被画成好几个标记
                            try {
                                Object ctrl = be.getClass().getMethod("isController").invoke(be);
                                if (Boolean.FALSE.equals(ctrl)) {
                                    continue;
                                }
                            } catch (Throwable ignored) {
                                // 没这个方法就不过滤
                            }
                            String nm = null;
                            try {
                                Object v = be.getClass().getMethod("getName").invoke(be);
                                if (v instanceof String s && !s.isBlank()) {
                                    nm = s;
                                }
                            } catch (Throwable ignored) {
                                // 反射失败
                            }
                            if (nm == null) {
                                continue;
                            }
                            net.minecraft.core.BlockPos bp = be.getBlockPos();
                            // SubLevel 本地坐标 → 世界坐标
                            double[] world = localToWorld(pose, bp.getX(), bp.getY(), bp.getZ());

                            // 同名且相近的重复项直接跳过（多方块残留）
                            boolean dup = false;
                            for (int k = 0; k < BEACON_POS_CACHE.size(); k++) {
                                if (!nm.equals(BEACON_NAME_CACHE.get(k))) {
                                    continue;
                                }
                                int[] q = BEACON_POS_CACHE.get(k);
                                if (Math.abs(q[0] - world[0]) < 8
                                        && Math.abs(q[1] - world[1]) < 8
                                        && Math.abs(q[2] - world[2]) < 8) {
                                    dup = true;
                                    break;
                                }
                            }
                            if (dup) {
                                continue;
                            }
                            BEACON_POS_CACHE.add(new int[]{
                                    (int) Math.round(world[0]),
                                    (int) Math.round(world[1]),
                                    (int) Math.round(world[2])});
                            BEACON_NAME_CACHE.add(nm);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            CreateNavigationTerminal.LOGGER.debug("[Nav] SubLevel 铭牌扫描跳过：{}", t.toString());
        }
    }

    private static int lastBeaconCount = -1;

    /**
     * SubLevel 本地坐标 → 世界坐标。
     *
     * Pose3d 类不在 sable 本体里（依赖 veil），所以全程反射。
     * 优先找 transformPosition(Vector3dc) / transformPosition(Vector3d)，
     * 拿不到位姿或方法就退回原样（位置会偏，但不会崩）。
     */
    private static double[] localToWorld(Object pose, double lx, double ly, double lz) {
        if (pose == null) {
            return new double[]{lx, ly, lz};
        }
        try {
            var v = new org.joml.Vector3d(lx, ly, lz);
            for (var m : pose.getClass().getMethods()) {
                String name = m.getName();
                if (!name.equals("transformPosition") && !name.equals("transform")
                        && !name.equals("apply")) {
                    continue;
                }
                if (m.getParameterCount() != 1) {
                    continue;
                }
                Class<?> p = m.getParameterTypes()[0];
                if (!p.isAssignableFrom(org.joml.Vector3d.class)
                        && !p.getName().contains("Vector3")) {
                    continue;
                }
                Object out = m.invoke(pose, new org.joml.Vector3d(v));
                if (out instanceof org.joml.Vector3d vd) {
                    return new double[]{vd.x, vd.y, vd.z};
                }
            }
        } catch (Throwable ignored) {
            // 反射失败退回原坐标
        }
        return new double[]{lx, ly, lz};
    }

    /** UUID 前 8 位，日志里短一点 */
    private static String shortUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return "-";
        }
        return uuid.substring(0, Math.min(8, uuid.length()));
    }

    /**
     * 玩家当前骑乘的载具是否被某个已加载终端绑定。
     *
     * 兼容三种绑定形式：
     *   1) 玩家坐的实体本身
     *   2) 该实体的载具链（Sable/Create 的座椅常常嵌套在载具里）
     *   3) 方块坐标绑定（"block:<packed>"，玩家在方块附近即视为匹配）
     *
     * 不要求终端已设置导航目标 —— 绑上就该显示 HUD（仪表常亮）。
     */
    private static NavigationTerminalBlockEntity findMatchedTerminal(ServerPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return null;
        }

        // 收集载体链上的所有 UUID（玩家坐的实体 + 它的上级载具）
        java.util.Set<String> chain = new java.util.HashSet<>();
        Entity cur = vehicle;
        for (int depth = 0; cur != null && depth < 8; depth++) {
            chain.add(cur.getUUID().toString());
            cur = cur.getVehicle();
        }
        // 也把根载具算上（有些实现里 getRootVehicle 与链路不同）
        try {
            Entity root = vehicle.getRootVehicle();
            if (root != null) {
                chain.add(root.getUUID().toString());
            }
        } catch (Throwable ignored) {
            // 忽略
        }

        for (NavigationTerminalBlockEntity be : NavigationTerminalBlockEntity.active()) {
            if (be.getLevel() != player.level()) {
                continue;
            }
            String bound = be.getBoundSeatUuid();
            if (bound == null || bound.isEmpty()) {
                continue;
            }

            // ① 绑座椅方块坐标：seat 实体每次重建 UUID 会变，
            //    实体 blockPosition 也随坐姿浮动，只有方块坐标稳定。
            //    用 8 格容差容忍载具姿态浮动。
            if (bound.startsWith("seatpos:")) {
                try {
                    BlockPos seatPos = BlockPos.of(Long.parseLong(bound.substring(8)));
                    BlockPos vehPos = vehicle.blockPosition();
                    if (Math.abs(vehPos.getX() - seatPos.getX()) <= 8
                            && Math.abs(vehPos.getY() - seatPos.getY()) <= 8
                            && Math.abs(vehPos.getZ() - seatPos.getZ()) <= 8) {
                        return be;
                    }
                } catch (Throwable ignored) {
                    // 解析失败跳过
                }
            }

            // ② 绑玩家（兼容旧数据）
            if (bound.equals("player:" + player.getUUID())) {
                return be;
            }

            // ③ 绑实体 UUID（旧数据/其他情况）
            if (chain.contains(bound)) {
                return be;
            }
            // 位置绑定：用真实世界坐标做距离匹配（SubLevel 虚拟坐标不可比）
            if (bound.startsWith("pos:")) {
                try {
                    String[] parts = bound.substring(4).split(",");
                    double bx = Double.parseDouble(parts[0]);
                    double by = Double.parseDouble(parts[1]);
                    double bz = Double.parseDouble(parts[2]);
                    if (player.distanceToSqr(bx, by, bz) < 256.0) {  // 16 格内
                        return be;
                    }
                } catch (Throwable ignored) {
                    // 解析失败跳过
                }
            }
            // 兼容旧的方块坐标绑定（虚拟坐标场景下不可用）
            if (bound.startsWith("block:")) {
                try {
                    BlockPos bp = BlockPos.of(Long.parseLong(bound.substring(6)));
                    if (bp.distToCenterSqr(player.position()) < 64.0) {
                        return be;
                    }
                } catch (Throwable ignored) {
                    // 解析失败跳过
                }
            }
        }
        return null;
    }
}
