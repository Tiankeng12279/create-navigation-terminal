package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 服务端 → 客户端：终端的卡槽里是否放入了 AeroFreight 的货运清单。
 *
 * <p>卡槽内容不在客户端可见（方块实体只同步必要字段），所以打开界面时由服务端告知；
 * 客户端据此把「路径点」候选切换成货运站点/订单。
 */
public record FreightModePayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<FreightModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "freight_mode"));

    public static final StreamCodec<FriendlyByteBuf, FreightModePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBoolean(p.enabled()),
                    buf -> new FreightModePayload(buf.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
