package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端→客户端：导航 HUD 数据
 *
 * 只有玩家正坐在「被导航终端绑定的座椅」上时才会收到 active=true 的包；
 * 下车或绑定失效时收到 active=false，客户端据此清空 HUD。
 *
 * routePoints    ：航线上除当前目标外的其他站点（HUD 用另一种颜色显示）
 * fleetTerminals ：视野内其他「带导航终端」的载具世界坐标 + 名字（编队用）
 */
public record NavigationHudPayload(
        boolean active,
        BlockPos terminalPos,
        String targetName,
        int targetX,
        int targetY,
        int targetZ,
        String targetDim,
        boolean arrived,
        int arriveRadius,
        int routeIndex,
        int routeTotal,
    /** 航线是否循环：单程航线到末站后不应再回绕显示「下一站」 */
    boolean routeLoop,
        List<int[]> routePoints,
        List<String> fleetNames,
        List<int[]> fleetTerminals,
        List<Boolean> fleetFlags,
    /** 同频道（含未绑定座椅）的编队终端总数，来自服务端 FleetRegistry */
    int fleetPeerCount,
        int myChannel,
        List<String> beaconNames,
        List<int[]> beaconPositions
) implements CustomPacketPayload {

    public static final Type<NavigationHudPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "navigation_hud"));

    public static final StreamCodec<FriendlyByteBuf, NavigationHudPayload> STREAM_CODEC =
            StreamCodec.of(NavigationHudPayload::encode, NavigationHudPayload::decode);

    public static void encode(FriendlyByteBuf buf, NavigationHudPayload p) {
        buf.writeBoolean(p.active);
        buf.writeBlockPos(p.terminalPos);
        buf.writeUtf(p.targetName, 64);
        buf.writeVarInt(p.targetX);
        buf.writeVarInt(p.targetY);
        buf.writeVarInt(p.targetZ);
        buf.writeUtf(p.targetDim, 128);
        buf.writeBoolean(p.arrived);
        buf.writeVarInt(p.arriveRadius);
        buf.writeVarInt(p.routeIndex);
        buf.writeVarInt(p.routeTotal);
        buf.writeBoolean(p.routeLoop);

        List<int[]> pts = p.routePoints == null ? List.of() : p.routePoints;
        buf.writeVarInt(pts.size());
        for (int[] pt : pts) {
            buf.writeVarInt(pt[0]);
            buf.writeVarInt(pt[1]);
            buf.writeVarInt(pt[2]);
            buf.writeBoolean(pt.length > 3 && pt[3] == 1);
        }

        // 编队终端：坐标 + 名字 + 是否在编队
        List<int[]> fleet = p.fleetTerminals == null ? List.of() : p.fleetTerminals;
        List<String> names = p.fleetNames == null ? List.of() : p.fleetNames;
        List<Boolean> flags = p.fleetFlags == null ? List.of() : p.fleetFlags;
        buf.writeVarInt(fleet.size());
        for (int i = 0; i < fleet.size(); i++) {
            int[] f = fleet.get(i);
            buf.writeVarInt(f[0]);
            buf.writeVarInt(f[1]);
            buf.writeVarInt(f[2]);
            buf.writeUtf(i < names.size() ? names.get(i) : "", 64);
            buf.writeBoolean(i < flags.size() && Boolean.TRUE.equals(flags.get(i)));
        }
        buf.writeVarInt(p.myChannel);
        buf.writeVarInt(p.fleetPeerCount);

        // 信标：只有铭牌、没有导航终端的位置
        List<int[]> bcn = p.beaconPositions == null ? List.of() : p.beaconPositions;
        List<String> bcnNames = p.beaconNames == null ? List.of() : p.beaconNames;
        buf.writeVarInt(bcn.size());
        for (int i = 0; i < bcn.size(); i++) {
            int[] b = bcn.get(i);
            buf.writeVarInt(b[0]);
            buf.writeVarInt(b[1]);
            buf.writeVarInt(b[2]);
            buf.writeUtf(i < bcnNames.size() ? bcnNames.get(i) : "", 64);
        }
    }

    public static NavigationHudPayload decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        BlockPos pos = buf.readBlockPos();
        String name = buf.readUtf(64);
        int x = buf.readVarInt();
        int y = buf.readVarInt();
        int z = buf.readVarInt();
        String dim = buf.readUtf(128);
        boolean arrived = buf.readBoolean();
        int radius = buf.readVarInt();
        int idx = buf.readVarInt();
        int total = buf.readVarInt();
        boolean loop = buf.readBoolean();

        int n = Math.min(buf.readVarInt(), 64);
        List<int[]> pts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int px = buf.readVarInt();
            int py = buf.readVarInt();
            int pz = buf.readVarInt();
            boolean cur = buf.readBoolean();
            pts.add(new int[]{px, py, pz, cur ? 1 : 0});
        }

        int fn = Math.min(buf.readVarInt(), 64);
        List<int[]> fleet = new ArrayList<>(fn);
        List<String> fleetNames = new ArrayList<>(fn);
        List<Boolean> fleetFlags = new ArrayList<>(fn);
        for (int i = 0; i < fn; i++) {
            int fx = buf.readVarInt();
            int fy = buf.readVarInt();
            int fz = buf.readVarInt();
            fleet.add(new int[]{fx, fy, fz});
            fleetNames.add(buf.readUtf(64));
            fleetFlags.add(buf.readBoolean());
        }

        int myChannel = buf.readVarInt();
        int fleetPeerCount = buf.readVarInt();

        int bn = Math.min(buf.readVarInt(), 64);
        List<String> beaconNames = new ArrayList<>(bn);
        List<int[]> beaconPos = new ArrayList<>(bn);
        for (int i = 0; i < bn; i++) {
            int bx = buf.readVarInt();
            int by = buf.readVarInt();
            int bz = buf.readVarInt();
            beaconPos.add(new int[]{bx, by, bz});
            beaconNames.add(buf.readUtf(64));
        }

        return new NavigationHudPayload(active, pos, name, x, y, z, dim,
                arrived, radius, idx, total, loop, pts, fleetNames, fleet, fleetFlags,
                fleetPeerCount, myChannel, beaconNames, beaconPos);
    }

    public static NavigationHudPayload inactive() {
        return new NavigationHudPayload(false, BlockPos.ZERO, "", 0, 0, 0,
                "", false, 0, 0, 0, false, List.of(), List.of(), List.of(), List.of(),
                0, 0, List.of(), List.of());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
