package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：把当前目标或整条航线推送给同频道的编队终端。
 *
 * @param action {@link #PUSH_TARGET} 推送目标；{@link #PUSH_ROUTE} 推送整条航线
 */
public record PushToFleetPayload(BlockPos pos, int action) implements CustomPacketPayload {

    /** 只推送当前导航目标 */
    public static final int PUSH_TARGET = 0;
    /** 推送整条航线（替换对方的航线） */
    public static final int PUSH_ROUTE = 1;

    public static final Type<PushToFleetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "push_to_fleet"));

    public static final StreamCodec<FriendlyByteBuf, PushToFleetPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.action());
                    },
                    buf -> new PushToFleetPayload(buf.readBlockPos(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
