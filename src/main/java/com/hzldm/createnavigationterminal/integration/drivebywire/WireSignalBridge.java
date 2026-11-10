package com.hzldm.createnavigationterminal.integration.drivebywire;

import com.hzldm.createnavigationterminal.block.NavigationTerminalBlock;
import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import edn.stratodonut.drivebywire.wire.WireNetworkManager;
import edn.stratodonut.drivebywire.wire.graph.WireNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.util.Map;
import java.util.Set;

/**
 * 把导航终端的操纵信号推送到线控（Drive-By-Wire）网络。
 *
 * <p>四个通道是<b>相对终端朝向</b>的操纵量，直接对应飞行器的执行机构：
 * <pre>
 *   前进 —— 目标在机头方向时的推力，越正对越强
 *   后退 —— 目标在机尾方向时的反向推力
 *   左转 —— 目标在左侧时的偏航量
 *   右转 —— 目标在右侧时的偏航量
 * </pre>
 *
 * <p>强度用 asin 非线性分配（照搬 Create: Aeronautics 导航台的算法），
 * 所以目标正对着某个方向时会急剧增强，稍微偏离就快速衰减。
 *
 * <p><b>线缆是怎么读到这些值的</b>（从 drivebywire-0.3.0 反编译确认，不是猜的）：
 * <pre>
 *   // drivebywire 的 mixin/MixinServerLevel
 *   public int getSignal(BlockPos pos, Direction direction) {
 *       int signal = state.getSignal(this, pos, direction);
 *       BlockPos target = pos.relative(direction.getOpposite());
 *       return Math.max(signal, WireNetworkManager.get(this).getSignalAt(target, direction));
 *   }
 * </pre>
 * 也就是说：<b>接在某个方块 face 上的线缆端点，等价于给那个方块的 face 加了一个红石源</b>。
 * 任何用 {@code level.getSignal(...)}（含 {@code getBestNeighborSignal}）读红石的机器，
 * 只要把线缆端点接在它的某一面，就能读到我们推的值。
 *
 * <p>执行机构实际读的 (方块, 面) 对照表（offroad 的 WheelMountBlockEntity 反编译结果）：
 * <pre>
 *   左转 = level.getSignal(pos.relative(d1), d1) -> 端点 = 轮座自己, 面 = facing 的顺时针侧
 *   右转 = level.getSignal(pos.relative(d2), d2) -> 端点 = 轮座自己, 面 = facing 的逆时针侧
 *   刹车 = level.getSignal(pos.above(), UP)      -> 端点 = 轮座自己, 面 = 顶面
 * </pre>
 * 注意轮座<b>不产生推力</b>：它只有悬挂、转向和刹车。前进/后退必须有别的执行机构
 * （Create 的离合器/换向器 / 热气球燃烧器等真正读红石的方块）。
 *
 * <b>类隔离</b>：本类 import 了 {@code edn.stratodonut.drivebywire.*}，
 * 未安装该模组时 JVM 解析本类会抛 {@code NoClassDefFoundError}。
 * 所有调用点必须先过 {@link #isAvailable()}。
 */
public final class WireSignalBridge {

    /** 通道名，顺序固定：前进 后退 左转 右转 */
    public static final String[] CHANNELS = {
            "\u524d\u8fdb", "\u540e\u9000", "\u5de6\u8f6c", "\u53f3\u8f6c"
    };

    private static int dbgWire = 0;
    private static int dbgProbe = 0;
    private static int dbgHint = 0;
    /** 按终端坐标分桶：多台终端共用一个静态字段会互相覆盖，导致每次都判定为「变了」（1.1.4 踩到了） */
    private static final Map<Long, String> LAST_SIG = new java.util.HashMap<>();

    private WireSignalBridge() {
    }

    /** 只查 mod 列表，不触碰 drivebywire 类型，可安全调用 */
    public static boolean isAvailable() {
        return ModList.get().isLoaded("drivebywire");
    }

    /**
     * 推送四个操纵通道的当前值。
     *
     * 只能在 {@link #isAvailable()} 为 true 时调用。
     */
    public static void pushAll(NavigationTerminalBlockEntity terminal, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(NavigationTerminalBlock.FACING)) {
            return;
        }
        // 用校准后的机头方向（SubLevel 上 FACING 是世界坐标里错的）
        Direction facing = terminal.effectiveFacing();

        // 前进/后退：按方位投影给推力
        int fwd = terminal.controlPower(0, facing);
        int back = terminal.controlPower(1, facing);

        // 左转/右转：用 20 Hz 缓存值（转向控制在 serverTick 每 tick 跑，见 getCachedSteer）
        int steer = terminal.getCachedSteer();
        int left = steer < 0 ? -steer : 0;
        int right = steer > 0 ? steer : 0;

        int[] values = {fwd, back, left, right};

        // 一个 manager 实例算到底：避免「取管理器 / 写信号」走到不同实例上
        WireNetworkManager manager = WireNetworkManager.get(level);

        boolean anySink = false;
        boolean anyValue = false;
        StringBuilder sinkSig = new StringBuilder();
        for (int i = 0; i < CHANNELS.length; i++) {
            boolean hasSink = manager.hasSinksOnChannel(pos, CHANNELS[i]);
            anySink |= hasSink;
            anyValue |= values[i] != 0;
            sinkSig.append(hasSink ? '1' : '0');
            // 0 视为「清除」，正好符合「无信号」语义
            manager.setSource(level, pos, CHANNELS[i], values[i]);
        }

        // 「总线中转」保活（见 keepAliveRelay 的注释）：值不变时也要让第二跳重新采样一次
        if (anySink && anyValue) {
            keepAliveRelay(manager, level, pos);
        }

        // 只在「有变化」时记录：老写法按次数限制，会在玩家还没设目标时就把预算烧光，
        // 真正开始输出之后反而一行都看不到（1.1.2 踩到了）。
        // 签名按终端分桶（1.1.5 起）：多台终端共用静态字段会互相覆盖导致每次都判定为变化。
        String sig = values[0] + "," + values[1] + "," + values[2] + "," + values[3] + "|" + sinkSig;
        boolean changed = !sig.equals(LAST_SIG.get(pos.asLong()));
        if (changed) {
            if (LAST_SIG.size() > 32) {
                LAST_SIG.clear();
            }
            LAST_SIG.put(pos.asLong(), sig);
        }

        if (changed && dbgWire < 40) {
            dbgWire++;
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Wire] [{}] pos={} level={} values=[{}] anySink={} ch0Sink={} ch2Sink={} ch3Sink={}",
                    terminal.getTerminalName(),
                    pos.toShortString(), level.getClass().getSimpleName(),
                    values[0] + "," + values[1] + "," + values[2] + "," + values[3],
                    anySink,
                    manager.hasSinksOnChannel(pos, CHANNELS[0]),
                    manager.hasSinksOnChannel(pos, CHANNELS[2]),
                    manager.hasSinksOnChannel(pos, CHANNELS[3]));
        }

        // 端点清单：端点集合变化时打一次（含邻域方块 id），任何「连上了却不生效」的答案都在这行里
        if (changed && dbgProbe < 6) {
            dbgProbe++;            Map<String, Set<WireNetworkNode.WireNetworkSink>> perChannel =
                    manager.getNetwork().get(pos.asLong());
            StringBuilder sb = new StringBuilder();
            for (String channel : CHANNELS) {
                Set<WireNetworkNode.WireNetworkSink> sinks =
                        perChannel == null ? null : perChannel.get(channel);
                sb.append(channel).append('[').append(sinks == null ? 0 : sinks.size()).append("]{");
                if (sinks != null) {
                    for (WireNetworkNode.WireNetworkSink sink : sinks) {
                        BlockPos sinkPos = BlockPos.of(sink.position());
                        Direction sinkDir = Direction.from3DDataValue(sink.direction());
                        sb.append(sinkPos.toShortString()).append('/').append(sinkDir.getName())
                                .append('=').append(manager.getSignalAt(sinkPos, sinkDir))
                                .append(" -> ").append(describe(level, sinkPos))
                                .append(" 邻域{").append(neighborhood(level, sinkPos)).append("}; ");
                    }
                }
                sb.append("} ");
            }
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[WireProbe] [{}] src={} level={} client={} mgr={} anySink={} sinks={}",
                    terminal.getTerminalName(),
                    pos.toShortString(), level.getClass().getName(),
                    level.isClientSide(), System.identityHashCode(manager), anySink, sb);

            if (!anySink) {
                probeMisplacedSource(manager, level, pos);
            } else {
                probePlainSinkBlock(manager, level, pos);
            }
            // 「总线中转」接法（源→方块→方块→接收端）在 drivebywire 里是支持的：
            // 第二跳的源是那块总线方块、频道自动变成 world，由 WireCommonEvents.onNeighborNotify
            // 用 level.getBestNeighborSignal(总线) 接力（而我们推的值正好能被它读到，
            // 因为 MixinServerLevel 把 getBestNeighborSignal 的查询落回 node(总线, 面)）。
            // 但第二跳是另一个「源」，下面这段把整张网络打出来，才能看见它到底存不存在。
            dumpNetwork(manager, level, pos);
            dumpRelay(manager, level, pos);
        }

        // 端点如果是轮座，把「轮座真正会去查的那三个键」打出来，肉眼比对
        if (dbgHint < 3) {
            WheelMountHint hint = wheelMountHint(manager, level, pos);
            if (hint != null) {
                dbgHint++;
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                        "[WireHint] 轮座 {} facing={} 真正会被读的端点：左转=({}/{}) 右转=({}/{}) 刹车=({}/UP) "
                                + "| 轮座只做转向和刹车，前进/后退要另接读红石的方块",
                        hint.pos.toShortString(), hint.facing.getName(),
                        hint.pos.toShortString(), hint.left.getName(),
                        hint.pos.toShortString(), hint.right.getName(),
                        hint.pos.toShortString());
            }
        }
    }

    /** 方块 id + 方块实体类名，用来一眼看出缆线到底接在什么东西上 */
    private static String describe(Level level, BlockPos pos) {
        String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? id : id + "+" + be.getClass().getSimpleName();
    }

    /**
     * 把整张线缆网络打一遍（源 → 频道 → 端点），★ 标出本终端。
     *
     * <p>为什么需要：玩家可以用「源 → 总线方块 → 接收端」两跳接力，第二跳的源是那块总线方块，
     * 频道会被强制成 {@code world}。只看本终端名下的端点，永远看不到第二跳。
     * 限流：最多 12 个源、24 个端点。
     */
    private static void dumpNetwork(WireNetworkManager manager, Level level, BlockPos self) {
        Map<Long, Map<String, Set<WireNetworkNode.WireNetworkSink>>> net = manager.getNetwork();
        StringBuilder sb = new StringBuilder();
        int sources = 0;
        int sinks = 0;
        for (Map.Entry<Long, Map<String, Set<WireNetworkNode.WireNetworkSink>>> source
                : net.entrySet()) {
            if (sources >= 12 || sinks >= 24) {
                sb.append("…(截断) ");
                break;
            }
            sources++;
            BlockPos key = BlockPos.of(source.getKey());
            sb.append(key.equals(self) ? "\u2605" : "").append(key.toShortString())
                    .append('(').append(describe(level, key)).append("){");
            for (Map.Entry<String, Set<WireNetworkNode.WireNetworkSink>> channel
                    : source.getValue().entrySet()) {
                sb.append(channel.getKey()).append('[').append(channel.getValue().size()).append("]{");
                for (WireNetworkNode.WireNetworkSink sink : channel.getValue()) {
                    if (sinks >= 24) {
                        break;
                    }
                    sinks++;
                    BlockPos sp = BlockPos.of(sink.position());
                    sb.append(sp.toShortString()).append('/')
                            .append(Direction.from3DDataValue(sink.direction()).getName())
                            .append("->").append(describe(level, sp)).append(' ');
                }
                sb.append("} ");
            }
            sb.append("} ");
        }
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[WireNet] 全网 {} 个源，列出 {} 个：{}", net.size(), sources, sb);
    }

    /**
     * 中转体检：把「我们端点所在的那个方块」当成总线，看它下游端点实际拿到的值。
     *
     * <p>{@code best=} 那一项就是 {@code WireCommonEvents.onNeighborNotify} 采样时用的
     * {@code level.getBestNeighborSignal(总线)}——它非 0 就说明接力拿得到我们的值；
     * 后面 {@code 下游坐标/面=值} 是 {@code manager.getSignalAt} 读出来的落库值，
     * 非 0 就说明机器那边已经收到。
     */
    private static void dumpRelay(WireNetworkManager manager, Level level, BlockPos self) {
        Map<Long, Map<String, Set<WireNetworkNode.WireNetworkSink>>> net = manager.getNetwork();
        Map<String, Set<WireNetworkNode.WireNetworkSink>> mine = net.get(self.asLong());
        if (mine == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int printed = 0;
        for (Set<WireNetworkNode.WireNetworkSink> sinks : mine.values()) {
            for (WireNetworkNode.WireNetworkSink sink : sinks) {
                BlockPos bus = BlockPos.of(sink.position());
                sb.append(bus.toShortString())
                        .append("[best=").append(level.getBestNeighborSignal(bus)).append(']');
                Map<String, Set<WireNetworkNode.WireNetworkSink>> down = net.get(bus.asLong());
                if (down == null) {
                    sb.append("(该方块不是源，没有下游) ");
                    continue;
                }
                sb.append("=>");
                for (Map.Entry<String, Set<WireNetworkNode.WireNetworkSink>> entry : down.entrySet()) {
                    for (WireNetworkNode.WireNetworkSink d : entry.getValue()) {
                        if (printed >= 40) {
                            sb.append("…");
                            break;
                        }
                        printed++;
                        BlockPos dp = BlockPos.of(d.position());
                        Direction dd = Direction.from3DDataValue(d.direction());
                        sb.append(dp.toShortString()).append('/').append(dd.getName())
                                .append('=').append(manager.getSignalAt(dp, dd)).append(' ');
                    }
                }
                sb.append("| ");
            }
        }
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[WireRelay] {}", sb);
    }


    private static int manhattan(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    /**
     * 「总线中转」接法的保活（源 → 总线方块 → 接收端，两跳）。
     *
     * <p><b>为什么必须有</b>：第二跳的源是那块总线方块，频道被强制成 {@code world}，
     * 它的值由 {@code WireCommonEvents.onNeighborNotify} 在「邻居被通知」时用
     * {@code level.getBestNeighborSignal(总线)} 采样，而 {@code getBestNeighborSignal} 会经
     * {@code MixinServerLevel} 落回我们写的 {@code node(总线, 面)} —— 机制是通的。
     *
     * <p>但 {@code applySignalToSink} 只在<b>值发生变化</b>时才发邻居通知：
     * <pre>
     * if (!node.setInput(key, signal)) return;      // 值没变，直接返回
     * level.updateNeighborsAt(updatedPos, ...);     // 只有值变了才通知
     * </pre>
     * 于是「接线的那一刻终端还没设目标（推 0）」这种最常见的情况，第二跳会把 0 记死；
     * 之后就算「前进」稳定输出 6（值一直没变过），也永远不会再通知一次。
     *
     * <p>这里按我们的每个端点补一次与 {@code applySignalToSink} 完全相同的通知，
     * 让第二跳每 8 tick 都能重新采样一次。值没变时对端 {@code setInput} 返回 false，
     * 递归到此为止，不会形成回路。
     */
    private static void keepAliveRelay(WireNetworkManager manager, Level level, BlockPos self) {
        Map<String, Set<WireNetworkNode.WireNetworkSink>> perChannel =
                manager.getNetwork().get(self.asLong());
        if (perChannel == null) {
            return;
        }
        for (Set<WireNetworkNode.WireNetworkSink> sinks : perChannel.values()) {
            for (WireNetworkNode.WireNetworkSink sink : sinks) {
                BlockPos sinkPos = BlockPos.of(sink.position());
                Direction sinkDir = Direction.from3DDataValue(sink.direction());
                BlockPos updatedPos = sinkPos.relative(sinkDir);
                level.updateNeighborsAt(updatedPos, level.getBlockState(updatedPos).getBlock());
            }
        }
    }

    /**
     * 端点周围 3×3×3 里非空气的方块（只打偏移+方块id+方块实体类名）。
     *
     * <p>用途：缆线端点贴在一块普通方块上时，机器到底在哪、离多远，一眼就能看出来。
     * 只保留相对偏移，避免日志里出现一长串绝对坐标。
     */
    private static String neighborhood(Level level, BlockPos center) {
        StringBuilder sb = new StringBuilder();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState bs = level.getBlockState(p);
                    if (bs.isAir()) {
                        continue;
                    }
                    sb.append('(').append(dx).append(',').append(dy).append(',').append(dz).append(')')
                            .append(BuiltInRegistries.BLOCK.getKey(bs.getBlock()).getPath());
                    BlockEntity be = level.getBlockEntity(p);
                    if (be != null) {
                        sb.append('+').append(be.getClass().getSimpleName());
                    }
                    sb.append(' ');
                }
            }
        }
        return sb.length() == 0 ? "全空气" : sb.toString();
    }

    /**
     * 诊断 A：导航仪本体上一个接收端点都没有，但附近方块上挂着线缆源端点。
     *
     * <p>线缆的「第一下点击」决定源端点位置，也决定频道表：
     * 点在导航仪上 → 频道是 前进/后退/左转/右转；
     * 点在别的方块上 → {@code ClientWireNetworkHandler.changeChannel} 会把频道强制成 {@code world}。
     * 我们只往这四个中文频道推信号，所以源端点错了就永远送不到。
     */
    private static void probeMisplacedSource(WireNetworkManager manager, Level level, BlockPos self) {
        for (Map.Entry<Long, Map<String, Set<WireNetworkNode.WireNetworkSink>>> entry
                : manager.getNetwork().entrySet()) {
            BlockPos key = BlockPos.of(entry.getKey());
            if (key.equals(self) || manhattan(key, self) > 2 || entry.getValue().isEmpty()) {
                continue;
            }
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.warn(
                    "[WireFix] 导航仪 {} 上一个接收端点都没有，但旁边的 {} 上挂着线缆源端点"
                            + "（频道={}，方块={}）。"
                            + "线缆第一下必须点在【导航仪方块本体】上：点在别的方块上频道会被强制成 "
                            + "\"world\"，前进/后退/左转/右转 四个通道永远送不出去。",
                    self.toShortString(), key.toShortString(), entry.getValue().keySet(),
                    describe(level, key));
        }
    }

    /**
     * 诊断 B：接收端点点在了一块普通方块上（该方块自己没有方块实体），机器在旁边。
     *
     * <p>信号是按「接收方自己的坐标 + 朝向线缆那一面的方向」入表的
     * （{@code MixinServerLevel} 里 {@code target = pos.relative(direction.getOpposite())}），
     * 所以端点要贴在机器本体上；贴在旁边方块上，机器那一侧的查询键对不上，什么也读不到。
     */
    private static void probePlainSinkBlock(WireNetworkManager manager, Level level, BlockPos self) {
        Map<String, Set<WireNetworkNode.WireNetworkSink>> perChannel =
                manager.getNetwork().get(self.asLong());
        if (perChannel == null) {
            return;
        }
        for (Map.Entry<String, Set<WireNetworkNode.WireNetworkSink>> channelEntry : perChannel.entrySet()) {
            for (WireNetworkNode.WireNetworkSink sink : channelEntry.getValue()) {
                BlockPos sinkPos = BlockPos.of(sink.position());
                if (level.getBlockEntity(sinkPos) != null) {
                    continue;
                }
                // 这块普通方块自己是不是「总线」？如果它以 world 频道往下游分发，
                // 那端点贴普通方块就是**设计**（接力中转），不是接错线。1.1.6 之前会把这种误报成错误。
                if (manager.hasSinksOnChannel(sinkPos, "world")) {
                    continue;
                }
                for (Direction d : Direction.values()) {
                    BlockPos near = sinkPos.relative(d);
                    if (level.getBlockEntity(near) == null) {
                        continue;
                    }
                    com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.warn(
                            "[WireFix] 频道 {} 的端点 {} 是一块普通方块（{}），"
                                    + "它自己也没有下游线缆，不是总线；"
                                    + "旁边 {}（{}）才是机器。端点应点在【机器本体】的面上。",
                            channelEntry.getKey(), sinkPos.toShortString(), describe(level, sinkPos),
                            near.toShortString(), describe(level, near));
                    break;
                }
            }
        }
    }

    private record WheelMountHint(BlockPos pos, Direction facing, Direction left, Direction right) {
    }

    /**
     * 在已注册端点里找轮座，算出它真正会查询的三个 (pos, face) 键。
     * 找不到返回 null。类名匹配用字符串，避免硬引用 offroad 的类。
     */
    private static WheelMountHint wheelMountHint(WireNetworkManager manager, Level level, BlockPos source) {
        Map<String, Set<WireNetworkNode.WireNetworkSink>> perChannel =
                manager.getNetwork().get(source.asLong());
        if (perChannel == null) {
            return null;
        }
        for (Set<WireNetworkNode.WireNetworkSink> sinks : perChannel.values()) {
            for (WireNetworkNode.WireNetworkSink sink : sinks) {
                BlockPos sinkPos = BlockPos.of(sink.position());
                BlockEntity be = level.getBlockEntity(sinkPos);
                if (be == null || !be.getClass().getSimpleName().contains("WheelMount")) {
                    continue;
                }
                BlockState sinkState = level.getBlockState(sinkPos);
                Direction facing = sinkState.hasProperty(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                        ? sinkState.getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                        : Direction.NORTH;
                // WheelMountBlockEntity: d1 = facing.getClockWise()（左）, d2 = getCounterClockWise()（右）
                return new WheelMountHint(sinkPos, facing, facing.getClockWise(), facing.getCounterClockWise());
            }
        }
        return null;
    }
}
