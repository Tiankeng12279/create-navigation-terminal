package com.hzldm.createnavigationterminal.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 导航点（航线）卡片数据（物品 DataComponent）。
 *
 * <p>与 Xaero 解耦的纯数据快照：客户端从 Xaero 读到的标记点转成 {@link WpTarget} 列表，
 * 经网络包上传到服务端写入物品组件，之后卡片脱离 Xaero 也能用。
 *
 * <p>卡片级属性：
 * <ul>
 *   <li>{@code name} —— 这条航线的名字（空串 = 未命名，界面显示导航点数量）；</li>
 *   <li>{@code readOnly} —— 只读：终端不能把当前航线**写进**这张卡（防覆盖），但可以读出来用；</li>
 *   <li>{@code color} —— 染料色号（-1 = 不上色，0..15 对应 {@code DyeColor}），
 *       用于给卡片分类：贴图会按该颜色着色。</li>
 * </ul>
 * 三个字段在 NBT 里都是 optional（带默认值），旧存档的卡照常能读。
 */
public record WaypointCardData(List<WpTarget> entries, String name, boolean readOnly, int color) {

    /** 卡片最多存储的导航点数量 */
    public static final int MAX_ENTRIES = 32;

    /** 名字最大长度（网络包也用它校验） */
    public static final int MAX_NAME = 32;

    /** 未上色 */
    public static final int NO_COLOR = -1;

    public static final WaypointCardData EMPTY = new WaypointCardData(List.of(), "", false, NO_COLOR);

    /** 兼容旧调用：只有条目，无名字、非只读、未上色 */
    public WaypointCardData(List<WpTarget> entries) {
        this(entries, "", false, NO_COLOR);
    }

    /** 兼容旧调用：条目 + 名字 + 只读，未上色 */
    public WaypointCardData(List<WpTarget> entries, String name, boolean readOnly) {
        this(entries, name, readOnly, NO_COLOR);
    }

    public WaypointCardData withEntries(List<WpTarget> newEntries) {
        return new WaypointCardData(newEntries, name, readOnly, color);
    }

    public WaypointCardData withName(String newName) {
        return new WaypointCardData(entries, newName == null ? "" : newName, readOnly, color);
    }

    public WaypointCardData withReadOnly(boolean flag) {
        return new WaypointCardData(entries, name, flag, color);
    }

    public WaypointCardData withColor(int newColor) {
        return new WaypointCardData(entries, name, readOnly,
                newColor < 0 || newColor > 15 ? NO_COLOR : newColor);
    }

    /** 是否上过色 */
    public boolean dyed() {
        return color >= 0 && color <= 15;
    }

    /** 显示名：没起名就显示「N 个导航点」 */
    public String displayName() {
        return name == null || name.isBlank() ? entries.size() + " \u4e2a\u5bfc\u822a\u70b9" : name;
    }

    public static final Codec<WaypointCardData> CODEC = RecordCodecBuilder.create(i -> i.group(
            WpTarget.CODEC.listOf().fieldOf("entries").forGetter(WaypointCardData::entries),
            Codec.STRING.optionalFieldOf("name", "").forGetter(WaypointCardData::name),
            Codec.BOOL.optionalFieldOf("readOnly", false).forGetter(WaypointCardData::readOnly),
            Codec.INT.optionalFieldOf("color", NO_COLOR).forGetter(WaypointCardData::color)
    ).apply(i, WaypointCardData::new));

    private static final StreamCodec<FriendlyByteBuf, List<WpTarget>> ENTRY_LIST =
            WpTarget.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES));

    public static final StreamCodec<FriendlyByteBuf, WaypointCardData> STREAM_CODEC =
            StreamCodec.of(
                    (buf, data) -> {
                        ENTRY_LIST.encode(buf, data.entries());
                        buf.writeUtf(data.name() == null ? "" : data.name(), MAX_NAME);
                        buf.writeBoolean(data.readOnly());
                        buf.writeByte(data.color());
                    },
                    buf -> new WaypointCardData(
                            ENTRY_LIST.decode(buf), buf.readUtf(MAX_NAME), buf.readBoolean(),
                            buf.readByte()));
}
