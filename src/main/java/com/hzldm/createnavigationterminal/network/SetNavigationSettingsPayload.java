package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：修改导航终端的输出参数
 *
 *  - mode         0=距离渐变 1=到达开关 2=固定强度
 *  - maxPower     1-15，信号强度上限（模式 2 时即固定输出值）
 *  - maxRange     8-512，覆盖距离，超出归 0
 *  - arriveRadius 0-64，到达判定半径
 *  - arriveAction 0=保持 1=静默停止 2=自动清除目标
 *  - decelDistance 0-512，进入该距离后按剩余距离线性收油门（0=关闭距离减速）
 *
 * 服务端会重新 clamp（客户端只是体验），并保证 maxRange > arriveRadius 以避开除零。
 */
public record SetNavigationSettingsPayload(BlockPos pos, int mode, int maxPower,
                                           int maxRange, int arriveRadius, int arriveAction,
                                           int decelDistance, int steerScaleDeg)
        implements CustomPacketPayload {

    public static final Type<SetNavigationSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNavigationTerminal.MODID, "set_navigation_settings"));

    public static final StreamCodec<FriendlyByteBuf, SetNavigationSettingsPayload> STREAM_CODEC =
            StreamCodec.of(SetNavigationSettingsPayload::encode, SetNavigationSettingsPayload::decode);

    public static void encode(FriendlyByteBuf buf, SetNavigationSettingsPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeVarInt(payload.mode);
        buf.writeVarInt(payload.maxPower);
        buf.writeVarInt(payload.maxRange);
        buf.writeVarInt(payload.arriveRadius);
        buf.writeVarInt(payload.arriveAction);
        buf.writeVarInt(payload.decelDistance);
        buf.writeVarInt(payload.steerScaleDeg);
    }

    public static SetNavigationSettingsPayload decode(FriendlyByteBuf buf) {
        return new SetNavigationSettingsPayload(
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
