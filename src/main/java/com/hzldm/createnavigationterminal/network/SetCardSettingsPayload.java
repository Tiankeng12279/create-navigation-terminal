package com.hzldm.createnavigationterminal.network;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 客户端→服务端：修改槽里导航点卡的「名字」与「只读」标记。
 *
 * <p>条目本身不动；只读只限制"把当前航线写进卡"。
 */
public record SetCardSettingsPayload(BlockPos pos, String name, boolean readOnly)
        implements CustomPacketPayload {

    public static final int MAX_NAME = 32;

    public static final Type<SetCardSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CreateNavigationTerminal.MODID, "set_card_settings"));

    public static final StreamCodec<FriendlyByteBuf, SetCardSettingsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeBlockPos(p.pos());
                        buf.writeUtf(p.name() == null ? "" : p.name(), MAX_NAME);
                        buf.writeBoolean(p.readOnly());
                    },
                    buf -> new SetCardSettingsPayload(
                            buf.readBlockPos(), buf.readUtf(MAX_NAME), buf.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
