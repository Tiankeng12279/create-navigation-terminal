package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** 直接设置航向校准偏移（度） */
public record SetHeadingOffsetPayload(BlockPos pos, double degrees)
        implements CustomPacketPayload {

    public static final Type<SetHeadingOffsetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "set_heading_offset"));

    public static final StreamCodec<FriendlyByteBuf, SetHeadingOffsetPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeDouble(p.degrees());
                    },
                    buf -> new SetHeadingOffsetPayload(buf.readBlockPos(), buf.readDouble()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
