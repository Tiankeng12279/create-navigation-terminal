package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：预设参数操作
 *
 * action: 0=应用槽位 1=把当前参数存入槽位
 */
public record PresetPayload(BlockPos pos, int action, int slot) implements CustomPacketPayload {

    public static final int APPLY = 0;
    public static final int SAVE = 1;

    public static final Type<PresetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "preset"));

    public static final StreamCodec<FriendlyByteBuf, PresetPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeVarInt(p.action());
                        buf.writeVarInt(p.slot());
                    },
                    buf -> new PresetPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
