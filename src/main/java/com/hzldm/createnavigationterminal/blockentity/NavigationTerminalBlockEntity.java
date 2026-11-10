package com.hzldm.createnavigationterminal.blockentity;

import com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu;
import com.hzldm.createnavigationterminal.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 导航终端 BlockEntity
 *
 * 保存：
 *  - 导航目标（名称 / 坐标 / 维度）
 *  - 五个输出参数（模式、强度上限、覆盖距离、到达半径、到达后行为）
 *  - arrived 到达锁存位
 *
 * 派生值：
 *  - redstonePower：当前强度缓存，仅用于判断是否需要 updateNeighborsAt；
 *    实际 getSignal 走 computePower() 实时计算（见 NavigationTerminalBlock）
 *
 * arrived 为什么必须锁存：
 *  - 方块位置固定，若不做锁存，目标半径正好压在边界上时信号会逐 tick 抽动
 *  - 复位只允许：重新选目标 / 清除目标 / 玩家点「重置到达」
 */
public class NavigationTerminalBlockEntity extends BlockEntity implements MenuProvider {

    // ------------------------------------------------------------------
    // 枚举与取值范围（服务端 clamp 与界面控件共用）
    // ------------------------------------------------------------------

    /** 输出模式：距离渐变（越近越强） */
    public static final int MODE_RAMP = 0;
    /** 输出模式：到达开关（圈内满信号，圈外 0） */
    public static final int MODE_SWITCH = 1;
    /** 输出模式：固定强度（有目标即恒输出上限） */
    public static final int MODE_FIXED = 2;

    /** 到达后：保持输出（到达给满信号） */
    public static final int ARRIVE_HOLD = 0;
    /** 到达后：静默停止（信号归 0 并锁存）—— 默认 */
    public static final int ARRIVE_SILENT = 1;
    /** 到达后：自动清除目标（单次导航） */
    public static final int ARRIVE_CLEAR = 2;

    public static final int MIN_POWER = 1;
    /** 强度上限：**14 不是 15**。15 在部分机构里是「常通/极限位」，会顶死舵机或让换向器不给调节 */
    public static final int MAX_POWER = 14;
    public static final int MIN_RANGE = 8;
    public static final int MAX_RANGE = 512;
    public static final int MIN_ARRIVE_RADIUS = 0;
    public static final int MAX_ARRIVE_RADIUS = 64;

    public static final int DEFAULT_MODE = MODE_RAMP;
    public static final int DEFAULT_POWER = 14;
    public static final int DEFAULT_RANGE = 256;
    public static final int DEFAULT_ARRIVE_RADIUS = 16;
    public static final int DEFAULT_ARRIVE_ACTION = ARRIVE_SILENT;

    /** 距离减速：进入该距离后按剩余距离线性收油门；0=关闭（保持旧行为） */
    public static final int MIN_DECEL = 0;
    public static final int MAX_DECEL = 512;
    public static final int DEFAULT_DECEL = 64;

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    private String targetName = "";
    private int targetX;
    private int targetY;
    private int targetZ;
    private String targetDimension = "";

    private int mode = DEFAULT_MODE;
    private int maxPower = DEFAULT_POWER;
    private int maxRange = DEFAULT_RANGE;
    private int arriveRadius = DEFAULT_ARRIVE_RADIUS;
    private int arriveAction = DEFAULT_ARRIVE_ACTION;
    private int decelDistance = DEFAULT_DECEL;

    /** 满舵角度（度）：偏角达到它给满舵 */
    private int steerScaleDeg = DEFAULT_STEER_SCALE;

    public int getSteerScaleDeg() {
        return steerScaleDeg;
    }

    public int getDecelDistance() {
        return decelDistance;
    }

    public void setDecelDistance(int v) {
        this.decelDistance = Mth.clamp(v, MIN_DECEL, MAX_DECEL);
        setChanged();
        syncNow();
    }

    private boolean arrived = false;

    /**
     * 绑定的座椅（载具实体 UUID 字符串，空串表示未绑定）。
     *
     * 只有玩家正坐在这个载具上时，服务端才会给他推送导航 HUD 数据，
     * 因此 HUD 严格「只对绑定的座椅生效」。
     */
    private String boundSeatUuid = "";

    /** 服务端算好的水平距离（-1=无目标/不同维度），随方块更新同步给客户端 */
    private int syncedDistance = -1;

    /**
     * 世界参考坐标（真实主世界坐标）。
     *
     * Sable 物理结构（SubLevel）内的方块，worldPosition 是虚拟坐标
     * （实测 20481034, 128, 20483095），拿它算距离会得到 2.9e7 这种天文数字。
     * 因此绑定时 / 玩家坐在绑定载具上时，用玩家真实坐标覆盖它。
     * NaN = 未设置，退回方块自身位置。
     */
    private double refX = Double.NaN;
    private double refY = Double.NaN;
    private double refZ = Double.NaN;

    /** 终端名（命名牌重命名后用于编队识别） */
    private String terminalName = "";

    /** 是否属于编队（编队内用主色，编队外用次色区分） */
    private boolean inFleet = false;

    /** 编队频道（0-15）：同频道才算同一编队 */
    private int fleetChannel = 0;

    /** 航线：多个导航目标，到达一个自动切下一个（循环） */
    private final java.util.List<com.hzldm.createnavigationterminal.item.WpTarget> route =
            new java.util.ArrayList<>();

    /**
     * 航线卡槽（1 格）。
     *
     * 放入空卡 → 把当前航线写进卡（导出）
     * 放入有数据的卡 → 把卡里的路点追加进航线（导入）
     */
    private final net.minecraft.world.SimpleContainer cardSlot =
            new net.minecraft.world.SimpleContainer(1) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    onCardSlotChanged();
                }

                @Override
                public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
                    return stack.getItem() instanceof
                            com.hzldm.createnavigationterminal.item.WaypointCardItem;
                }
            };

    public net.minecraft.world.Container getCardSlot() {
        return cardSlot;
    }

    /** 槽位变化：空卡导出，有数据的卡导入 */
    /** 防止 cardSlot.setChanged() 递归回调本方法。 */
    private boolean handlingCard = false;

    private void onCardSlotChanged() {
        if (handlingCard || level == null || level.isClientSide()) {
            return;
        }
        handlingCard = true;
        try {
            doOnCardSlotChanged();
        } finally {
            handlingCard = false;
        }
    }

    private void doOnCardSlotChanged() {
        net.minecraft.world.item.ItemStack stack = cardSlot.getItem(0);
        if (stack.isEmpty() || !(stack.getItem()
                instanceof com.hzldm.createnavigationterminal.item.WaypointCardItem)) {
            setChanged();
            return;
        }
        var data = stack.get(
                com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD);

        if (data == null || data.entries() == null || data.entries().isEmpty()) {
            // 空卡 → 导出当前航线
            if (route.isEmpty()) {
                return;
            }
            if (data.readOnly()) {
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                        "[Nav] 卡片只读，跳过写入：{}", data.name());
                setChanged();
                return;
            }
            stack.set(com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD,
                    data.withEntries(java.util.List.copyOf(route)));
            cardSlot.setItem(0, stack);
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Nav] 航线导出到卡：{} 站", route.size());
        } else {
            // 有数据的卡 → 导入（替换整条航线，不是追加）
            // 之前是 route.add(...) 追加，卡片每触发一次 setChanged 就再追加一遍，
            // 航线会从 2 站涨到 4、6、8…，表现为「几个点来回跑」
            route.clear();
            int added = 0;
            for (var wp : data.entries()) {
                if (route.size() >= 32) {
                    break;
                }
                route.add(wp);
                added++;
            }
            if (added > 0) {
                routeIndex = 0;
                applyRouteTarget(routeIndex);
                // 单站航线强制单程，否则到了就原地打转
                if (route.size() <= 1) {
                    routeLoop = false;
                }
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                        "[Nav] 从卡导入航线：{} 站", added);
            }
        }
        setChanged();
        syncNow();
    }

    /**
     * 修改槽里卡片的名字与只读标记（条目保持不变）。
     * 只读只限制「把当前航线写进卡」，改名与开关只读本身永远允许。
     *
     * @return true 表示设置成功
     */
    public boolean updateCardSettings(String name, boolean readOnly) {
        net.minecraft.world.item.ItemStack stack = cardSlot.getItem(0);
        if (stack.isEmpty() || !(stack.getItem()
                instanceof com.hzldm.createnavigationterminal.item.WaypointCardItem)) {
            return false;
        }
        var data = stack.get(
                com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD);
        var base = data == null
                ? com.hzldm.createnavigationterminal.item.WaypointCardData.EMPTY : data;
        String clean = name == null ? "" : name.trim();
        int max = com.hzldm.createnavigationterminal.item.WaypointCardData.MAX_NAME;
        if (clean.length() > max) {
            clean = clean.substring(0, max);
        }
        handlingCard = true;
        try {
            stack.set(com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD,
                    base.withName(clean).withReadOnly(readOnly));
            cardSlot.setItem(0, stack);
        } finally {
            handlingCard = false;
        }
        setChanged();
        syncNow();
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Nav] 卡片设置：name='{}' readOnly={}", clean, readOnly);
        return true;
    }

    /** 当前指向航线中的第几个 */
    private int routeIndex = 0;

    /**
     * 把当前航线写进槽里的路径点卡（导出）。
     *
     * @return true 表示导出成功
     */
    public boolean exportRouteToCard() {
        net.minecraft.world.item.ItemStack stack = cardSlot.getItem(0);
        if (stack.isEmpty() || !(stack.getItem()
                instanceof com.hzldm.createnavigationterminal.item.WaypointCardItem)) {
            return false;
        }
        if (route.isEmpty()) {
            return false;
        }
        // 只读卡：拒绝覆盖（名字与只读标记本身仍可改，见 updateCardSettings）
        var cur = stack.get(
                com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD);
        if (cur != null && cur.readOnly()) {
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Nav] 卡片只读，拒绝写入：{}", cur.name());
            return false;
        }
        int saved = route.size();
        // 关键：写卡会让 cardSlot.setChanged() 触发 onCardSlotChanged()，
        // 那条路径会 route.clear() 再导入，把刚写的航线冲掉（表现为只剩 1 站）。
        handlingCard = true;
        try {
            stack.set(com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD,
                    (cur == null
                            ? com.hzldm.createnavigationterminal.item.WaypointCardData.EMPTY
                            : cur).withEntries(java.util.List.copyOf(route)));
            cardSlot.setItem(0, stack);
        } finally {
            handlingCard = false;
        }
        setChanged();
        syncNow();
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Nav] 手动导出航线到卡：{} 站", saved);
        return true;
    }

    /**
     * 从槽里的导航点卡读取航线（替换当前航线）。
     *
     * @return true 表示读取成功
     */
    public boolean importRouteFromCard() {
        net.minecraft.world.item.ItemStack stack = cardSlot.getItem(0);
        if (stack.isEmpty() || !(stack.getItem()
                instanceof com.hzldm.createnavigationterminal.item.WaypointCardItem)) {
            return false;
        }
        var data = stack.get(
                com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD);
        if (data == null || data.entries() == null || data.entries().isEmpty()) {
            return false;
        }
        route.clear();
        for (var wp : data.entries()) {
            if (route.size() >= 32) {
                break;
            }
            route.add(wp);
        }
        if (route.isEmpty()) {
            return false;
        }
        routeIndex = 0;
        applyRouteTarget(routeIndex);
        arrived = false;
        if (route.size() <= 1) {
            routeLoop = false;
        }
        setChanged();
        syncNow();
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Nav] 手动读取航线：{} 站", route.size());
        return true;
    }

    /**
     * 把当前目标推送给同频道编队终端。
     *
     * @return 实际接收推送的终端数
     */
    public int pushTargetToFleet() {
        if (level == null || level.isClientSide() || !hasTarget() || isSoloChannel()) {
            return 0;
        }
        var peers = com.hzldm.createnavigationterminal.fleet.FleetRegistry.sameChannel(
                level.dimension(), fleetChannel, worldPosition, 0);
        int n = 0;
        for (var p : peers) {
            if (level.getBlockEntity(p) instanceof NavigationTerminalBlockEntity other) {
                other.targetName = this.targetName;
                other.targetX = this.targetX;
                other.targetY = this.targetY;
                other.targetZ = this.targetZ;
                other.targetDimension = this.targetDimension;
                other.arrived = false;
                other.setChanged();
                other.syncNow();
                n++;
            }
        }
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Nav] 推送目标到 CH{} {} 台终端：{}", fleetChannel, n, targetName);
        return n;
    }

    /**
     * 把整条航线推送给同频道编队终端（替换对方航线）。
     *
     * @return 实际接收推送的终端数
     */
    public int pushRouteToFleet() {
        if (level == null || level.isClientSide() || route.isEmpty() || isSoloChannel()) {
            return 0;
        }
        var peers = com.hzldm.createnavigationterminal.fleet.FleetRegistry.sameChannel(
                level.dimension(), fleetChannel, worldPosition, 0);
        int n = 0;
        for (var p : peers) {
            if (level.getBlockEntity(p) instanceof NavigationTerminalBlockEntity other) {
                other.route.clear();
                other.route.addAll(this.route);
                other.routeIndex = 0;
                other.routeLoop = this.routeLoop;
                other.arrived = false;
                other.applyRouteTarget(0);
                other.setChanged();
                other.syncNow();
                n++;
            }
        }
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Nav] 推送航线到 CH{} {} 台终端：{} 站", fleetChannel, n, route.size());
        return n;
    }

    /** 槽里是否有路径点卡 */
    public boolean hasCardInSlot() {
        return !cardSlot.getItem(0).isEmpty();
    }

    /** 航线末尾行为：true=循环回第一站，false=单次（到最后一站就停） */
    private boolean routeLoop = true;

    public boolean isRouteLoop() {
        return routeLoop;
    }

    public void toggleRouteLoop() {
        this.routeLoop = !routeLoop;
        setChanged();
        syncNow();
    }

    public java.util.List<com.hzldm.createnavigationterminal.item.WpTarget> getRoute() {
        return java.util.List.copyOf(route);
    }

    /** 预设：3 个槽位，每槽 5 个参数（mode, power, range, radius, action） */
    private final int[][] presets = new int[3][5];
    private final boolean[] presetSet = new boolean[3];

    public boolean isPresetSet(int slot) {
        return slot >= 0 && slot < 3 && presetSet[slot];
    }

    /** 把当前参数存进指定预设槽 */
    public void savePreset(int slot) {
        if (slot < 0 || slot > 2) {
            return;
        }
        presets[slot][0] = mode;
        presets[slot][1] = maxPower;
        presets[slot][2] = maxRange;
        presets[slot][3] = arriveRadius;
        presets[slot][4] = arriveAction;
        presetSet[slot] = true;
        setChanged();
        syncNow();
    }

    /** 应用指定预设槽 */
    public void applyPreset(int slot) {
        if (slot < 0 || slot > 2 || !presetSet[slot]) {
            return;
        }
        setSettings(presets[slot][0], presets[slot][1], presets[slot][2],
                presets[slot][3], presets[slot][4]);
    }

    /** 跳到上一站（循环） */
    public void prevRouteTarget() {
        if (route.isEmpty()) {
            return;
        }
        routeIndex = (routeIndex - 1 + route.size()) % route.size();
        applyRouteTarget(routeIndex);
        this.arrived = false;
        setChanged();
        syncNow();
    }

    /**
     * 把航线中的 from 位置移到 to 位置（PS 图层式的拖拽重排）。
     *
     * 同时维护 routeIndex，保证「当前正在导航的那一站」跟着它自己的
     * 数据走，而不是停在原来的序号上。
     */
    public boolean moveRoute(int from, int to) {
        int n = route.size();
        if (n < 2 || from < 0 || from >= n || to < 0 || to >= n || from == to) {
            return false;
        }

        com.hzldm.createnavigationterminal.item.WpTarget moving = route.remove(from);
        route.add(to, moving);

        if (routeIndex == from) {
            routeIndex = to;
        } else if (from < routeIndex && to >= routeIndex) {
            routeIndex--;
        } else if (from > routeIndex && to <= routeIndex) {
            routeIndex++;
        }
        routeIndex = Math.max(0, Math.min(route.size() - 1, routeIndex));

        applyRouteTarget(routeIndex);
        this.arrived = false;
        setChanged();
        syncNow();
        return true;
    }

    /** 删除当前站；删空后清目标 */
    public void removeCurrentRoute() {        if (route.isEmpty()) {
            return;
        }
        route.remove(routeIndex);
        if (route.isEmpty()) {
            clearTarget();
        } else {
            routeIndex = routeIndex % route.size();
            applyRouteTarget(routeIndex);
        }
        this.arrived = false;
        setChanged();
        syncNow();
    }

    /** 按坐标从航线中移除一个点（右键菜单用） */
    public boolean removeRouteAt(int x, int y, int z) {
        for (int i = 0; i < route.size(); i++) {
            var t = route.get(i);
            if (t.x() == x && t.y() == y && t.z() == z) {
                route.remove(i);
                if (route.isEmpty()) {
                    clearTarget();
                } else {
                    routeIndex = Math.min(routeIndex, route.size() - 1);
                    applyRouteTarget(routeIndex);
                }
                this.arrived = false;
                setChanged();
                syncNow();
                return true;
            }
        }
        return false;
    }

    /** 某坐标是否已在航线里 */
    public boolean isInRoute(int x, int y, int z) {
        for (var t : route) {
            if (t.x() == x && t.y() == y && t.z() == z) {
                return true;
            }
        }
        return false;
    }

    public int getRouteIndex() {
        return routeIndex;
    }

    /** 把目标追加到航线；航线原来为空时直接生效 */
    public void addRouteTarget(com.hzldm.createnavigationterminal.item.WpTarget t) {
        route.add(t);
        if (route.size() == 1) {
            applyRouteTarget(0);
        }
        setChanged();
        syncNow();
    }

    /** 清空整条航线与当前目标 */
    public void clearRoute() {
        route.clear();
        routeIndex = 0;
        clearTarget();
        setChanged();
        syncNow();
    }

    /** 跳到航线下一个；单次模式到末尾则停在最后一站 */
    public void nextRouteTarget() {
        if (route.isEmpty()) {
            return;
        }
        if (!routeLoop && routeIndex + 1 >= route.size()) {
            // 单次：已到最后一站，停在原地并标记到达
            this.arrived = true;
            setChanged();
            syncNow();
            return;
        }
        routeIndex = (routeIndex + 1) % route.size();
        applyRouteTarget(routeIndex);
        this.arrived = false;
        setChanged();
        syncNow();
    }

    private void applyRouteTarget(int idx) {
        if (idx < 0 || idx >= route.size()) {
            return;
        }
        var t = route.get(idx);
        this.targetName = t.name();
        this.targetX = t.x();
        this.targetY = t.y();
        this.targetZ = t.z();
        this.targetDimension = t.dimension();
    }

    private void syncNow() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** 目标是否在别的维度（跨维度导航） */
    public boolean isCrossDimension() {
        return hasTarget() && level != null
                && !level.dimension().location().toString().equals(targetDimension);
    }
    private int redstonePower = 0;

    // ------------------------------------------------------------------
    // 构造
    // ------------------------------------------------------------------

    public NavigationTerminalBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.NAVIGATION_TERMINAL.get(), pos, state);
    }

    protected NavigationTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    public String getTargetName() { return targetName; }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }
    public int getTargetZ() { return targetZ; }
    public String getTargetDimension() { return targetDimension; }
    public boolean hasTarget() { return !targetName.isEmpty() && !targetDimension.isEmpty(); }

    public int getMode() { return mode; }
    public int getMaxPower() { return maxPower; }
    public int getMaxRange() { return maxRange; }
    public int getArriveRadius() { return arriveRadius; }
    public int getArriveAction() { return arriveAction; }
    public boolean isArrived() { return arrived; }
    public int getRedstonePower() { return redstonePower; }

    // ------------------------------------------------------------------
    // 写入（服务端调用，内部全部 clamp）
    // ------------------------------------------------------------------

    /** 设置导航目标；name 为空等价于清除 */
    public void setTarget(String name, int x, int y, int z, String dimension) {
        this.targetName = safeTrim(name, 64);
        this.targetDimension = safeTrim(dimension, 128);
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.arrived = false;
        alignLatched = false;   // 换目标后必须重新对准
        arm();
        onStateChanged();
    }

    // ------------------------------------------------------------------
    // 出击许可（arm）
    //
    // 为什么需要：目标、驾驶模式都写进 NBT。若加载存档后立刻按旧目标出力，
    // 没人看管的载具会当场被开走 —— 现场表现就是「每次进存档机头方向都不一样」，
    // 实测加载后第一帧就是 values=[0,7,0,8]（倒车 7 + 右转 8）。
    // 所以 arm 不持久化：读档后一律待命，玩家点一次「自动」或重设目标才出力。
    // ------------------------------------------------------------------

    private boolean armed = false;

    public boolean isArmed() {
        return armed;
    }

    /** 进入可出力状态；读档后不会自动为 true */
    public void arm() {
        this.armed = true;
    }

    /** 待命：立刻停止出力（下一次 serverTick 会把四个线缆通道清零） */
    public void disarm() {
        this.armed = false;
        smoothOutput = Double.NaN;
        smoothFacingDeg = Double.NaN;
        lastFacingDeg = Double.NaN;
        integralErr = 0.0;
        turnLatched = false;
        alignLatched = false;
    }

    public void clearTarget() {
        this.targetName = "";
        this.targetDimension = "";
        this.targetX = 0;
        this.targetY = 0;
        this.targetZ = 0;
        this.arrived = false;
        onStateChanged();
    }

    /** 复位「已到达」锁存 */
    public void resetArrived() {
        if (!arrived) {
            return;
        }
        this.arrived = false;
        onStateChanged();
    }

    /**
     * 修改输出参数。
     * 所有值在此夹紧，保证 computePower 的分母恒大于 0。
     */
    public void setSettings(int mode, int maxPower, int maxRange, int arriveRadius, int arriveAction,
                            int decelDistance, int steerScaleDeg) {
        this.mode = Mth.clamp(mode, MODE_RAMP, MODE_FIXED);
        this.maxPower = Mth.clamp(maxPower, MIN_POWER, MAX_POWER);
        this.arriveRadius = Mth.clamp(arriveRadius, MIN_ARRIVE_RADIUS, MAX_ARRIVE_RADIUS);
        int range = Mth.clamp(maxRange, MIN_RANGE, MAX_RANGE);
        if (range <= this.arriveRadius) {
            range = Math.min(MAX_RANGE, this.arriveRadius + 1);
        }
        this.maxRange = range;
        this.arriveAction = Mth.clamp(arriveAction, ARRIVE_HOLD, ARRIVE_CLEAR);
        this.decelDistance = Mth.clamp(decelDistance, MIN_DECEL, MAX_DECEL);
        this.steerScaleDeg = Mth.clamp(steerScaleDeg, MIN_STEER_SCALE, MAX_STEER_SCALE);
        onStateChanged();
    }

    /** 兼容旧调用（不带新参数时保持当前值） */
    public void setSettings(int mode, int maxPower, int maxRange, int arriveRadius, int arriveAction,
                            int decelDistance) {
        setSettings(mode, maxPower, maxRange, arriveRadius, arriveAction, decelDistance,
                this.steerScaleDeg);
    }

    /** 兼容旧调用（不带减速距离时保持当前值） */
    public void setSettings(int mode, int maxPower, int maxRange, int arriveRadius, int arriveAction) {
        setSettings(mode, maxPower, maxRange, arriveRadius, arriveAction, this.decelDistance);
    }

    /** 同步 + 立即重算（不等下一个 tick） */
    private void onStateChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            // 先维护到达锁存再算强度：否则选点瞬间若目标已在半径内，
            // 会先输出满信号、等 8 tick 才掉到 0，肉眼可见地闪一下
            updateArrived();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            refreshRedstone(true);
        }
    }

    // ------------------------------------------------------------------
    // 距离与强度
    // ------------------------------------------------------------------

    /**
     * 方块中心到目标的**水平**距离（只算 X/Z，忽略 Y）。
     *
     * 导航以平面距离为准：高度差不参与信号计算，只在界面作为参考显示。
     * 维度不同或无目标返回 -1。
     */
    public double distanceToTarget() {
        if (level == null || !hasTarget()) {
            return -1.0;
        }
        if (!level.dimension().location().toString().equals(targetDimension)) {
            return -1.0;
        }
        // 位置来源优先级（到达判定必须用**实时**位置）：
        //  1) 在 SubLevel 上 → 用姿态投影出的实时世界坐标；
        //  2) 不在 SubLevel 上 → 有座椅参考就用参考（等于方块自身位置），否则用方块中心。
        //
        // 旧实现无条件优先 refX/refZ，而参考坐标只在玩家坐进绑定载具时更新。
        // 玩家一起身参考就冻住，距离从此不再变化 → 永远判不到「到达」，
        // 表现就是「到了目标点，但到达后自动清除目标不生效」。
        double px;
        double pz;
        boolean onVehicle = false;
        if (com.hzldm.createnavigationterminal.integration.sable.SableTransform.isAvailable()) {
            onVehicle = com.hzldm.createnavigationterminal.integration.sable.SableTransform.isInSubLevel(this);
        }
        if (onVehicle) {
            double[] self = this.worldCenterXZ();
            px = self[0];
            pz = self[1];
        } else if (!Double.isNaN(refX) && !Double.isNaN(refZ)) {
            px = refX;
            pz = refZ;
        } else {
            double[] self = this.worldCenterXZ();
            px = self[0];
            pz = self[1];
        }
        double dx = px - targetX;
        double dz = pz - targetZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 目标相对参考点的高度差（仅参考，不参与距离与信号计算） */
    public int getHeightDelta() {
        if (!hasTarget()) {
            return 0;
        }
        int py = Double.isNaN(refY) ? worldPosition.getY() : (int) Math.floor(refY);
        return targetY - py;
    }

    /** 更新世界参考坐标（绑定时 / 玩家坐进绑定载具时调用） */
    public void setWorldRef(double x, double y, double z) {
        if (!Double.isNaN(refX)
                && Math.abs(x - refX) < 0.25
                && Math.abs(y - refY) < 0.25
                && Math.abs(z - refZ) < 0.25) {
            return;
        }
        this.refX = x;
        this.refY = y;
        this.refZ = z;
        setChanged();
    }

    /** 是否已有世界参考坐标 */
    public boolean hasWorldRef() {
        return !Double.isNaN(refX);
    }

    /** 世界参考坐标（未绑定时返回方块自身坐标） */
    public double refXOrPos() {
        return Double.isNaN(refX) ? worldPosition.getX() + 0.5 : refX;
    }

    public double refYOrPos() {
        return Double.isNaN(refY) ? worldPosition.getY() + 0.5 : refY;
    }

    public double refZOrPos() {
        return Double.isNaN(refZ) ? worldPosition.getZ() + 0.5 : refZ;
    }

    /** 终端名（可用命名牌重命名，用于编队识别） */
    public String getTerminalName() {
        return terminalName == null || terminalName.isEmpty()
                ? "导航终端" : terminalName;
    }

    public void setTerminalName(String name) {
        this.terminalName = name == null ? "" : name;
        setChanged();
        syncNow();
    }

    /** 是否在编队中 */
    public boolean isInFleet() {
        return inFleet;
    }

    /** 切换编队归属 */
    public boolean toggleFleet() {
        inFleet = !inFleet;
        setChanged();
        syncNow();
        return inFleet;
    }

    /** 当前编队频道 */
    public int getFleetChannel() {
        return fleetChannel;
    }

    /** 频道 +1（0-15 循环） */
    public int cycleFleetChannel() {
        fleetChannel = (fleetChannel + 1) % 16;
        inFleet = true;
        setChanged();
        syncNow();
        return fleetChannel;
    }

    /** 独占频道：不参与任何编队，收不到也推不出 */
    public static final int FLEET_CHANNEL_SOLO = -1;

    public boolean isSoloChannel() {
        return this.fleetChannel == FLEET_CHANNEL_SOLO;
    }

    /** 在「独占」与「CH0」之间切换 */
    public void toggleSoloChannel() {
        setFleetChannel(isSoloChannel() ? 0 : FLEET_CHANNEL_SOLO);
    }

    /** 直接设置频道（0-15，-1=独占），自动入队 */
    public void setFleetChannel(int ch) {
        if (ch < 0) {
            this.fleetChannel = FLEET_CHANNEL_SOLO;
        } else {
            this.fleetChannel = Math.min(15, ch);
        }
        this.inFleet = true;
        setChanged();
        syncNow();
    }

    // ------------------------------------------------------------------
    // 座椅绑定
    // ------------------------------------------------------------------

    /** 当前绑定的载具 UUID（空串=未绑定） */
    public String getBoundSeatUuid() {
        return boundSeatUuid;
    }

    public void setBoundSeatUuid(String uuid) {
        this.boundSeatUuid = uuid == null ? "" : uuid;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** 已加载的终端集合：供服务端 tick 查找「绑定了座椅」的终端 */
    private static final java.util.Set<NavigationTerminalBlockEntity> ACTIVE =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private int dbgRedstone = 0;
    private int dbgRsFaces = 0;

    public static java.util.Collection<NavigationTerminalBlockEntity> active() {
        return ACTIVE;
    }

    /** 按当前参数与距离计算应输出的红石强度（0-15） */
    /** 供外部（线控桥）读取强度上限 */
    public int getMaxPowerForDisplay() {
        return this.maxPower;
    }

    // 客户端每 tick 的缓存：渲染、Jade 之类工具提示、红石线显示会在同一 tick 内
    // 反复问同一个值，而每次都要算距离 + Sable 姿态投影（有分配）。
    // 服务端保持实时（refreshRedstone / onStateChanged 依赖立即重算）。
    private long clientPowerTick = Long.MIN_VALUE;
    private int clientPowerSnap = 0;
    private final int[] clientFaceSnap = new int[6];

    public int computePower() {
        if (level != null && level.isClientSide()) {
            clientSnapTick();
            return clientPowerSnap;
        }
        return computePowerLive();
    }

    public int computePowerByFace(net.minecraft.core.Direction dir) {
        if (level != null && level.isClientSide()) {
            clientSnapTick();
            return dir == null ? clientPowerSnap : clientFaceSnap[dir.ordinal()];
        }
        return computePowerByFaceLive(dir);
    }

    private void clientSnapTick() {
        long now = level.getGameTime();
        if (now == clientPowerTick) {
            return;
        }
        clientPowerTick = now;
        clientPowerSnap = computePowerLive();
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            clientFaceSnap[d.ordinal()] = computePowerByFaceLive(d);
        }
    }

    private int computePowerLive() {
        if (!hasTarget()) {
            return 0;
        }
        // 跨维度：距离无意义，直接给满信号表示「目标在别的维度，需要换维度」
        if (isCrossDimension()) {
            return maxPower;
        }
        double dist = distanceToTarget();
        if (dist < 0) {
            return 0;
        }
        if (arrived && (arriveAction == ARRIVE_SILENT || arriveAction == ARRIVE_CLEAR)) {
            return 0;
        }
        switch (mode) {
            case MODE_FIXED:
                return maxPower;
            case MODE_SWITCH:
                return dist <= arriveRadius ? maxPower : 0;
            case MODE_RAMP:
            default:
                if (dist >= maxRange) {
                    return 0;
                }
                if (dist <= arriveRadius) {
                    return maxPower;
                }
                double ratio = (maxRange - dist) / (double) (maxRange - arriveRadius);
                return Mth.clamp((int) Math.round(ratio * maxPower), MIN_POWER, maxPower);
        }
    }

    /**
     * 六个面各输出一路独立信号（线控多通道）。
     *
     * <pre>
     * 上   UP    —— 有目标即满信号（目标存在）
     * 下   DOWN  —— 已到达即满信号（到站脉冲）
     * 北   NORTH —— 距离衰减（原有主输出）
     * 南   SOUTH —— 目标在别的维度时满信号（换维度提示）
     * 东   EAST  —— 航线非空即满信号（航线就绪）
     * 西   WEST  —— 航线循环模式时满信号（模式指示）
     * </pre>
     *
     * 所有值都会在 {@link #refreshRedstone} 触发邻居更新时被重新拉取。
     */
    // ------------------------------------------------------------------
    // 多通道红石输出：每面绑定一个「信号源」，玩家可在设置面板里改
    // ------------------------------------------------------------------

    /** 信号源：距离衰减（默认） */
    public static final int SRC_RAMP = 0;
    /** 信号源：到站脉冲 */
    public static final int SRC_ARRIVED = 1;
    /** 信号源：跨维度指示 */
    public static final int SRC_CROSS_DIM = 2;
    /** 信号源：航线就绪 */
    public static final int SRC_ROUTE_READY = 3;
    /** 信号源：循环模式 */
    public static final int SRC_LOOP = 4;
    /** 信号源：有目标 */
    public static final int SRC_HAS_TARGET = 5;
    /** 信号源：未到站 */
    public static final int SRC_NOT_ARRIVED = 6;
    /** 信号源：航线进度（已走站数 / 总站数） */
    public static final int SRC_PROGRESS = 7;
    /** 信号源：目标方向（目标在哪个水平方向，就由哪个面输出满信号） */
    public static final int SRC_TARGET_DIR = 8;
    public static final int SRC_COUNT = 9;

    /** 6 个面的信号源索引，顺序 = Direction.ordinal()（DOWN,UP,NORTH,SOUTH,WEST,EAST） */
    private final int[] faceSource = {SRC_RAMP, SRC_HAS_TARGET, SRC_RAMP, SRC_CROSS_DIM,
                                      SRC_LOOP, SRC_ROUTE_READY};

    public int getFaceSource(int face) {
        return face >= 0 && face < 6 ? faceSource[face] : SRC_RAMP;
    }

    public void setFaceSource(int face, int src) {
        if (face >= 0 && face < 6) {
            faceSource[face] = Math.max(0, Math.min(SRC_COUNT - 1, src));
            setChanged();
            syncNow();
            refreshRedstone(true);
        }
    }

    public void cycleFaceSource(int face, int delta) {
        setFaceSource(face, (getFaceSource(face) + delta + SRC_COUNT) % SRC_COUNT);
    }

    public static String sourceName(int src) {
        return switch (src) {
            case SRC_RAMP -> "\u8ddd\u79bb\u8870\u51cf";
            case SRC_ARRIVED -> "\u5df2\u5230\u8fbe";
            case SRC_CROSS_DIM -> "\u8de8\u7ef4\u5ea6";
            case SRC_ROUTE_READY -> "\u822a\u7ebf\u5c31\u7eea";
            case SRC_LOOP -> "\u5faa\u73af\u6a21\u5f0f";
            case SRC_HAS_TARGET -> "\u6709\u76ee\u6807";
            case SRC_NOT_ARRIVED -> "\u672a\u5230\u8fbe";
            case SRC_PROGRESS -> "\u822a\u7ebf\u8fdb\u5ea6";
            case SRC_TARGET_DIR -> "\u76ee\u6807\u65b9\u5411";
            default -> "?";
        };
    }

    /** 正在用扳手选中的面（-1=未选中），供滚轮切换通道用 */
    private int editingFace = -1;
    /** 选中的时间戳，10 秒无操作自动取消 */
    private long editingFaceAt = 0;

    public int getEditingFace() {
        if (editingFace >= 0 && System.currentTimeMillis() - editingFaceAt > 10000L) {
            editingFace = -1;
        }
        return editingFace;
    }

    public void setEditingFace(int face) {
        this.editingFace = face;
        this.editingFaceAt = System.currentTimeMillis();
        setChanged();
        syncNow();
    }

    /** 滚轮切换当前选中面的通道 */
    public int scrollEditingFace(int delta) {
        int f = getEditingFace();
        if (f < 0) {
            return -1;
        }
        cycleFaceSource(f, delta);
        this.editingFaceAt = System.currentTimeMillis();
        return f;
    }

    public int computePowerForSource(int src) {
        return switch (src) {
            case SRC_RAMP -> computePower();
            case SRC_ARRIVED -> (arrived && hasTarget()) ? maxPower : 0;
            case SRC_CROSS_DIM -> isCrossDimension() ? maxPower : 0;
            case SRC_ROUTE_READY -> route.isEmpty() ? 0 : maxPower;
            case SRC_LOOP -> (routeLoop && route.size() > 1) ? maxPower : 0;
            case SRC_HAS_TARGET -> hasTarget() ? maxPower : 0;
            case SRC_NOT_ARRIVED -> (hasTarget() && !arrived) ? maxPower : 0;
            case SRC_PROGRESS -> {
                int n = route.size();
                if (n <= 1) {
                    yield n == 1 ? maxPower : 0;
                }
                yield Mth.clamp(Math.round(routeIndex * maxPower / (float) (n - 1)),
                        MIN_POWER, maxPower);
            }
            default -> 0;
        };
    }

    /**
     * 目标相对本终端的水平方位（北/南/东/西）。
     *
     * MC 坐标：+X 东，+Z 南。{@code atan2(dx, dz)} 得到 0=南、90=东、±180=北、-90=西。
     * 无目标或目标就在脚下时返回 null。
     */
    public net.minecraft.core.Direction directionToTarget() {
        if (!hasTarget()) {
            return null;
        }
        double dx = (double) targetX - (worldPosition.getX() + 0.5);
        double dz = (double) targetZ - (worldPosition.getZ() + 0.5);
        if (Math.abs(dx) < 1.0e-4 && Math.abs(dz) < 1.0e-4) {
            return null;
        }
        double deg = Math.toDegrees(Math.atan2(dx, dz));
        if (deg >= -45.0 && deg < 45.0) {
            return net.minecraft.core.Direction.SOUTH;
        }
        if (deg >= 45.0 && deg < 135.0) {
            return net.minecraft.core.Direction.EAST;
        }
        if (deg >= 135.0 || deg < -135.0) {
            return net.minecraft.core.Direction.NORTH;
        }
        return net.minecraft.core.Direction.WEST;
    }

    private int computePowerByFaceLive(net.minecraft.core.Direction dir) {
        if (dir == null) {
            return computePowerLive();
        }
        int src = faceSource[dir.ordinal()];
        if (src == SRC_TARGET_DIR) {
            return directionPower(dir);
        }
        return computePowerForSource(src);
    }

    /**
     * 按「目标方向」算某个面的输出（0~maxPower）。
     *
     * 用方向向量投影，强度和角度连续变化：
     * <pre>
     *   目标正东         → 东面满信号，其余 0
     *   目标偏东北 45°   → 东面 北面 各约 71%
     *   目标正北         → 北面满信号
     *   目标在背后       → 该面 0
     * </pre>
     * 上下两面不参与方位判断，固定为 0。
     */
    // ------------------------------------------------------------------
    // 驾驶模式与飞行策略
    // ------------------------------------------------------------------

    /** 自动：终端按目标方向驱动执行机构 */
    public static final int DRIVE_AUTO = 0;
    /** 手动：线控通道全部输出 0，玩家自己操作，终端只做导航显示 */
    public static final int DRIVE_MANUAL = 1;

    /** 先对准再前进：偏角超过阈值时不给推力 */
    public static final int STRATEGY_ALIGN_FIRST = 0;
    /** 边飞边修正：推力和转向同时给 */
    public static final int STRATEGY_FLY_AND_CORRECT = 1;

    /**
     * 机头朝向校准：-1 = 用方块 FACING；0..3 = 手动指定（北/东/南/西）。
     *
     * 放在 SubLevel（飞行器）上时，方块的 FACING 是载具局部坐标的朝向，
     * 和世界坐标里的实际前进方向不一致，会导致转向永远算错、一直打转。
     * 这时把校准设成载具实际的机头方向即可。
     */
    private int dbgSteer = 0;
    /** 转向滞回状态：true = 已对准（不再给转向，直到偏差重新变大） */
    private boolean turnLatched = false;
    /** 上一次转向计算的航向与时刻，用于算角速度 */
    private double lastFacingDeg = Double.NaN;
    private long lastSteerTick = 0L;
    /** 平滑后的航向与输出 */
    private double smoothFacingDeg = Double.NaN;
    private double smoothOutput = Double.NaN;

    private int headingOverride = -1;

    /**
     * 航向校准偏移（度，-180..180）。
     *
     * 自动驾驶算出的转向角需要加上这个偏移才能和实际机头对上，
     * 和 HUD 里的「方位校准」是同一件事：那边校准的是显示，这边校准的是控制。
     */
    private double headingOffsetDeg = 0.0;

    public double getHeadingOffsetDeg() {
        return headingOffsetDeg;
    }

    public void setHeadingOffsetDeg(double v) {
        // 归一化到 -180..180
        double x = v % 360.0;
        if (x > 180.0) {
            x -= 360.0;
        } else if (x < -180.0) {
            x += 360.0;
        }
        this.headingOffsetDeg = x;
        setChanged();
        syncNow();
    }

    public void cycleHeadingOffset(int delta) {
        setHeadingOffsetDeg(headingOffsetDeg + delta * 15.0);
    }

    public static String headingOffsetName(double v) {
        if (Math.abs(v) < 0.5) {
            return "0\u00b0";
        }
        return String.format("%+.0f\u00b0", v);
    }

    /** 转向反向：接反舵机时用 */
    private boolean steerInverted = false;

    public boolean isSteerInverted() {
        return steerInverted;
    }

    public void toggleSteerInverted() {
        this.steerInverted = !steerInverted;
        setChanged();
        syncNow();
    }

    public int getHeadingOverride() {
        return headingOverride;
    }

    public void setHeadingOverride(int v) {
        this.headingOverride = (v < 0 || v > 3) ? -1 : v;
        setChanged();
        syncNow();
    }

    public void cycleHeadingOverride(int delta) {
        int next = headingOverride + delta;
        if (next > 3) {
            next = -1;      // 3 -> 自动
        } else if (next < -1) {
            next = 3;       // 自动 -> 顶部
        }
        setHeadingOverride(next);
    }

    public static String headingName(int v) {
        return switch (v) {
            case 0 -> "\u5317";
            case 1 -> "\u4e1c";
            case 2 -> "\u5357";
            case 3 -> "\u897f";
            default -> "\u81ea\u52a8";
        };
    }

    /** 4 个水平朝向，用于 headingOverride 的粗档（0=北 1=东 2=南 3=西） */
    private static final net.minecraft.core.Direction[] HEADINGS = {
            net.minecraft.core.Direction.NORTH,
            net.minecraft.core.Direction.EAST,
            net.minecraft.core.Direction.SOUTH,
            net.minecraft.core.Direction.WEST
    };

    /** 实际用于计算的机头方向 */
    public net.minecraft.core.Direction effectiveFacing() {
        if (headingOverride >= 0 && headingOverride < HEADINGS.length) {
            return HEADINGS[headingOverride];
        }
        return blockFacing();
    }

    /** 方块自身的 FACING（不受 headingOverride 影响） */
    private net.minecraft.core.Direction blockFacing() {
        BlockState st = getBlockState();
        return st.hasProperty(com.hzldm.createnavigationterminal.block.NavigationTerminalBlock.FACING)
                ? st.getValue(com.hzldm.createnavigationterminal.block.NavigationTerminalBlock.FACING)
                : net.minecraft.core.Direction.NORTH;
    }

    /**
     * 机头在世界坐标里的方位角（度，MC 约定 {@code atan2(x, z)}：南=0 东=90 北=180 西=-90）。
     *
     * <p><b>统一口径</b>：转向（{@link #steeringRate}）和推力（{@link #controlPower}）必须用同一个
     * 「机头」定义，否则会出现「转向有输出、前进恒为 0」这种诡异现象——1.1.8 之前就是：
     * 转向用 SubLevel 姿态算航向，推力用方块面法线投影；终端 FACING 若与载具实际机头差 180°，
     * 推力那边点积恒为负，{@code asin} 变负 → 直接钳成 0。
     *
     * <p>不在 SubLevel 上时退化为方块 FACING 的方位角，与旧行为一致。
     */
    /** SubLevel 姿态给出的实时航向（度）；不在 SubLevel/无姿态时返回 NaN */
    private double poseHeadingDeg() {
        if (com.hzldm.createnavigationterminal.integration.sable.SableTransform.isAvailable()) {
            return com.hzldm.createnavigationterminal.integration.sable.SableTransform.headingDegrees(this);
        }
        return Double.NaN;
    }

    /** 方块 FACING 的方位角（度），与姿态无关、存档稳定 */
    private double facingBearingDeg() {
        net.minecraft.core.Direction f = blockFacing();
        return Math.toDegrees(Math.atan2(f.getStepX(), f.getStepZ()));
    }

    private double noseBearingDeg() {
        double deg = poseHeadingDeg();
        if (!Double.isFinite(deg)) {
            deg = facingBearingDeg();
        }
        if (headingOverride >= 0) {
            deg += headingOverride * 90.0;
        }
        return deg + headingOffsetDeg;
    }

    /** 每次进存档只打一次的机头基准体检，用来判断「每次加载机头都在变」到底变的是哪一项 */
    private static final java.util.Set<Long> NOSE_LOGGED = new java.util.HashSet<>();

    /** 每次进存档只打一次的「读档待命」提示（放 serverTick 里：loadAdditional 客户端同步也会跑） */
    private static final java.util.Set<Long> ARM_LOGGED = new java.util.HashSet<>();

    private void logDisarmedOnce() {
        if (level == null || level.isClientSide() || armed) {
            return;
        }
        if (driveMode != DRIVE_AUTO || !hasTarget() || !ARM_LOGGED.add(worldPosition.asLong())) {
            return;
        }
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Arm] [{}] 读档待命中：有旧目标「{}」但不出力。"
                        + "在界面里点一次「自动」或重新设目标即可出击。",
                terminalName == null || terminalName.isBlank() ? "未命名" : terminalName,
                targetName);
    }

    private void logNoseOnce() {
        if (level == null || level.isClientSide() || !NOSE_LOGGED.add(worldPosition.asLong())) {
            return;
        }
        double pose = poseHeadingDeg();
        double face = facingBearingDeg();
        String poseInfo = "n/a";
        if (com.hzldm.createnavigationterminal.integration.sable.SableTransform.isAvailable()) {
            try {
                String dbg = com.hzldm.createnavigationterminal.integration.sable.SableTransform.debugPose(this);
                if (dbg != null) {
                    poseInfo = dbg;
                }
            } catch (Throwable ignored) {
                // 姿态调试失败不影响主逻辑
            }
        }
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Nose] [{}] pos={} facing={} facingBearing={} poseHeading={} override={} offset={} nose={} 偏移={} {}",
                terminalName == null || terminalName.isBlank() ? "未命名" : terminalName,
                worldPosition.toShortString(), blockFacing(),
                String.format("%.1f", face),
                Double.isFinite(pose) ? String.format("%.1f", pose) : "NaN",
                headingOverride, String.format("%.1f", headingOffsetDeg),
                String.format("%.1f", noseBearingDeg()),
                Double.isFinite(pose) ? String.format("%.1f", Mth.wrapDegrees(pose - face)) : "NaN",
                poseInfo);
    }

    /** 驾驶锁：自动模式下禁止改目标，防止飞行中误触 */
    private boolean driveLock = true;

    public boolean isDriveLocked() {
        return driveLock;
    }

    public void setDriveLock(boolean locked) {
        this.driveLock = locked;
        setChanged();
        syncNow();
    }

    public void toggleDriveLock() {
        setDriveLock(!driveLock);
    }

    /**
     * 当前是否允许修改导航目标。
     *
     * 只有「自动驾驶 + 驾驶锁开启」时才拒绝，手动模式永远可改。
     */
    public boolean canEditTarget() {
        return !(driveLock && driveMode == DRIVE_AUTO);
    }

    /** 对准阈值（度）：偏角小于它才开始给前进推力 */
    public static final double ALIGN_THRESHOLD_DEG = 15.0;

    /** 推力锥：|偏角| ≤ 此值给满推力 */
    public static final double ALIGN_FULL_DEG = 60.0;
    /** 推力锥：|偏角| ≥ 此值完全不给推力（朝侧后方推没有意义） */
    public static final double ALIGN_ZERO_DEG = 90.0;

    /**
     * 「先对准」的推进锁存。
     *
     * <p>语义是两段式，不是每 tick 重新判定：
     * <pre>
     *   未推进：偏角 &gt; ALIGN_THRESHOLD_DEG → 只转向，不给推力
     *           偏角 ≤ ALIGN_THRESHOLD_DEG → 锁存「推进中」，开始前进
     *   推进中：靠转向修正，即使偏角变大也继续给推力
     *           只有偏角 ≥ ALIGN_ZERO_DEG（目标甩到侧后方）才解锁，退回只转向
     * </pre>
     * 旧实现是每 tick 用 15° 重新卡，一转向偏角就超阈值 → 推力立刻断，
     * 表现就是「对准了也不走 / 走两步又停」。
     */
    private boolean alignLatched = false;

    public boolean isAlignLatched() {
        return alignLatched;
    }
    /** 转向死区（度）：偏角小于它时不给转向 */
    public static final double TURN_DEADZONE_DEG = 4.0;
    /** 退出死区需要的角度（滞回）：13°→4°、24°→8°，原来那组等于留了 ±24° 的静默带 */
    public static final double TURN_RELEASE_DEG = 8.0;

    /** 「满舵角度」：偏角达到它就给满舵；越小越准、越大越稳 */
    public static final int MIN_STEER_SCALE = 10;
    public static final int MAX_STEER_SCALE = 90;
    public static final int DEFAULT_STEER_SCALE = 30;

    /** 角速度限幅（度/秒）：Sable 物理子级姿态抖动会把瞬时角速度顶到几十度/秒 */
    public static final double RATE_CLAMP_DEG_PER_SEC = 45.0;
    /** D 项最大贡献（归一化到 0..1 的控制量），防止抖动把舵一把打满 */
    public static final double D_TERM_CAP = 0.20;
    /** 转向增益：降低转向输出，减少过冲 */
    /** 比例系数（P）：偏差越大转向越强。1.0 = 满舵角度处给满舵 */
    private double kp = 1.0;
    /** 积分系数（I）：消除长期静态误差，默认关闭 */
    private double ki = 0.0;
    /** 微分系数（D）：抑制过冲，越大越稳但越慢 */
    private double kd = 0.80;

    public double getKp() { return kp; }
    public double getKi() { return ki; }
    public double getKd() { return kd; }

    private void logPid() {
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[PID] [{}] {} Kp={} Ki={} Kd={}",
                terminalName == null || terminalName.isBlank() ? "\u672a\u547d\u540d" : terminalName,
                worldPosition.toShortString(),
                String.format("%.2f", kp), String.format("%.2f", ki),
                String.format("%.2f", kd));
    }

    public void setKp(double v) { this.kp = Mth.clamp(v, 0.0, 3.0); setChanged(); syncNow(); logPid(); }
    public void setKi(double v) { this.ki = Mth.clamp(v, 0.0, 2.0); setChanged(); syncNow(); logPid(); }
    public void setKd(double v) { this.kd = Mth.clamp(v, 0.0, 3.0); setChanged(); syncNow(); logPid(); }

    public void cycleKp(int d) { setKp(kp + d * 0.10); }
    public void cycleKi(int d) { setKi(ki + d * 0.02); }
    public void cycleKd(int d) { setKd(kd + d * 0.1); }

    /** 积分累加器（度·秒） */
    private double integralErr = 0.0;
    /** 转向最小输出：低于它就几乎推不动舵机，看起来像「没转」 */
    public static final int TURN_MIN_OUTPUT = 5;
    /** 微分阻尼系数：越大越不容易过冲，但转向越慢 */
    public static final double DAMP_FACTOR = 0.8;
    /** 航向低通系数：越小越平滑但越迟钝 */
    public static final double HEADING_SMOOTH = 0.35;
    /** 输出低通系数：越小越平滑但响应越慢 */
    public static final double OUTPUT_SMOOTH = 0.35;

    private int driveMode = DRIVE_AUTO;
    private int flightStrategy = STRATEGY_FLY_AND_CORRECT;

    public int getDriveMode() {
        return driveMode;
    }

    public void setDriveMode(int mode) {
        this.driveMode = mode == DRIVE_MANUAL ? DRIVE_MANUAL : DRIVE_AUTO;
        if (driveMode == DRIVE_AUTO) {
            arm();
        } else {
            disarm();
        }
        setChanged();
        syncNow();
    }

    public void toggleDriveMode() {
        setDriveMode(driveMode == DRIVE_MANUAL ? DRIVE_AUTO : DRIVE_MANUAL);
    }

    public int getFlightStrategy() {
        return flightStrategy;
    }

    public void setFlightStrategy(int strategy) {
        this.flightStrategy = strategy == STRATEGY_ALIGN_FIRST
                ? STRATEGY_ALIGN_FIRST : STRATEGY_FLY_AND_CORRECT;
        alignLatched = false;   // 换策略后重新对准
        setChanged();
        syncNow();
    }

    public void toggleFlightStrategy() {
        setFlightStrategy(flightStrategy == STRATEGY_ALIGN_FIRST
                ? STRATEGY_FLY_AND_CORRECT : STRATEGY_ALIGN_FIRST);
    }

    public static String driveModeName(int mode) {
        return mode == DRIVE_MANUAL ? "\u624b\u52a8" : "\u81ea\u52a8";
    }

    public static String strategyName(int strategy) {
        return strategy == STRATEGY_ALIGN_FIRST
                ? "\u5148\u5bf9\u51c6" : "\u8fb9\u98de\u8fb9\u4fee";
    }

    /**
     * 转向角速度建议值（-15..15）。
     *
     * 正数=右转、负数=左转。这个值直接由方位角差算出，
     * 不走 asin 投影，所以对准时能平滑收敛到 0，不会在 0 附近来回摆。
     */
    public int steeringRate(net.minecraft.core.Direction facing) {
        if (!armed || driveMode == DRIVE_MANUAL || !hasTarget() || facing == null) {
            return 0;
        }
        // 目标方位必须用世界坐标算：
        // 终端在 SubLevel 上时 worldPosition 是 plot 坐标（因为 Sable 把飞行器的方块
        // 存在同一个 ServerLevel 的保留区块里），直接和主世界目标相减得到的是几千万格的假方向，
        // targetDeg 会变成一个和飞行器朝向无关的常数 —— 表现为「转向信号一直在给，但永远对不准」。
        // 这条规则见交接文档 5.2：方向/距离一律走 worldCenterXZ() / worldNormalXZ()。
        double[] self = this.worldCenterXZ();
        double dx = (double) targetX - self[0];
        double dz = (double) targetZ - self[1];
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) {
            return 0;
        }
        // 机头朝向的平面角：统一走 noseBearingDeg()（实时姿态 + 档位 + 精调）
        //
        // 注意：这里必须用「随飞行器变化的实时航向」，不能把 headingOverride 当固定值，
        // 那是静态档位（北/东/南/西），飞行器转起来后它不会变，diff 永远不归零 -> 原地打转。
        double facingDeg = this.noseBearingDeg();
        // 航向低通滤波：SubLevel 姿态每 tick 有微小抖动，
        // 不平滑的话 diff 跟着抖，输出就是一跳一跳的
        if (Double.isFinite(smoothFacingDeg)) {
            facingDeg = smoothFacingDeg + Mth.wrapDegrees(facingDeg - smoothFacingDeg) * HEADING_SMOOTH;
        }
        smoothFacingDeg = facingDeg;

        double targetDeg = Math.toDegrees(Math.atan2(dx, dz));
        // 归一化到 -180..180
        double diff = ((targetDeg - facingDeg + 540.0) % 360.0) - 180.0;
        double abs = Math.abs(diff);
        // 滞回：进入 2° 才停，回到 6° 以上才重新给力，避免边界抖动
        if (turnLatched) {
            if (abs < TURN_RELEASE_DEG) {
                return 0;
            }
            turnLatched = false;
        } else if (abs < TURN_DEADZONE_DEG) {
            turnLatched = true;
            integralErr = 0.0;   // 对准后清积分，防止残留导致下次过冲
            return 0;
        }
        // 标准 PID：
        //   P = kp * (偏差 / 90)，偏差归一化到 -1..1
        //   I = ki * 累计偏差（对准后清零，防饱和）
        //   D = -kd * 角速度（反向阻尼）
        long now = level != null ? level.getGameTime() : 0L;
        double dtTicks = Math.max(1.0, now - lastSteerTick);
        double dtSec = dtTicks / 20.0;

        // 归一化基准是「满舵角度」而不是固定的 90°：
        // 90° 归一化意味着偏角 20° 只给 0.22 的控制量 → 折算成信号 ≈ 3 → 掉到舵机启动阈值以下，
        // 于是误差 10~24° 这一段指令根本推不动舵，落点精度就只能停在 ±20° 上下。
        // 把基准做成可调（默认 30°）之后，同样的偏角能给出足够的信号，精度才提得上来。
        double normDeg = Math.max(10.0, steerScaleDeg);
        double errNorm = Math.max(-1.0, Math.min(1.0, diff / normDeg));

        // 积分：只在有偏差时累加，限幅防饱和
        integralErr += errNorm * dtSec;
        integralErr = Math.max(-3.0, Math.min(3.0, integralErr));

        // 角速度（度/秒）：物理子级（Sable）每 tick 的姿态抖动可以达到几度，
        // 换算成角速度会瞬间冲到几十度/秒，必须限幅，否则 D 项会放大噪声而不是阻尼。
        double rateDegPerSec = 0.0;
        if (Double.isFinite(lastFacingDeg) && now > lastSteerTick) {
            rateDegPerSec = Mth.clamp(Mth.wrapDegrees(facingDeg - lastFacingDeg) / dtSec,
                    -RATE_CLAMP_DEG_PER_SEC, RATE_CLAMP_DEG_PER_SEC);
        }
        lastSteerTick = now;
        lastFacingDeg = facingDeg;

        double pTerm = kp * errNorm;
        double iTerm = ki * integralErr;
        // D 项贡献上限：物理抖动下 D 很容易一把给满舵，实测输出会 5→14→6→5 地 bang-bang。
        double dTerm = Mth.clamp(kd * (rateDegPerSec / 90.0), -D_TERM_CAP, D_TERM_CAP);
        double out = pTerm + iTerm - dTerm;

        double magnitude = Math.abs(out) * maxPower;
        // 输出低通：新值只占 35%，避免信号一跳一跳
        if (Double.isFinite(smoothOutput)) {
            magnitude = smoothOutput + (magnitude - smoothOutput) * OUTPUT_SMOOTH;
        }
        smoothOutput = magnitude;

        int mag = (int) Math.round(magnitude);
        if (mag < TURN_MIN_OUTPUT) {
            // 低于舵机启动阈值：给零。
            //
            // 1.1.15 曾在这里加过「出死区至少给 5」的地板，用来绕开 90° 归一化造成的 67° 死区。
            // 但地板本身会把小偏差的舵量顶到 36%（5/14），落点必然过冲、来回找点 —— 那是「不准」的另一半原因。
            // 现在归一化基准改成可调的「满舵角度」（默认 30°），67° 死区不存在了，地板也就不需要了。
            return 0;
        }
        mag = Math.min(maxPower, mag);

        if (dbgSteer < 30 && (level.getGameTime() % 20L == 0L)) {
            dbgSteer++;
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Steer] [{}] pos={} facing={} facingDeg={} targetDeg={} diff={} abs={} mag={} out={} p={} i={} d={} 推进锁={}",
                    terminalName == null || terminalName.isBlank() ? "未命名" : terminalName,
                    worldPosition.toShortString(), facing,
                    String.format("%.1f", facingDeg), String.format("%.1f", targetDeg),
                    String.format("%.1f", diff), String.format("%.1f", abs), mag,
                    diff < 0 ? "+" + mag : "-" + mag,
                    String.format("%.2f", pTerm), String.format("%.2f", iTerm),
                    String.format("%.2f", dTerm),
                    alignLatched);
        }
        // MC 的 atan2(x, z)：北=180° 东=90° 南=0° 西=-90°，
        // 所以俯视顺时针（右转）对应角度减小 -> diff < 0 才是右转。
        // 幅值已由 PID 决定（含阻尼），方向只认 diff 的符号。
        boolean right = diff < 0;
        if (steerInverted) {
            right = !right;
        }
        return right ? mag : -mag;
    }

    /**
     * 目标相对机头的偏离角度（0=正前方，180=正后方）。
     *
     * @param facing 机头朝向
     * @return 0..180；无目标返回 180
     */
    public double deviationFromFacing(net.minecraft.core.Direction facing) {
        if (!hasTarget() || facing == null) {
            return 180.0;
        }
        // 和 directionPower 一样必须用世界坐标：
        // worldPosition / facing.getStepX 都是 SubLevel 局部量，直接和世界坐标算会完全错
        double[] self = this.worldCenterXZ();
        double[] normal = this.worldNormalXZ(facing);
        double dx = (double) targetX - self[0];
        double dz = (double) targetZ - self[1];
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) {
            return 0.0;
        }
        double dot = (dx * normal[0] + dz * normal[1]) / len;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    /**
     * 线控操纵通道的输出。
     *
     * @param channel 0=前进 1=后退 2=左转 3=右转
     * @param facing  机头朝向
     */
    public int controlPower(int channel, net.minecraft.core.Direction facing) {
        if (!armed || driveMode == DRIVE_MANUAL || !hasTarget() || facing == null) {
            return 0;
        }
        // 通道相对机头的角度偏移：0=前进 180=后退 +90=左 -90=右
        double offset = switch (channel) {
            case 0 -> 0.0;
            case 1 -> 180.0;
            case 2 -> 90.0;
            case 3 -> -90.0;
            default -> Double.NaN;
        };
        if (Double.isNaN(offset) || channel == 2 || channel == 3) {
            // 转向通道由 steeringRate() 统一算（含 PID），这里不再重复
            return 0;
        }

        // 方向计算与转向用同一个机头基准（见 noseBearingDeg 的注释）：
        // 旧实现用方块面法线 × 世界目标做点积，终端 FACING 与真实机头差 180° 时点积恒为负，
        // asin 出负值被钳成 0 —— 表现就是「转向能动、前进永远是 0」。
        double[] self = this.worldCenterXZ();
        double dx = (double) targetX - self[0];
        double dz = (double) targetZ - self[1];
        if (Math.abs(dx) < 1.0e-4 && Math.abs(dz) < 1.0e-4) {
            return 0;
        }
        double targetDeg = Math.toDegrees(Math.atan2(dx, dz));
        double nose = this.noseBearingDeg();

        // 先对准（两段式，见 alignLatched 注释）：只对「前进」通道生效，
        // 后退通道靠下面的推力锥判断，不能被机头对准与否卡住。
        double noseDiff = Math.abs(Mth.wrapDegrees(targetDeg - nose));
        if (flightStrategy == STRATEGY_ALIGN_FIRST && channel == 0) {
            if (alignLatched) {
                if (noseDiff >= ALIGN_ZERO_DEG) {
                    // 目标被甩到侧后方（大幅越障/被撞偏）：解锁，退回只转向
                    alignLatched = false;
                    return 0;
                }
            } else {
                if (noseDiff > ALIGN_THRESHOLD_DEG) {
                    return 0;          // 还没对准：只转向
                }
                alignLatched = true;   // 对准完成，锁存推进
            }
        }

        double diff = Mth.wrapDegrees(targetDeg - (nose + offset));
        double absDiff = Math.abs(diff);

        // 推力只由两件事决定：**对准程度** 和 **剩余距离**。
        //
        // 原来用 asin(dot) 做非线性分配，曲线太陡：偏角 30° 只剩 10，45° 只剩 7.5，
        // 现场感受就是「明明没进减速带，出力也只有一半」。
        // 现在改成锥形分配：
        //   |diff| ≤ ALIGN_FULL_DEG           → 满推力
        //   ALIGN_FULL_DEG ~ ALIGN_ZERO_DEG   → 线性递减
        //   ≥ ALIGN_ZERO_DEG                  → 0（朝侧面/后方推没有意义）
        if (absDiff >= ALIGN_ZERO_DEG) {
            return 0;
        }
        double alignScale = absDiff <= ALIGN_FULL_DEG ? 1.0
                : (ALIGN_ZERO_DEG - absDiff) / (ALIGN_ZERO_DEG - ALIGN_FULL_DEG);
        int raw = (int) Math.round(maxPower * alignScale);
        if (raw <= 0) {
            return 0;
        }
        raw = applyApproachDecel(raw);
        return Math.min(maxPower, raw);
    }

    /**
     * 距离减速：进入「减速距离」后按剩余距离线性收油门，到「到达半径」收干净。
     *
     * <p>为什么需要：原先推力只由<b>方位</b>（asin 投影）决定，与距离无关，
     * 于是一路满推冲进到达半径、然后突然断力 —— 表现为到点前不减速、过冲、来回找点。
     *
     * <pre>
     *   scale = (dist - arriveRadius) / (decelDistance - arriveRadius)，夹到 0..1
     * </pre>
     *
     * @param raw 已按方位算好的推力（0..maxPower）
     */
    private int applyApproachDecel(int raw) {
        if (raw <= 0 || decelDistance <= 0) {
            return raw;
        }
        double dist = distanceToTarget();
        if (dist < 0) {
            return raw;
        }
        double from = Math.max(1.0, arriveRadius);
        double to = Math.max(decelDistance, from + 1.0);
        if (dist >= to) {
            return raw;
        }
        double scale = (dist - from) / (to - from);
        scale = Math.max(0.0, Math.min(1.0, scale));
        int out = (int) Math.round(raw * scale);
        // 距离减速之后仍然保留最小 1：不然在减速带尾段会完全断力、靠惯性飘
        return out <= 0 ? 1 : out;
    }

    public int directionPower(net.minecraft.core.Direction dir) {
        if (!hasTarget() || dir == null) {
            return 0;
        }
        int vx = dir.getStepX();
        int vz = dir.getStepZ();
        if (vx == 0 && vz == 0) {
            return 0;
        }
        // 终端可能在飞行器（SubLevel）上，worldPosition 是局部坐标，
        // 必须投影到世界坐标才能和导航目标比较
        double[] self = this.worldCenterXZ();
        double dx = (double) targetX - self[0];
        double dz = (double) targetZ - self[1];
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) {
            return 0;
        }
        // 面法线同样要转到世界坐标
        double[] normal = this.worldNormalXZ(dir);
        double dot = (dx * normal[0] + dz * normal[1]) / len;
        dot = Math.max(-1.0, Math.min(1.0, dot));

        // 照搬航空学导航台的非线性分配（asin 投影），再按 maxPower 折算
        // 注意 MAX_POWER 是 14 不是 15：15 在部分机构里是「常通/极限位」
        int raw = (int) (Math.asin(dot) / Math.PI * 30.0 + 0.5);
        if (raw <= 0) {
            return 0;
        }
        if (maxPower != 15) {
            raw = (int) Math.round(raw * (maxPower / 15.0));
        }
        return Mth.clamp(raw, MIN_POWER, maxPower);
    }

    /** 每个方向上次发出的强度，用于判断是否需要通知邻居（照搬导航台） */
    private final java.util.Map<net.minecraft.core.Direction, Integer> signalStrengthCache
            = new java.util.EnumMap<>(net.minecraft.core.Direction.class);

    /** 终端中心的世界坐标（x, z）；在 SubLevel 上会做投影 */
    private double[] worldCenterXZ() {
        if (com.hzldm.createnavigationterminal.integration.sable.SableTransform.isAvailable()) {
            var v = com.hzldm.createnavigationterminal.integration.sable.SableTransform
                    .worldCenter(this);
            return new double[]{v.x, v.z};
        }
        return new double[]{worldPosition.getX() + 0.5, worldPosition.getZ() + 0.5};
    }

    /** 某个面的法线在世界坐标里的 (x, z)；在 SubLevel 上会做旋转 */
    private double[] worldNormalXZ(net.minecraft.core.Direction dir) {
        if (com.hzldm.createnavigationterminal.integration.sable.SableTransform.isAvailable()) {
            var v = com.hzldm.createnavigationterminal.integration.sable.SableTransform
                    .worldNormal(this, dir);
            return new double[]{v.x, v.z};
        }
        return new double[]{dir.getStepX(), dir.getStepZ()};
    }

    private void refreshRedstone(boolean force) {
        if (level == null || level.isClientSide()) {
            return;
        }
        // 主信号（距离衰减）用于判断是否需要整体刷新
        int next = computePower();
        if (force || next != redstonePower) {
            redstonePower = next;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        // 逐方向刷新：照搬导航台，只对强度变了的那个方向发邻居更新，
        // 避免每次全量刷新把周围方块全部重新求值
        selectivelyUpdateNeighbors();

    }

    /**
     * 只对「强度发生变化」的方向触发邻居更新（照搬航空学导航台的做法）。
     *
     * @return 是否发生过更新
     */
    private boolean selectivelyUpdateNeighbors() {
        if (level == null || level.isClientSide()) {
            return false;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(com.hzldm.createnavigationterminal.block.NavigationTerminalBlock.FACING)) {
            return false;
        }
        boolean notify = false;
        if (dbgRsFaces < 20 && level.getGameTime() % 40L == 0L) {
            dbgRsFaces++;
            StringBuilder sb = new StringBuilder();
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                sb.append(d.name().charAt(0)).append('=')
                        .append(computePowerByFace(d)).append(' ');
            }
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[RsFace] [{}] {} dist={} range={} mode={} | {}",
                    terminalName == null || terminalName.isBlank() ? "\u672a\u547d\u540d" : terminalName,
                    worldPosition.toShortString(), (int) distanceToTarget(), maxRange, mode, sb);
        }
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (dir.getAxis() == state.getValue(
                    com.hzldm.createnavigationterminal.block.NavigationTerminalBlock.FACING).getAxis()) {
                continue;
            }
            int old = signalStrengthCache.getOrDefault(dir, -1);
            int cur = computePowerByFace(dir);
            if (old == cur) {
                continue;
            }
            signalStrengthCache.put(dir, cur);
            level.updateNeighborsAt(worldPosition.relative(dir), state.getBlock());
            notify = true;
        }
        return notify;
    }

    /** 服务端 tick：维护到达锁存 + 刷新强度（内部 8 tick 节流） */
    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        // 转向控制每 tick 算一次（20 Hz），结果缓存给 8 tick 一次的线缆推送用。
        // 之前只在推送时算 → 闭环只有 2.5 Hz，D 项（角速度）在这种采样率下基本是噪声，
        // 也是「前进时左右来回摆」的结构性原因之一。
        this.cachedSteer = steeringRate(effectiveFacing());
        if ((level.getGameTime() & 7L) != 0L) {
            return;
        }
        updateArrived();
        logNoseOnce();
        logDisarmedOnce();
        refreshRedstone(false);
        syncDistanceToClient();
        com.hzldm.createnavigationterminal.integration.drivebywire.WireSignalBridge
                .pushAll(this, level, worldPosition);
    }

    /** 最近一次 20 Hz 算出的转向指令（负数=左转，正数=右转，0=不转） */
    private int cachedSteer = 0;

    public int getCachedSteer() {
        return cachedSteer;
    }

    /**
     * 把服务端算好的水平距离同步给客户端。
     *
     * 客户端自己算距离曾出现天文数字（用了可疑的坐标副本），
     * 干脆以服务端为准：红石和界面显示同源，永不打架。
     */
    private void syncDistanceToClient() {
        double d = distanceToTarget();
        int next = d < 0 ? -1 : (int) Math.round(Math.min(d, Integer.MAX_VALUE - 1));
        if (next != syncedDistance) {
            syncedDistance = next;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** 服务端算出的水平距离（-1=无目标或不同维度）；客户端直接显示这个值 */
    public int getSyncedDistance() {
        return syncedDistance;
    }

    /**
     * 到达判定与锁存（只在首次进入半径时做一次迁移）
     *
     *  - ARRIVE_SILENT：锁存 arrived，信号由 computePower 归 0
     *  - ARRIVE_CLEAR ：直接清空目标（targetName 置空后 computePower 首判即返回 0，天然幂等）
     *  - ARRIVE_HOLD  ：锁存 arrived 仅供界面提示，信号照常
     */
    /** 到达事件日志（每终端最多 10 条），用来确认到达判定真的触发了 */
    private static int dbgArrive = 0;

    private void logArrive(double dist, String what) {
        if (dbgArrive >= 10) {
            return;
        }
        dbgArrive++;
        com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                "[Arrive] [{}] pos={} dist={} 半径={} 动作={} → {}",
                terminalName == null || terminalName.isBlank() ? "未命名" : terminalName,
                worldPosition.toShortString(), String.format("%.1f", dist), arriveRadius,
                arriveAction, what);
    }

    private void updateArrived() {
        if (!hasTarget() || arrived) {
            return;
        }
        double dist = distanceToTarget();
        if (dist < 0 || dist > arriveRadius) {
            return;
        }
        if (arriveAction == ARRIVE_CLEAR) {
            if (route.size() > 1) {
                // 多目标：切下一个（循环），继续导航
                nextRouteTarget();
                return;
            }
            logArrive(dist, "清除目标");
            this.targetName = "";
            this.targetDimension = "";
            this.targetX = 0;
            this.targetY = 0;
            this.targetZ = 0;
            this.arrived = false;
        } else {
            logArrive(dist, arriveAction == ARRIVE_SILENT ? "锁存并静默" : "锁存并保持输出");
            this.arrived = true;
            // 多目标 + 保持输出：也切下一个，否则航线卡死在第一站
            if (route.size() > 1) {
                nextRouteTarget();
                return;
            }
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && fleetChannel >= 0) {
            com.hzldm.createnavigationterminal.fleet.FleetRegistry.register(
                    level.dimension(), worldPosition, fleetChannel);
        }
        // 服务端登记，供 HUD 服务查找绑定了座椅的终端
        if (level != null && !level.isClientSide()) {
            ACTIVE.add(this);
        }
        // 首次加载参数已从 NBT 恢复，立刻算一次，避免依赖第一个 tick 才有信号
        if (level != null && !level.isClientSide()) {
            refreshRedstone(true);
        }
    }

    @Override
    public void setRemoved() {
        ACTIVE.remove(this);
        super.setRemoved();
    }

    // ------------------------------------------------------------------
    // NBT / 同步
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("TargetName", targetName);
        tag.putInt("TargetX", targetX);
        tag.putInt("TargetY", targetY);
        tag.putInt("TargetZ", targetZ);
        tag.putString("TargetDimension", targetDimension);
        tag.putInt("Mode", mode);
        tag.putInt("MaxPower", maxPower);
        tag.putInt("MaxRange", maxRange);
        tag.putInt("ArriveRadius", arriveRadius);
        tag.putInt("ArriveAction", arriveAction);
        tag.putInt("DecelDistance", decelDistance);
        tag.putInt("SteerScaleDeg", steerScaleDeg);
        tag.putBoolean("Arrived", arrived);
        tag.putString("BoundSeat", boundSeatUuid);
        tag.putString("TerminalName", terminalName == null ? "" : terminalName);
        tag.putBoolean("InFleet", inFleet);
        tag.putInt("FleetChannel", fleetChannel);
        tag.putIntArray("FaceSource", faceSource);
        tag.putInt("DriveMode", driveMode);
        tag.putInt("FlightStrategy", flightStrategy);
        tag.putBoolean("DriveLock", driveLock);
        tag.putInt("HeadingOverride", headingOverride);
        tag.putDouble("HeadingOffsetDeg", headingOffsetDeg);
        tag.putDouble("Kp", kp);
        tag.putDouble("Ki", ki);
        tag.putDouble("Kd", kd);
        tag.putBoolean("SteerInverted", steerInverted);

        // 航线卡槽持久化（空栈不能 save，会抛 Cannot encode empty ItemStack）
        if (!cardSlot.getItem(0).isEmpty()) {
            var cardTag = new net.minecraft.nbt.CompoundTag();
            cardSlot.getItem(0).save(registries, cardTag);
            tag.put("CardSlot", cardTag);
        }

        // 航线持久化
        net.minecraft.nbt.ListTag routeTag = new net.minecraft.nbt.ListTag();
        for (var t : route) {
            net.minecraft.nbt.CompoundTag e = new net.minecraft.nbt.CompoundTag();
            e.putString("N", t.name());
            e.putInt("X", t.x());
            e.putInt("Y", t.y());
            e.putInt("Z", t.z());
            e.putString("D", t.dimension());
            routeTag.add(e);
        }
        tag.put("Route", routeTag);
        tag.putInt("RouteIdx", routeIndex);
        tag.putBoolean("RouteLoop", routeLoop);

        // 预设持久化
        for (int i = 0; i < 3; i++) {
            tag.putBoolean("PresetSet" + i, presetSet[i]);
            tag.putIntArray("Preset" + i, presets[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.targetName = safeTrim(tag.getString("TargetName"), 64);
        this.targetX = tag.getInt("TargetX");
        this.targetY = tag.getInt("TargetY");
        this.targetZ = tag.getInt("TargetZ");
        this.targetDimension = safeTrim(tag.getString("TargetDimension"), 128);
        this.mode = Mth.clamp(tag.getInt("Mode"), MODE_RAMP, MODE_FIXED);
        this.maxPower = Mth.clamp(tag.getInt("MaxPower"), MIN_POWER, MAX_POWER);
        this.arriveRadius = Mth.clamp(tag.getInt("ArriveRadius"), MIN_ARRIVE_RADIUS, MAX_ARRIVE_RADIUS);
        int loadedRange = tag.getInt("MaxRange");
        if (loadedRange <= this.arriveRadius) {
            loadedRange = Math.min(MAX_RANGE, this.arriveRadius + 1);
        }
        this.maxRange = Mth.clamp(loadedRange, MIN_RANGE, MAX_RANGE);
        this.arriveAction = Mth.clamp(tag.getInt("ArriveAction"), ARRIVE_HOLD, ARRIVE_CLEAR);
        // 老存档没有这个键：用默认值（不是 0），否则升级后距离减速会静默失效
        this.decelDistance = Mth.clamp(
                tag.contains("DecelDistance") ? tag.getInt("DecelDistance") : DEFAULT_DECEL,
                MIN_DECEL, MAX_DECEL);
        this.steerScaleDeg = Mth.clamp(
                tag.contains("SteerScaleDeg") ? tag.getInt("SteerScaleDeg") : DEFAULT_STEER_SCALE,
                MIN_STEER_SCALE, MAX_STEER_SCALE);
        this.arrived = tag.getBoolean("Arrived");
        this.boundSeatUuid = tag.contains("BoundSeat") ? tag.getString("BoundSeat") : "";
        this.terminalName = tag.contains("TerminalName") ? tag.getString("TerminalName") : "";
        this.inFleet = tag.getBoolean("InFleet");
        this.fleetChannel = tag.getInt("FleetChannel");

        // 航线卡槽恢复
        if (tag.contains("CardSlot")) {
            var cardTag = tag.getCompound("CardSlot");
            cardSlot.setItem(0, net.minecraft.world.item.ItemStack.parse(registries, cardTag)
                    .orElse(net.minecraft.world.item.ItemStack.EMPTY));
        }
        this.syncedDistance = tag.contains("Dist") ? tag.getInt("Dist") : -1;

        // 航线恢复
        route.clear();
        if (tag.contains("Route")) {
            net.minecraft.nbt.ListTag routeTag = tag.getList("Route", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < routeTag.size(); i++) {
                net.minecraft.nbt.CompoundTag e = routeTag.getCompound(i);
                route.add(new com.hzldm.createnavigationterminal.item.WpTarget(
                        e.getString("N"), e.getInt("X"), e.getInt("Y"), e.getInt("Z"), e.getString("D")));
            }
        }
        this.routeIndex = tag.getInt("RouteIdx");
        if (routeIndex < 0 || routeIndex >= Math.max(1, route.size())) {
            routeIndex = 0;
        }
        this.routeLoop = !tag.contains("RouteLoop") || tag.getBoolean("RouteLoop");

        // 预设恢复
        for (int i = 0; i < 3; i++) {
            presetSet[i] = tag.getBoolean("PresetSet" + i);
            int[] arr = tag.getIntArray("Preset" + i);
            if (arr.length == 5) {
                presets[i] = arr;
            }
        }

        // 红石面输出与自动驾驶设置（saveAdditional 已写入，这里必须读回）
        int[] fs = tag.getIntArray("FaceSource");
        if (fs.length == 6) {
            System.arraycopy(fs, 0, faceSource, 0, 6);
        }
        this.driveMode = tag.getInt("DriveMode") == DRIVE_MANUAL ? DRIVE_MANUAL : DRIVE_AUTO;
        this.flightStrategy = tag.getInt("FlightStrategy") == STRATEGY_ALIGN_FIRST
                ? STRATEGY_ALIGN_FIRST : STRATEGY_FLY_AND_CORRECT;
        this.driveLock = !tag.contains("DriveLock") || tag.getBoolean("DriveLock");
        this.headingOverride = tag.contains("HeadingOverride")
                ? Math.max(-1, Math.min(3, tag.getInt("HeadingOverride"))) : -1;
        this.headingOffsetDeg = tag.getDouble("HeadingOffsetDeg");
        this.kp = Mth.clamp(tag.contains("Kp") ? tag.getDouble("Kp") : 0.45, 0.0, 3.0);
        this.ki = Mth.clamp(tag.contains("Ki") ? tag.getDouble("Ki") : 0.0, 0.0, 2.0);
        this.kd = Mth.clamp(tag.contains("Kd") ? tag.getDouble("Kd") : 0.80, 0.0, 3.0);
        this.steerInverted = tag.getBoolean("SteerInverted");

        // 读档一律待命：目标/模式会恢复，但「出击许可」不持久化。
        // 否则加载瞬间就按旧目标满舵（实测 values=[0,7,0,8]），无人看管的载具会被当场开走。
        this.armed = false;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        // 距离不落盘（每次都由服务端重算），但必须随同步包下发
        tag.putInt("Dist", syncedDistance);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------
    // MenuProvider
    // ------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.create_navigation_terminal.navigation_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NavigationTerminalMenu(id, inventory, this);
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static String safeTrim(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
