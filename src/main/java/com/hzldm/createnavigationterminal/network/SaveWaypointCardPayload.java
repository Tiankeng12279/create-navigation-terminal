package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import com.hzldm.createnavigationterminal.item.WpTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 客户端→服务端：把勾选的 Xaero 标记点写入主手/副手的导航点卡
 *
 * 服务端必须校验：手持物确实是导航点卡、条目数量/坐标范围都二次 clamp。
 */
public record SaveWaypointCardPayload(InteractionHand hand, List<WpTarget> entries)
        implements CustomPacketPayload {

    public static final Type<SaveWaypointCardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateNavigationTerminal.MODID, "save_waypoint_card"));

    public static final StreamCodec<FriendlyByteBuf, SaveWaypointCardPayload> STREAM_CODEC =
            StreamCodec.of(SaveWaypointCardPayload::encode, SaveWaypointCardPayload::decode);

    public static void encode(FriendlyByteBuf buf, SaveWaypointCardPayload payload) {
        buf.writeEnum(payload.hand);
        WpTarget.STREAM_CODEC.apply(ByteBufCodecs.list(32)).encode(buf, payload.entries);
    }

    public static SaveWaypointCardPayload decode(FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        List<WpTarget> entries = WpTarget.STREAM_CODEC.apply(ByteBufCodecs.list(32)).decode(buf);
        return new SaveWaypointCardPayload(hand, entries);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
