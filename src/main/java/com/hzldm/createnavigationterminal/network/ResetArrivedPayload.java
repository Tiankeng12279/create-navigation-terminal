package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：复位导航终端的「已到达」状态
 *
 * arrived 是锁存位（进入到达半径后恒为 true，避免边界抖动导致红石线狂闪）。
 * 复位途径：重新选目标 / 清除目标 / 玩家点「重置到达」。
 */
public record ResetArrivedPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ResetArrivedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNavigationTerminal.MODID, "reset_arrived"));

    public static final StreamCodec<FriendlyByteBuf, ResetArrivedPayload> STREAM_CODEC =
            StreamCodec.of(ResetArrivedPayload::encode, ResetArrivedPayload::decode);

    public static void encode(FriendlyByteBuf buf, ResetArrivedPayload payload) {
        buf.writeBlockPos(payload.pos);
    }

    public static ResetArrivedPayload decode(FriendlyByteBuf buf) {
        return new ResetArrivedPayload(buf.readBlockPos());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
