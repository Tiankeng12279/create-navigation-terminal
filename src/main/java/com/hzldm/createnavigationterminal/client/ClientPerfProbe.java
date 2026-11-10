package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.CreateNavigationTerminal;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 轻量性能探针：定位"看向导航终端就掉帧"到底花在哪。
 *
 * <p>只做计数与计时，不参与任何逻辑；每 5 秒打一行（最多 40 行），没数据就不打。
 *
 * <p>判读方式：
 * <pre>
 * [Perf] fps≈.. hud=..ms/帧  getSignal=../../5s  getDirectSignal=..  getShape=..  weakPower=..
 * </pre>
 * - {@code getSignal}/{@code getShape} 每秒上万  → 是别的模组（红石线/管道）在轮询，不是模型；
 * - {@code hud} 每帧几毫秒以上                  → 是 HUD 叠加层；
 * - 三个计数都不高却仍掉帧                       → 就是模型本身的渲染开销（四边形数/剔除）。
 */
@EventBusSubscriber(modid = CreateNavigationTerminal.MODID, value = Dist.CLIENT)
public final class ClientPerfProbe {

    public static final AtomicLong GET_SIGNAL = new AtomicLong();
    public static final AtomicLong GET_DIRECT_SIGNAL = new AtomicLong();
    public static final AtomicLong GET_SHAPE = new AtomicLong();
    public static final AtomicLong WEAK_POWER = new AtomicLong();
    public static final AtomicLong HUD_NANOS = new AtomicLong();
    public static final AtomicLong HUD_FRAMES = new AtomicLong();

    private static int ticks = 0;
    private static int logged = 0;
    /** 行数上限：够做完多轮 A/B（400 行 ≈ 33 分钟） */
    private static final int MAX_LINES = 400;
    /** 低于这个帧率时，即使所有计数为 0 也要打一行 */
    private static final int LOW_FPS = 20;

    private ClientPerfProbe() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (++ticks < 100) {           // 5 秒一次
            return;
        }
        ticks = 0;
        if (logged >= MAX_LINES) {
            return;
        }
        long gs = GET_SIGNAL.getAndSet(0);
        long gd = GET_DIRECT_SIGNAL.getAndSet(0);
        long gsh = GET_SHAPE.getAndSet(0);
        long wp = WEAK_POWER.getAndSet(0);
        long hn = HUD_NANOS.getAndSet(0);
        long hf = HUD_FRAMES.getAndSet(0);

        Minecraft mc = Minecraft.getInstance();
        int fps = mc == null ? 0 : mc.getFps();
        // 关键：**低帧窗口无论计数是否为零都要打**。
        // 上一版是"没调用就不打"，结果 A/B 测试时探针早已耗尽 40 行预算，数据全丢。
        if (gs == 0 && gd == 0 && gsh == 0 && wp == 0 && hf == 0 && fps >= LOW_FPS) {
            return;
        }
        logged++;
        String fpsText = fps > 0 ? String.valueOf(fps) : "?";
        double hudMs = hf > 0 ? (hn / 1.0e6) / hf : 0.0;
        CreateNavigationTerminal.LOGGER.info(
                "[Perf] fps≈{} hud={}ms/帧({}帧) getSignal={}/5s getDirectSignal={} getShape={} weakPower={}",
                fpsText, String.format("%.2f", hudMs), hf, gs, gd, gsh, wp);
    }
}
