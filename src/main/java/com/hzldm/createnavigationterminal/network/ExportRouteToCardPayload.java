package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** 客户端→服务端：把当前航线导出到槽里的路径点卡 */
public record ExportRouteToCardPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ExportRouteToCardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "export_route_to_card"));

    public static final StreamCodec<FriendlyByteBuf, ExportRouteToCardPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBlockPos(p.pos()),
                    buf -> new ExportRouteToCardPayload(buf.readBlockPos()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
