package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：按坐标把某个点从航线中移除（右键菜单用）
 */
public record RemoveRoutePayload(BlockPos pos, int x, int y, int z) implements CustomPacketPayload {

    public static final Type<RemoveRoutePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "remove_route"));

    public static final StreamCodec<FriendlyByteBuf, RemoveRoutePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.x());
                        buf.writeVarInt(p.y());
                        buf.writeVarInt(p.z());
                    },
                    buf -> new RemoveRoutePayload(buf.readBlockPos(),
                            buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
