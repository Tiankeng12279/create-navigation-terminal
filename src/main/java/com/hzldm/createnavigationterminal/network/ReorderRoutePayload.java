package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：重排航线（把 from 位置的站移到 to 位置）
 */
public record ReorderRoutePayload(BlockPos pos, int from, int to) implements CustomPacketPayload {

    public static final Type<ReorderRoutePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "reorder_route"));

    public static final StreamCodec<FriendlyByteBuf, ReorderRoutePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.from());
                        buf.writeVarInt(p.to());
                    },
                    buf -> new ReorderRoutePayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
