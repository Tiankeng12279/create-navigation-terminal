package com.hzldm.createnavigationterminal.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 单个导航点（卡片与导航台共用的数据单元）
 */
public record WpTarget(String name, int x, int y, int z, String dimension) {

    public static final Codec<WpTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(WpTarget::name),
            Codec.INT.fieldOf("x").forGetter(WpTarget::x),
            Codec.INT.fieldOf("y").forGetter(WpTarget::y),
            Codec.INT.fieldOf("z").forGetter(WpTarget::z),
            Codec.STRING.fieldOf("dim").forGetter(WpTarget::dimension)
    ).apply(i, WpTarget::new));

    // 手写 StreamCodec：字段超过 6 个时 composite 重载不够用，手写最稳
    public static final StreamCodec<FriendlyByteBuf, WpTarget> STREAM_CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.name(), 64);
                buf.writeVarInt(v.x());
                buf.writeVarInt(v.y());
                buf.writeVarInt(v.z());
                buf.writeUtf(v.dimension(), 128);
            },
            buf -> new WpTarget(
                    buf.readUtf(64),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(128)
            ));
}
