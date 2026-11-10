package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：把一个导航点追加到终端的航线里。
 *
 * 界面里对列表项「右键」触发；左键仍是设单目标（会清空航线）。
 */
public record AddRoutePayload(
        BlockPos pos,
        String name,
        int x,
        int y,
        int z,
        String dimension
) implements CustomPacketPayload {

    public static final Type<AddRoutePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "add_route"));

    public static final StreamCodec<FriendlyByteBuf, AddRoutePayload> STREAM_CODEC =
            StreamCodec.of(AddRoutePayload::encode, AddRoutePayload::decode);

    private static void encode(FriendlyByteBuf buf, AddRoutePayload p) {
        buf.writeBlockPos(p.pos());
        buf.writeUtf(p.name(), 64);
        buf.writeVarInt(p.x());
        buf.writeVarInt(p.y());
        buf.writeVarInt(p.z());
        buf.writeUtf(p.dimension(), 128);
    }

    private static AddRoutePayload decode(FriendlyByteBuf buf) {
        return new AddRoutePayload(
                buf.readBlockPos(),
                buf.readUtf(64),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(128));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
