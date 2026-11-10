package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** 设置 PID 参数（0=Kp 1=Ki 2=Kd） */
public record SetPidPayload(BlockPos pos, int which, double value) implements CustomPacketPayload {

    public static final Type<SetPidPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "set_pid"));

    public static final StreamCodec<FriendlyByteBuf, SetPidPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.which());
                        buf.writeDouble(p.value());
                    },
                    buf -> new SetPidPayload(buf.readBlockPos(), buf.readVarInt(), buf.readDouble()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
