package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：为导航终端设置 / 清除导航目标
 *
 * 导航点列表来自客户端本地 Xaero，服务端不可见，因此由客户端读取、
 * 玩家点选后发此包；name 为空串表示清除目标。
 */
public record SetNavigationTargetPayload(BlockPos pos, String name, int x, int y, int z, String dimension)
        implements CustomPacketPayload {

    public static final Type<SetNavigationTargetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNavigationTerminal.MODID, "set_navigation_target"));

    public static final StreamCodec<FriendlyByteBuf, SetNavigationTargetPayload> STREAM_CODEC =
            StreamCodec.of(SetNavigationTargetPayload::encode, SetNavigationTargetPayload::decode);

    public static void encode(FriendlyByteBuf buf, SetNavigationTargetPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeUtf(payload.name, 64);
        buf.writeInt(payload.x);
        buf.writeInt(payload.y);
        buf.writeInt(payload.z);
        buf.writeUtf(payload.dimension, 128);
    }

    public static SetNavigationTargetPayload decode(FriendlyByteBuf buf) {
        return new SetNavigationTargetPayload(
                buf.readBlockPos(),
                buf.readUtf(64),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(128)
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
