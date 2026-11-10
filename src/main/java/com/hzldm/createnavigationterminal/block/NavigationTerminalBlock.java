package com.hzldm.createnavigationterminal.block;

import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 导航终端方块
 *
 * 职责：
 *  - 右键打开导航目标选择界面（列表来自客户端本地 Xaero 地图标记点）
 *  - 按「本方块到目标的距离」输出 0-15 可调红石信号
 *  - 进入目标半径后按 arriveAction 停止导航（静默断电 / 自动清目标 / 保持输出）
 *
 * 为什么不把信号强度放进 BlockState：
 *  - 0-15 与 FACING 组合是 64 个 blockstate 变体，且每次变化都要 setBlock
 *  - 强度是派生值，缓存在 BlockEntity，Block#getSignal 直接实时计算，
 *    变化时由 BE 调用 updateNeighborsAt 触发邻居刷新（同原版「比较器读容器」思路）
 */
public class NavigationTerminalBlock extends Block
        implements EntityBlock, edn.stratodonut.drivebywire.wire.MultiChannelWireSource {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public NavigationTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(HALF);
    }

    // ------------------------------------------------------------------
    // 红石输出
    // ------------------------------------------------------------------

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * 线控（Drive-By-Wire）四通道：相对终端的操纵量。
     *
     * <p>注意语义：线缆节点对同一 (方块, 面) 的多个来源取 <b>max</b>
     * （{@code WireNetworkNode#getSignal}），所以把多条通道并到同一个面上
     * 拿到的是「较大的那个」，既不相加也不相减。
     * 双流传动是手动操控那条路的事，自动这边不做数值混合。
     */
    private static final java.util.List<String> WIRE_CHANNELS = java.util.List.of(
            "\u524d\u8fdb", "\u540e\u9000", "\u5de6\u8f6c", "\u53f3\u8f6c");

    @Override
    public java.util.List<String> wire$getChannels() {
        return WIRE_CHANNELS;
    }

    @Override
    public String wire$nextChannel(final String current, final boolean forward) {
        final int idx = WIRE_CHANNELS.indexOf(current);
        if (idx == -1) {
            return WIRE_CHANNELS.getFirst();
        }
        return WIRE_CHANNELS.get(Math.floorMod(idx + (forward ? 1 : -1), WIRE_CHANNELS.size()));
    }

    /**
     * 照搬航空学导航台的输出规则：
     * 沿 FACING 轴的两个面（正前/正后）不输出，垂直于它的面才输出。
     */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        com.hzldm.createnavigationterminal.client.ClientPerfProbe.GET_SIGNAL.incrementAndGet();
        if (direction.getAxis() == state.getValue(FACING).getAxis()) {
            return 0;
        }
        return redstonePowerAt(level, pos, direction);
    }

    /**
     * 强信号只从底面给（朝向水平时），其余方向只提供弱信号。
     * 这样红石线能读到、但不会无意中强充能相邻方块。
     */
    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        com.hzldm.createnavigationterminal.client.ClientPerfProbe.GET_DIRECT_SIGNAL.incrementAndGet();
        if (direction.getAxis() == state.getValue(FACING).getAxis()) {
            return 0;
        }
        if (state.getValue(FACING).getAxis().isHorizontal() && direction == Direction.DOWN) {
            return this.getSignal(state, level, pos, direction);
        }
        return 0;
    }

    /** 不传导弱充能（同导航台） */
    @Override
    public boolean shouldCheckWeakPower(BlockState state,
                                       net.minecraft.world.level.SignalGetter level,
                                       BlockPos pos, Direction side) {
        com.hzldm.createnavigationterminal.client.ClientPerfProbe.WEAK_POWER.incrementAndGet();
        return false;
    }

    /** 红石线只能连到垂直于 FACING 的四个面（同导航台） */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos,
                                      @org.jetbrains.annotations.Nullable Direction direction) {
        if (direction == null) {
            return false;
        }
        return direction.getAxis() != state.getValue(FACING).getAxis();
    }

    /**
     * 实时计算强度，不读 BE 缓存字段。
     *
     * 原因：客户端渲染红石线时也会调用 getSignal，而客户端不执行 serverTick，
     * 缓存字段恒为 0 —— 直接读缓存会出现「服务端有信号、客户端红石线不亮」。
     * 缓存字段只用于判断是否需要 updateNeighborsAt。
     */
    private static int redstonePowerAt(BlockGetter level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof NavigationTerminalBlockEntity terminal) {
            return terminal.computePowerByFace(direction);
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // 放置 / 旋转 / 碰撞箱（楼梯式：朝向 + 上下半）
    // ------------------------------------------------------------------

    /** 与楼梯一致的上下半：贴地 = BOTTOM，贴天花板 = TOP（用另一套倒置模型） */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<
            net.minecraft.world.level.block.state.properties.Half> HALF =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF;

    /**
     * 模型包围盒（单位：格，已按 36 个元素算过）：
     * 正放 x[3.0,13.5] y[0,7.851] z[0,11.157]；反放是它绕 y=8 的镜像。
     */
    private static final float BOX_X0 = 3.0F / 16.0F;
    private static final float BOX_X1 = 13.5F / 16.0F;
    private static final float BOX_Y1 = 7.851F / 16.0F;   // BOTTOM 的上沿
    private static final float BOX_Z0 = 0.0F;
    private static final float BOX_Z1 = 11.157F / 16.0F;

    /** [half][facing.get2DDataValue()] → 形状 */
    private static final net.minecraft.world.phys.shapes.VoxelShape[][] SHAPES = buildShapes();

    private static net.minecraft.world.phys.shapes.VoxelShape[][] buildShapes() {
        var table = new net.minecraft.world.phys.shapes.VoxelShape[2][4];
        for (int half = 0; half < 2; half++) {
            boolean top = half == 1;
            float y0 = top ? 1.0F - BOX_Y1 : 0.0F;
            float y1 = top ? 1.0F : BOX_Y1;
            for (int f = 0; f < 4; f++) {
                // 以 facing=north 的盒子为基准，按 blockstate 的 y 旋转（0/90/180/270）
                float x0 = BOX_X0;
                float x1 = BOX_X1;
                float z0 = BOX_Z0;
                float z1 = BOX_Z1;
                for (int r = 0; r < f; r++) {
                    // 绕方块中心顺时针 90°：(x,z) → (z, 1-x)
                    float nx0 = z0;
                    float nx1 = z1;
                    float nz0 = 1.0F - x1;
                    float nz1 = 1.0F - x0;
                    x0 = nx0;
                    x1 = nx1;
                    z0 = nz0;
                    z1 = nz1;
                }
                table[half][f] = net.minecraft.world.phys.shapes.Shapes.box(
                        Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1),
                        Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1));
            }
        }
        return table;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        // 楼梯式判定：点顶面 → 下半；点底面 → 上半；点侧面看准星高度
        boolean top = clicked != Direction.DOWN
                && (clicked == Direction.UP
                || context.getClickLocation().y - (double) pos.getY() > 0.5D);
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, top
                        ? net.minecraft.world.level.block.state.properties.Half.TOP
                        : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        com.hzldm.createnavigationterminal.client.ClientPerfProbe.GET_SHAPE.incrementAndGet();
        int half = state.getValue(HALF) == net.minecraft.world.level.block.state.properties.Half.TOP ? 1 : 0;
        return SHAPES[half][state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    // ------------------------------------------------------------------
    // BlockEntity
    // ------------------------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NavigationTerminalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof NavigationTerminalBlockEntity terminal) {
                terminal.serverTick();
            }
        };
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    /** c:tools/wrench 通用扳手标签 */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> WRENCH_TAG =
            net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    /** 是否手持扳手（兼容 Create 扳手等只注册自身 id 的情况） */
    private static boolean isWrench(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            if (stack.is(WRENCH_TAG)) {
                return true;
            }
        } catch (Throwable ignored) {
            // 标签缺失时继续用 id 判断
        }
        var key = stack.getItem().builtInRegistryHolder().key().location();
        return key.getPath().contains("wrench");
    }

    /**
     * 扳手右键：把玩家当前所坐的载具绑定到这台终端。
     *
     * 与火控的扳手绑定一致：手持扳手对着目标方块右键即可，无需开界面。
     */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        // 先记日志：确认右键是否进到这里、以及扳手识别结果
        if (!level.isClientSide()) {
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Nav] 右键终端：item={} isWrench={}",
                    stack.getItem().builtInRegistryHolder().key().location(),
                    isWrench(stack));
        }

        // 命名牌右键 = 给终端命名（编队时显示的名字）
        if (stack.is(net.minecraft.world.item.Items.NAME_TAG)) {
            if (level.isClientSide()) {
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof NavigationTerminalBlockEntity term
                    && player instanceof ServerPlayer sp) {
                var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                if (custom != null && !custom.getString().isBlank()) {
                    term.setTerminalName(custom.getString());
                    sp.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§a终端已命名为：§f" + custom.getString()), true);
                    if (!sp.isCreative()) {
                        stack.shrink(1);
                    }
                    com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                            "[Nav] 终端命名：{} -> {}", custom.getString(), pos);
                } else {
                    sp.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§7请先用铁砧给命名牌起名字"), true);
                }
            }
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }

        if (!isWrench(stack)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof NavigationTerminalBlockEntity terminal)
                || !(player instanceof ServerPlayer sp)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 潜行 + 扳手右键某个面 = 选中该面，之后滚轮切换它的输出通道
        if (sp.isShiftKeyDown()) {
            net.minecraft.core.Direction face = hit.getDirection();
            terminal.setEditingFace(face.ordinal());
            String faceCn = switch (face) {
                case DOWN -> "\u4e0b";
                case UP -> "\u4e0a";
                case NORTH -> "\u5317";
                case SOUTH -> "\u5357";
                case WEST -> "\u897f";
                case EAST -> "\u4e1c";
            };
            String srcCn = NavigationTerminalBlockEntity.sourceName(
                    terminal.getFaceSource(face.ordinal()));
            sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§e\u5df2\u9009\u4e2d" + faceCn + "\u9762 §7\u2192 \u6eda\u8f6e\u5207\u6362\u901a\u9053"
                                    + "\uff08\u5f53\u524d\uff1a§f" + srcCn + "§7\uff09"),
                    true);
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }

        // 第一步：记下这台终端，等玩家用扳手右键座椅
        com.hzldm.createnavigationterminal.server.BindingTracker.setPending(sp, pos);
        sp.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§b已选中终端 §7" + pos.toShortString() + " §b→ 再用扳手右键要绑定的座椅"),
                true);
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Nav] 扳手选中终端 {}，等待右键座椅（玩家 {}）", pos, sp.getName().getString());
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof NavigationTerminalBlockEntity terminal
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(terminal, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }
}
