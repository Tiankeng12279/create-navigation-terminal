package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** 客户端→服务端：设置某个面的红石信号源 */
public record SetFaceSourcePayload(BlockPos pos, int face, int source)
        implements CustomPacketPayload {

    public static final Type<SetFaceSourcePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "set_face_source"));

    public static final StreamCodec<FriendlyByteBuf, SetFaceSourcePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.face());
                        buf.writeVarInt(p.source());
                    },
                    buf -> new SetFaceSourcePayload(buf.readBlockPos(),
                            buf.readVarInt(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
