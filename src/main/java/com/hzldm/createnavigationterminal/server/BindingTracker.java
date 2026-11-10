package com.hzldm.createnavigationterminal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 绑定中的终端登记表（服务端）
 *
 * 仿照 create-fire-control 的绑定会话：记下终端位置 + 进入时间，
 * 超过 {@link #TIMEOUT_MS} 自动失效，避免玩家忘记后误绑。
 *
 *   ① 扳手右键导航终端 → 记下这台终端
 *   ② 扳手右键座椅/载具 → 完成绑定
 */
public final class BindingTracker {

    /** 绑定会话有效时长 */
    public static final long TIMEOUT_MS = 30_000L;

    /** 玩家 → [终端位置(asLong), 进入时间(ms)] */
    private static final Map<UUID, long[]> PENDING = new ConcurrentHashMap<>();

    private BindingTracker() {
    }

    public static void setPending(Player player, BlockPos pos) {
        if (player == null || pos == null) {
            return;
        }
        PENDING.put(player.getUUID(), new long[]{pos.asLong(), System.currentTimeMillis()});
    }

    /** 取出待绑定终端；已超时则清除并返回 null */
    public static BlockPos getPending(Player player) {
        if (player == null) {
            return null;
        }
        long[] v = PENDING.get(player.getUUID());
        if (v == null) {
            return null;
        }
        if (System.currentTimeMillis() - v[1] > TIMEOUT_MS) {
            PENDING.remove(player.getUUID());
            return null;
        }
        return BlockPos.of(v[0]);
    }

    /** 剩余有效秒数（用于提示） */
    public static int remainingSeconds(Player player) {
        if (player == null) {
            return 0;
        }
        long[] v = PENDING.get(player.getUUID());
        if (v == null) {
            return 0;
        }
        long left = TIMEOUT_MS - (System.currentTimeMillis() - v[1]);
        return left <= 0 ? 0 : (int) (left / 1000L);
    }

    public static BlockPos clearPending(Player player) {
        return player == null ? null : BlockPos.of(PENDING.remove(player.getUUID()) == null
                ? BlockPos.ZERO.asLong()
                : 0);
    }

    public static void remove(Player player) {
        if (player != null) {
            PENDING.remove(player.getUUID());
        }
    }
}
