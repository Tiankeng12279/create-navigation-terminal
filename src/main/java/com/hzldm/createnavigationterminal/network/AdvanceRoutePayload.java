package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端 → 服务端：手动确认到达，前往下一站。
 *
 * <p>只在到达动作设为「手动确认」时由按键触发；服务端会再校验一次模式，
 * 避免自动模式下被误推进。
 */
public record AdvanceRoutePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<AdvanceRoutePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "advance_route"));

    public static final StreamCodec<FriendlyByteBuf, AdvanceRoutePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBlockPos(p.pos()),
                    buf -> new AdvanceRoutePayload(buf.readBlockPos()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
