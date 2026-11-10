package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** 客户端→服务端：从槽里的导航点卡读取航线（替换当前航线） */
public record ImportRouteFromCardPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ImportRouteFromCardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "import_route_from_card"));

    public static final StreamCodec<FriendlyByteBuf, ImportRouteFromCardPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBlockPos(p.pos()),
                    buf -> new ImportRouteFromCardPayload(buf.readBlockPos()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
