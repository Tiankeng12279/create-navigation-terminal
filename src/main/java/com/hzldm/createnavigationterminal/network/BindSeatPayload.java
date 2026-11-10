package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：在某个导航终端界面上点「绑定座椅」。
 *
 * 相比 SeatBinderItem 的「找最近终端」，这个入口明确指定目标终端，
 * 因此 SubLevel 内的终端（坐标虚拟、按距离永远选不中）也能绑上。
 */
public record BindSeatPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<BindSeatPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "bind_seat"));

    public static final StreamCodec<FriendlyByteBuf, BindSeatPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBlockPos(p.pos()),
                    buf -> new BindSeatPayload(buf.readBlockPos()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
