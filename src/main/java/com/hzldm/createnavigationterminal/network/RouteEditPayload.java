package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：航线编辑
 *
 * action: 0=上一站 1=下一站 2=删除当前站 3=切换单次/循环
 */
public record RouteEditPayload(BlockPos pos, int action) implements CustomPacketPayload {

    public static final int PREV = 0;
    public static final int NEXT = 1;
    public static final int REMOVE = 2;
    public static final int TOGGLE_LOOP = 3;

    public static final Type<RouteEditPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "route_edit"));

    public static final StreamCodec<FriendlyByteBuf, RouteEditPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.action());
                    },
                    buf -> new RouteEditPayload(buf.readBlockPos(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
