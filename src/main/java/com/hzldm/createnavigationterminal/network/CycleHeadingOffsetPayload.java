package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** 调整航向校准偏移（每次 ±15 度） */
public record CycleHeadingOffsetPayload(BlockPos pos, int delta) implements CustomPacketPayload {

    public static final Type<CycleHeadingOffsetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "cycle_heading_offset"));

    public static final StreamCodec<FriendlyByteBuf, CycleHeadingOffsetPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.delta());
                    },
                    buf -> new CycleHeadingOffsetPayload(buf.readBlockPos(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
