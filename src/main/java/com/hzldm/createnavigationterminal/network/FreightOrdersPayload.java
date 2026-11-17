package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.item.WpTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：卡槽里的 AeroFreight 货运清单对应的进行中订单。
 *
 * <p>卡槽内容不同步，订单数据又只存在于服务端，因此打开终端时由服务端把
 * 订单目的地推给客户端，客户端据此填充「货运清单」候选列表。
 */
public record FreightOrdersPayload(List<WpTarget> orders) implements CustomPacketPayload {

    public static final Type<FreightOrdersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "freight_orders"));

    public static final StreamCodec<FriendlyByteBuf, FreightOrdersPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeVarInt(p.orders().size());
                        for (WpTarget t : p.orders()) {
                            buf.writeUtf(t.name(), 96);
                            buf.writeBlockPos(new BlockPos(t.x(), t.y(), t.z()));
                            buf.writeUtf(t.dimension(), 128);
                        }
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        List<WpTarget> list = new ArrayList<>(Math.min(n, 256));
                        for (int i = 0; i < n; i++) {
                            String name = buf.readUtf(96);
                            BlockPos pos = buf.readBlockPos();
                            String dim = buf.readUtf(128);
                            list.add(new WpTarget(name, pos.getX(), pos.getY(), pos.getZ(), dim));
                        }
                        return new FreightOrdersPayload(List.copyOf(list));
                    });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
