/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity
 *  com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect$RawWp
 *  com.hzldm.createnavigationterminal.integration.xaero.XaeroWaypointService
 *  com.hzldm.createnavigationterminal.item.WaypointCardData
 *  com.hzldm.createnavigationterminal.item.WpTarget
 *  com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu
 *  com.hzldm.createnavigationterminal.network.AddRoutePayload
 *  com.hzldm.createnavigationterminal.network.BindSeatPayload
 *  com.hzldm.createnavigationterminal.network.ExportRouteToCardPayload
 *  com.hzldm.createnavigationterminal.network.PresetPayload
 *  com.hzldm.createnavigationterminal.network.RemoveRoutePayload
 *  com.hzldm.createnavigationterminal.network.ReorderRoutePayload
 *  com.hzldm.createnavigationterminal.network.ResetArrivedPayload
 *  com.hzldm.createnavigationterminal.network.RouteEditPayload
 *  com.hzldm.createnavigationterminal.network.SetFleetChannelPayload
 *  com.hzldm.createnavigationterminal.network.SetNavigationSettingsPayload
 *  com.hzldm.createnavigationterminal.network.SetNavigationTargetPayload
 *  com.hzldm.createnavigationterminal.registry.ModDataComponents
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.blaze3d.platform.NativeImage$Format
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.level.material.MapColor$Brightness
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package com.hzldm.createnavigationterminal.client;

import com.hzldm.createnavigationterminal.blockentity.NavigationTerminalBlockEntity;
import com.hzldm.createnavigationterminal.integration.xaero.XaeroReflect;
import com.hzldm.createnavigationterminal.integration.xaero.XaeroWaypointService;
import com.hzldm.createnavigationterminal.item.WaypointCardData;
import com.hzldm.createnavigationterminal.item.WpTarget;
import com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu;
import com.hzldm.createnavigationterminal.network.AddRoutePayload;
import com.hzldm.createnavigationterminal.network.BindSeatPayload;
import com.hzldm.createnavigationterminal.network.ExportRouteToCardPayload;
import com.hzldm.createnavigationterminal.network.PresetPayload;
import com.hzldm.createnavigationterminal.network.RemoveRoutePayload;
import com.hzldm.createnavigationterminal.network.ReorderRoutePayload;
import com.hzldm.createnavigationterminal.network.ResetArrivedPayload;
import com.hzldm.createnavigationterminal.network.RouteEditPayload;
import com.hzldm.createnavigationterminal.network.SetFleetChannelPayload;
import com.hzldm.createnavigationterminal.network.SetNavigationSettingsPayload;
import com.hzldm.createnavigationterminal.network.SetNavigationTargetPayload;
import com.hzldm.createnavigationterminal.registry.ModDataComponents;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class NavigationTerminalScreen
extends AbstractContainerScreen<NavigationTerminalMenu> {
    // ---- 配色统一走 NavGuiStyle（Create 行情牌 / 唱片机那套：铜色机身 + 米色内嵌面板）----
    private static final int BG_COLOR = NavGuiStyle.BODY;
    private static final int BORDER_COLOR = NavGuiStyle.BORDER;
    private static final int TEXT_COLOR = NavGuiStyle.TEXT;
    private static final int SUB_COLOR = NavGuiStyle.MUTED;
    private static final int OK_COLOR = NavGuiStyle.GREEN;
    private static final int WARN_COLOR = NavGuiStyle.ACCENT;
    private static final int LIST_COLOR = NavGuiStyle.PANEL_ALT;
    private static final int SEL_COLOR = NavGuiStyle.WASH_CURRENT;
    private static final int W = 470;
    private static final int H = 322;
    private static final int LIST_X = 10;
    private static final int LIST_W = 300;
    private static final int ROW_H = 16;
    private static final int TAB_Y = 22;
    private static final int TAB_W = 96;
    private static final int TAB_H = 16;
    private static final int NAV_LIST_Y = 102;
    private static final int NAV_VISIBLE = 8;
    private static final int PARAM_Y0 = 56;
    private static final int PARAM_DY = 26;
    private static final int PRESET_Y = 198;
    private static final int RANGE_STEP = 16;
    private static final int MENU_W = 140;
    private static final int MENU_ITEM_H = 16;
    private static final int MAP_X = 10;
    private static final int MAP_Y = 48;
    private static final int MAP_W = 300;
    private static final int MAP_H = 132;
    private static final int MAP_SAMPLE_W = 300;
    private static final int MAP_SAMPLE_H = 132;
    private static final double MAP_RANGE_EPS = 32.0;
    private static final long MAP_REFRESH_MS = 3000L;
    private static final int ROUTE_LIST_Y = 188;
    private static final int ROUTE_VISIBLE = 4;
    private static final String[] MODE_NAMES = new String[]{"create_navigation_terminal.nav.mode.ramp", "create_navigation_terminal.nav.mode.switch", "create_navigation_terminal.nav.mode.fixed"};
    private static final String[] ACTION_NAMES = new String[]{"create_navigation_terminal.nav.arrive.hold", "create_navigation_terminal.nav.arrive.silent", "create_navigation_terminal.nav.arrive.clear"};
    private static final String[] PAGE_NAMES = new String[]{"create_navigation_terminal.page.nav", "create_navigation_terminal.page.param", "create_navigation_terminal.page.route"};
    private int currentPage = 0;
    private List<WpTarget> candidates = List.of();
    private boolean xaeroLoaded = false;
    /** 鼠标在 GUI 内的坐标（悬停高亮用） */
    private int hoverGuiX = -1;
    private int hoverGuiY = -1;
    private int scroll = 0;
    private int localMode = 0;
    private int localPower = 14;
    private int localRange = 256;
    private int localRadius = 16;
    private int localAction = 1;
    /** 距离减速：进入该距离后按剩余距离收油门；0=关闭 */
    private int localDecel = 64;
    /** 满舵角度：偏角达到它给满舵 */
    private int localSteerScale = 30;
    private int lastPresetSlot = 0;
    private Button loopButton;
    /** 标题栏/左栏的设置按钮（打开设置面板时变绿） */
    private NavButton settingsBtn;
    /** 卡片「改名」与「只读」按钮 */
    private NavButton renameBtn;
    private NavButton readOnlyBtn;
    private int menuIdx = -1;
    private int menuX;
    private int menuY;
    private DynamicTexture mapTexture;
    private static int dbgSample = 0;
    private static int dbgMapPanel = 0;
    private static int dbgDrag = 0;
    private static int dbgMapDraw = 0;
    private static int dbgPt = 0;
    private ResourceLocation mapLocation;
    private int mapSerial;
    private double mapMinX;
    private double mapMaxX;
    private double mapMinZ;
    private double mapMaxZ;
    private long mapSampleTime;
    private double mapZoom = 1.0;
    private static final double MAP_ZOOM_MAX = 8.0;
    private static final double MAP_ZOOM_STEP = 1.4;
    private int dragFrom = -1;
    private int dragTo = -1;

    /** 设置面板页码：0=输出，1=驾驶 */
    private int settingsPage = 0;
    /** 每页包含的行号（row 从 1 开始） */
    private static final int[][] PAGE_ROWS = {
            {1, 2, 3, 4, 5, 15},       // 输出（15=距离减速）
            {6, 7, 8, 9, 10, 11, 16},  // 驾驶（16=满舵角度）
            {12, 13, 14}               // PID
    };
    private static final String[] CFG_PAGE_NAMES = {
            "\u8f93\u51fa", "\u9a7e\u9a76", "PID"};

    private boolean rowOnPage(int row) {
        for (int r : PAGE_ROWS[settingsPage]) {
            if (r == row) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 设置项表（4.4 重构）
    //
    // 以前是「PAGE_ROWS / labels[] / values[]」三处并行硬编码，长度一旦不一致就静默少画，
    // 而且旧实现靠一个扁平按钮表的魔法下标（[2..24) 是参数行箭头）来定位，PID 页的箭头会漏到别的页面上。
    // 现在：一行 = 一个 SettingRow（文案 + 实时取值），按钮分两个容器，可见性由 rowOnPage 决定。
    // 加参数只需要：ROW_COUNT +1、PAGE_ROWS 里挂上、buildSettingRows 里加一条、cycle() 加一个分支。
    // ------------------------------------------------------------------

    /** 设置项总数（行号 1..ROW_COUNT） */
    private static final int ROW_COUNT = 16;

    /** 一行设置：左边固定文案，右边取值（每次重画时实时求值） */
    private record SettingRow(String label, java.util.function.Supplier<String> value) {
    }

    private final SettingRow[] settingRows = new SettingRow[ROW_COUNT + 1];

    // ---- 设置覆盖层几何（自绘控件与点击命中共用一套常量）----
    private static final int CFG_X = 30;
    private static final int CFG_Y = 22;
    private static final int CFG_W = 220;
    private static final int CFG_H = 165;
    private static final int CFG_ROW_Y0 = 22;
    private static final int CFG_ROW_DY = 17;
    private static final int CFG_VAL_CX = 108;
    private static final int CFG_ARROW_LX = 150;
    private static final int CFG_ARROW_RX = 190;
    /** 第 11 行是航向校准输入框，没有左右箭头 */
    private static final int ROW_HEADING_OFFSET = 11;

    /** 参数行的左右箭头：下标 = (row-1)*2 + 0/1；第 11 行是输入框，留 null */
    private final Button[] rowArrows = new Button[ROW_COUNT * 2];

    /** 与行无关的常驻按钮：分页、预设、频道 */
    private final java.util.List<Button> chromeButtons = new java.util.ArrayList<>();

    private void buildSettingRows() {
        java.util.Arrays.fill(this.settingRows, null);
        this.settingRows[1] = new SettingRow("\u6a21\u5f0f", () -> this.modeName(this.localMode));
        this.settingRows[2] = new SettingRow("\u5f3a\u5ea6\u4e0a\u9650", () -> String.valueOf(this.localPower));
        this.settingRows[3] = new SettingRow("\u8986\u76d6\u8ddd\u79bb", () -> this.localRange + " \u683c");
        this.settingRows[4] = new SettingRow("\u5230\u8fbe\u534a\u5f84", () -> this.localRadius + " \u683c");
        this.settingRows[5] = new SettingRow("\u5230\u8fbe\u540e", () -> this.actionName(this.localAction));
        this.settingRows[6] = new SettingRow("\u9a7e\u9a76\u6a21\u5f0f", this::driveModeLabel);
        this.settingRows[7] = new SettingRow("\u98de\u884c\u7b56\u7565", this::strategyLabel);
        this.settingRows[8] = new SettingRow("\u9a7e\u9a76\u9501", this::driveLockLabel);
        this.settingRows[9] = new SettingRow("\u673a\u5934\u65b9\u5411", this::headingLabel);
        this.settingRows[10] = new SettingRow("\u8f6c\u5411\u53cd\u5411", this::steerInvertLabel);
        this.settingRows[11] = new SettingRow("\u822a\u5411\u6821\u51c6", () -> "");   // 由输入框显示
        this.settingRows[12] = new SettingRow("Kp \u6bd4\u4f8b", () -> this.pidLabel(0));
        this.settingRows[13] = new SettingRow("Ki \u79ef\u5206", () -> this.pidLabel(1));
        this.settingRows[14] = new SettingRow("Kd \u5fae\u5206", () -> this.pidLabel(2));
        this.settingRows[15] = new SettingRow("\u51cf\u901f\u8ddd\u79bb",
                () -> this.localDecel <= 0 ? "\u5173\u95ed" : this.localDecel + " \u683c");
        // 满舵角度：偏角达到它给满舵（越小越准、越大越稳）
        this.settingRows[16] = new SettingRow("\u6ee1\u8235\u89d2\u5ea6",
                () -> this.localSteerScale + " \u00b0");
    }

    /** 航向校准输入框（手动输入角度） */
    private net.minecraft.client.gui.components.EditBox headingOffsetBox;

    /** 设置面板里正在编辑的那个面（0..5，对应 Direction.ordinal） */
    private int editingFace = 2;   // 默认 NORTH

    /** 路径点（自由选择）列表的可视行数，渲染与滚动上限必须一致。 */
    private static final int CANDIDATE_VISIBLE = 3;

    /** 航线列表的滚动偏移。 */
    private int routeScroll = 0;

    /** 循环模式的本地副本（点按钮后立即翻转，不必等服务端）。 */
    private boolean localRouteLoop = true;
    /** 是否已经从 BlockEntity 同步过（只同步一次，之后以本地操作为准）。 */
    private boolean loopSynced = false;

    /** 循环模式的显示文案。 */
    private net.minecraft.network.chat.Component loopLabel() {
        return net.minecraft.network.chat.Component.literal(
                this.localRouteLoop ? "\u5faa\u73af" : "\u5355\u7a0b");
    }

    /** 拖动期间贴图的像素平移量：让画面跟手，同时不触发重采样。 */
    private float dragRenderOffsetX = 0;
    private float dragRenderOffsetY = 0;

    /** 采样时的 region 局部缓存（同一 region 内避免重复查 Map）。 */
    private int[] cachedRgImg;
    private int cachedRgX = Integer.MIN_VALUE;
    private int cachedRgZ = Integer.MIN_VALUE;

    /** 上一次采样时的世界跨度（供缩放锚点换算）。 */
    private double lastSpanX = 512.0;
    private double lastSpanZ = 512.0;

    /** 地图平移到航线中心之外的偏移（世界格）。 */
    private double mapPanX = 0;
    private double mapPanY = 0;
    /** 是否正在拖动地图。 */
    private boolean draggingMap = false;
    private double mapDragStartX = 0;
    private double mapDragStartY = 0;
    private double mapPanStartX = 0;
    private double mapPanStartY = 0;
    /** 地图区的屏幕矩形（相对 GUI）：x, y, w, h */
    private static final int VIEW_X = 30;
    private static final int VIEW_Y = 22;
    private static final int VIEW_W = 220;
    private static final int VIEW_H = 165;

    public NavigationTerminalScreen(NavigationTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 470;
        this.imageHeight = 322;
        this.inventoryLabelX = 10000;
        this.inventoryLabelY = 10000;
        this.titleLabelX = 10000;
        this.titleLabelY = 10000;
    }

    /**
     * 构建界面控件：页签、导航页按钮、参数页输入框、航线页按钮，以及底部操作栏。
     * 每次切页都会重建，所以布局常量集中在顶部，避免 draw 与 init 各算一套坐标。
     */
    /** 构建界面控件：左栏设置、右侧航线/目标按钮组、候选路点加载。 */
    protected void init() {
        super.init();
        int cx = this.leftPos;
        NavigationTerminalBlockEntity be = this.terminal();
        if (be != null) {
            this.localMode = be.getMode();
            this.localPower = be.getMaxPower();
            this.localRange = be.getMaxRange();
            this.localRadius = be.getArriveRadius();
            this.localAction = be.getArriveAction();
            this.localDecel = be.getDecelDistance();
            this.localSteerScale = be.getSteerScaleDeg();
            if (!this.loopSynced) {
                this.localRouteLoop = be.isRouteLoop();
                this.loopSynced = true;
            }
        }

        // ---- 左栏、卡槽下方：设置（三横杠图标，打开面板时变绿）----
        this.settingsBtn = NavButton.of(Component.literal("\u8bbe\u7f6e"), b -> {
            this.showSettings = !this.showSettings;
        }).bounds(cx + 6, this.topPos + 100, 18, 16).build();
        this.settingsBtn.withIcon((gg, ix, iy, col) -> NavIcons.menu(gg, ix, iy, col));
        this.settingsBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                net.minecraft.network.chat.Component.literal("\u8bbe\u7f6e")));
        this.addRenderableWidget(this.settingsBtn);
        // ---- 卡片：改名 / 只读（设置按钮下方两个小方块）----
        this.renameBtn = NavButton.of(Component.literal("改名"), b ->
                this.openCardNameScreen())
                .bounds(cx + 6, this.topPos + 118, 18, 16).build();
        this.renameBtn.withIcon((gg, ix, iy, col) -> NavIcons.pencil(gg, ix, iy, col));
        this.renameBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal("给这条航线起名")));
        this.addRenderableWidget(this.renameBtn);

        this.readOnlyBtn = NavButton.of(Component.literal("只读"), b -> {
            PacketDistributor.sendToServer(new com.hzldm.createnavigationterminal.network
                    .SetCardSettingsPayload(this.pos(), this.currentCardName(), !this.currentCardReadOnly()));
        }).bounds(cx + 6, this.topPos + 136, 18, 16).build();
        this.readOnlyBtn.withIcon((gg, ix, iy, col) -> NavIcons.lock(gg, ix, iy, col));
        this.readOnlyBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal("只读：不允许把当前航线写入这张卡")));
        this.addRenderableWidget(this.readOnlyBtn);




        // ---- 右侧按钮组 1：航线操作 ----
        // 循环/单程：文字随当前状态变，点一下切一次并重建控件
        net.minecraft.client.gui.components.Button loopBtn =
                NavButton.of(this.loopLabel(), b -> {
                    PacketDistributor.sendToServer(new RouteEditPayload(this.pos(), 3));
                    this.localRouteLoop = !this.localRouteLoop;
                    this.init(this.minecraft, this.width, this.height);
                }).bounds(cx + 256, this.topPos + 194, 74, 16).build();
        loopBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                net.minecraft.network.chat.Component.literal(
                        "\u5faa\u73af\uff1a\u8d70\u5b8c\u6574\u6761\u822a\u7ebf\u540e\u56de\u5230\u7b2c\u4e00\u7ad9\n"
                        + "\u5355\u7a0b\uff1a\u8d70\u5b8c\u6574\u6761\u822a\u7ebf\u540e\u505c\u5728\u6700\u540e\u4e00\u7ad9")));
        this.addRenderableWidget(loopBtn);

        this.addRenderableWidget(NavButton.of(Component.literal("\u5220\u9664\u8def\u5f84\u70b9"), b -> {
            PacketDistributor.sendToServer(new RouteEditPayload(this.pos(), 2));
        }).bounds(cx + 256, this.topPos + 214, 74, 16).build());

        this.addRenderableWidget(NavButton.of(Component.literal("\u25b3"), b -> {
            PacketDistributor.sendToServer(new RouteEditPayload(this.pos(), 0));
        }).bounds(cx + 256, this.topPos + 234, 35, 16).build());

        this.addRenderableWidget(NavButton.of(Component.literal("\u25bd"), b -> {
            PacketDistributor.sendToServer(new RouteEditPayload(this.pos(), 1));
        }).bounds(cx + 295, this.topPos + 234, 35, 16).build());

        // ---- 右侧按钮组 2：目标操作 ----
        this.addRenderableWidget(NavButton.of(
                Component.translatable("create_navigation_terminal.nav.clear_target"), b -> {
                    PacketDistributor.sendToServer(
                            new SetNavigationTargetPayload(this.pos(), "", 0, 0, 0, ""));
                }).bounds(cx + 336, this.topPos + 194, 124, 16).build());

        this.addRenderableWidget(NavButton.of(
                Component.translatable("create_navigation_terminal.nav.reset_arrived"), b -> {
                    PacketDistributor.sendToServer(new ResetArrivedPayload(this.pos()));
                }).bounds(cx + 336, this.topPos + 214, 124, 16).build());

        this.addRenderableWidget(NavButton.of(
                Component.translatable("create_navigation_terminal.nav.bind_seat"), b -> {
                    PacketDistributor.sendToServer(new BindSeatPayload(this.pos()));
                }).bounds(cx + 336, this.topPos + 234, 124, 16).build());

        // ---- 导航卡：写入 / 读取（标题栏右上角小按钮，单字 + tooltip）----
        NavButton exportBtn = NavButton.of(
                Component.literal("\u5199"), b ->
                        {
                            if (this.currentCardReadOnly()) {
                                if (this.minecraft != null && this.minecraft.player != null) {
                                    this.minecraft.player.displayClientMessage(
                                            Component.literal("卡片已只读，不能覆盖")
                                                    .withStyle(net.minecraft.ChatFormatting.RED), true);
                                }
                                return;
                            }
                            PacketDistributor.sendToServer(
                                    new com.hzldm.createnavigationterminal.network
                                            .ExportRouteToCardPayload(this.pos()));
                        })
                .bounds(cx + 6, this.topPos + 64, 18, 16).build();
        exportBtn.withIcon((gg, ix, iy, col) -> NavIcons.arrowDown(gg, ix, iy, col));
        exportBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                net.minecraft.network.chat.Component.literal(
                        "\u5199\u5165\uff1a\u628a\u5f53\u524d\u822a\u7ebf\u5b58\u5165\u5bfc\u822a\u5361")));
        this.addRenderableWidget(exportBtn);

        NavButton importBtn = NavButton.of(
                Component.literal("\u8bfb"), b ->
                        PacketDistributor.sendToServer(
                                new com.hzldm.createnavigationterminal.network
                                        .ImportRouteFromCardPayload(this.pos())))
                .bounds(cx + 6, this.topPos + 82, 18, 16).build();
        importBtn.withIcon((gg, ix, iy, col) -> NavIcons.arrowUp(gg, ix, iy, col));
        importBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                net.minecraft.network.chat.Component.literal(
                        "\u8bfb\u53d6\uff1a\u628a\u5361\u91cc\u7684\u822a\u7ebf\u8f7d\u5165\u7ec8\u7aef")));
        this.addRenderableWidget(importBtn);

        // ---- 编队推送 + 独占频道 ----
        this.addRenderableWidget(NavButton.of(
                Component.literal("\u63a8\u9001\u76ee\u6807"), b ->
                        PacketDistributor.sendToServer(
                                new com.hzldm.createnavigationterminal.network
                                        .PushToFleetPayload(this.pos(), 0)))
                .bounds(cx + 256, this.topPos + 290, 100, 16).build());

        this.addRenderableWidget(NavButton.of(
                Component.literal("\u63a8\u9001\u822a\u7ebf"), b ->
                        PacketDistributor.sendToServer(
                                new com.hzldm.createnavigationterminal.network
                                        .PushToFleetPayload(this.pos(), 1)))
                .bounds(cx + 364, this.topPos + 290, 100, 16).build());

        NavigationTerminalBlockEntity beSolo = this.terminal();
        boolean soloNow = beSolo != null && beSolo.isSoloChannel();
        this.addRenderableWidget(NavButton.of(
                Component.literal(soloNow ? "\u52a0\u5165\u7f16\u961f" : "\u72ec\u5360\u9891\u9053"), b -> {
                    NavigationTerminalBlockEntity cur = this.terminal();
                    if (cur != null) {
                        int t = cur.isSoloChannel() ? 0 : -1;
                        PacketDistributor.sendToServer(
                                new SetFleetChannelPayload(this.pos(), t));
                        this.init(this.minecraft, this.width, this.height);
                    }
                }).bounds(cx + 256, this.topPos + 306, 208, 16).build());

        // ---- 设置面板：分页切换 ----
        // 容器各自独立：chromeButtons（常驻）+ rowArrows（按行号定位），不再靠下标算术
        this.chromeButtons.clear();
        java.util.Arrays.fill(this.rowArrows, null);
        buildSettingRows();
        NavigationTerminalBlockEntity beTitle = this.terminal();
        String termId = (beTitle == null || beTitle.getTerminalName().isBlank())
                ? "\u672a\u547d\u540d" : beTitle.getTerminalName();
        Button pageBtn = NavButton.of(
                Component.literal(termId + " \u00b7 " + CFG_PAGE_NAMES[this.settingsPage]),
                b -> {
                    this.settingsPage = Math.floorMod(this.settingsPage + 1, PAGE_ROWS.length);
                    this.init(this.minecraft, this.width, this.height);
                })
                .bounds(cx + 30 + 40, this.topPos + 22 + 3, 140, 13).build();
        pageBtn.visible = false;
        this.addRenderableWidget(pageBtn);
        this.chromeButtons.add(pageBtn);

        // ---- 参数行的左右箭头（初始隐藏）----
        for (int row = 1; row <= ROW_COUNT; ++row) {
            final int rowId = row;
            // 本页内的序号（0..n-1），决定垂直位置
            int slot = 0;
            for (int r : PAGE_ROWS[settingsPage]) {
                if (r == row) {
                    break;
                }
                slot++;
            }
            // 与覆盖层自绘行同一套几何常量：y0(22) + CFG_ROW_Y0 + slot*CFG_ROW_DY
            int ry = CFG_Y + CFG_ROW_Y0 + slot * CFG_ROW_DY;
            boolean onPage = this.rowOnPage(row);
            if (row == 11) {
                // 航向校准：手动输入，不要 ±15° 步进
                this.headingOffsetBox = new net.minecraft.client.gui.components.EditBox(
                        this.font, cx + CFG_X + 74, this.topPos + ry + 1, 68, 13,
                        Component.literal("\u822a\u5411\u6821\u51c6"));
                this.headingOffsetBox.setMaxLength(8);
                this.headingOffsetBox.setFilter(t -> t.matches("-?\\d*\\.?\\d*"));
                NavigationTerminalBlockEntity be0 = this.terminal();
                this.headingOffsetBox.setValue(be0 == null ? "0"
                        : String.format("%.1f", be0.getHeadingOffsetDeg()));
                this.headingOffsetBox.setResponder(t -> {
                    try {
                        double v = t.isBlank() || t.equals("-") || t.equals(".") ? 0
                                : Double.parseDouble(t);
                        NavigationTerminalBlockEntity be1 = this.terminal();
                        if (be1 != null) {
                            be1.setHeadingOffsetDeg(v);
                            PacketDistributor.sendToServer(
                                    new com.hzldm.createnavigationterminal.network
                                            .SetHeadingOffsetPayload(this.pos(), v));
                        }
                    } catch (NumberFormatException ignored) {
                        // 输入到一半的中间态，忽略
                    }
                });
                this.headingOffsetBox.visible = false;
                this.headingOffsetBox.active = onPage;
                this.addRenderableWidget(this.headingOffsetBox);
                continue;
            }
            Button left = NavButton.of(Component.literal("\u25c0"),
                    b -> this.cycle(rowId, -1))
                    .bounds(cx + 30 + 156, this.topPos + ry, 16, 14).build();
            Button right = NavButton.of(Component.literal("\u25b6"),
                    b -> this.cycle(rowId, 1))
                    .bounds(cx + 30 + 194, this.topPos + ry, 16, 14).build();
            left.visible = false;
            right.visible = false;
            left.active = onPage;
            right.active = onPage;
            this.addRenderableWidget(left);
            this.addRenderableWidget(right);
            this.rowArrows[(row - 1) * 2] = left;
            this.rowArrows[(row - 1) * 2 + 1] = right;
        }

        // ---- 预设槽 1/2/3 + 编队频道加减 ----
        for (int i = 0; i < 3; ++i) {
            final int slot = i;
            Button pb = NavButton.of(Component.literal(String.valueOf(i + 1)), b -> {
                PacketDistributor.sendToServer(new PresetPayload(this.pos(), 0, slot));
            }).bounds(cx + 30 + 34 + i * 18, this.topPos + 22 + 150, 16, 14).build();
            pb.visible = false;
            this.addRenderableWidget(pb);
            this.chromeButtons.add(pb);
        }
        Button chDown = NavButton.of(Component.literal("\u25c0"), b -> this.cycleChannel(-1))
                .bounds(cx + 30 + 148, this.topPos + 22 + 150, 16, 14).build();
        Button chUp = NavButton.of(Component.literal("\u25b6"), b -> this.cycleChannel(1))
                .bounds(cx + 30 + 164, this.topPos + 22 + 150, 16, 14).build();
        chDown.visible = false;
        chUp.visible = false;
        this.addRenderableWidget(chDown);
        this.addRenderableWidget(chUp);
        this.chromeButtons.add(chDown);
        this.chromeButtons.add(chUp);

        // ---- 候选路点：卡上的 + Xaero 的 ----
        List<WpTarget> cardPts = this.readCardPoints();
        this.candidates = cardPts;
        this.xaeroLoaded = false;
        this.scroll = 0;
        if (this.isPonderScene()) {
            // 思索场景里不读真实 Xaero 数据，换成占位示例
            ArrayList<WpTarget> demo = new ArrayList<>(cardPts);
            demo.add(new WpTarget("\u793a\u4f8b1", -120, 70, -80, this.currentDim()));
            demo.add(new WpTarget("\u793a\u4f8b2", 140, 64, 60, this.currentDim()));
            demo.add(new WpTarget("\u793a\u4f8b3", -40, 80, 180, this.currentDim()));
            this.candidates = demo;
            this.xaeroLoaded = true;
            return;
        }
        XaeroWaypointService.listWaypointsOnClient(list -> {
            ArrayList<WpTarget> merged = new ArrayList<>(cardPts);
            String dim = this.currentDim();
            for (XaeroReflect.RawWp wp : list) {
                merged.add(new WpTarget(wp.name(), wp.x(), wp.y(), wp.z(), dim));
            }
            this.candidates = merged;
            this.xaeroLoaded = true;
        });
    }

    /**
     * 当前是否在思索（Ponder）场景里渲染这个界面——用来屏蔽真实存档数据。
     *
     * <p>三条判据，命中任一即认为在镜像里：
     * <ol>
     *   <li>当前活跃界面不是自己 —— 镜像是由 Ponderer 的界面把我们嵌进去渲染的；</li>
     *   <li>方块实体所在世界不是客户端世界 —— 镜像用的是 Ponder 的沙盒世界；</li>
     *   <li>世界类名里带 ponder（兜底）。</li>
     * </ol>
     */
    /** 思索场景里用的演示航线（不进存档，只在镜像界面渲染） */
    /**
     * 思索场景的演示航线：以玩家当前坐标为基准，就近取三个点。
     *
     * <p>早先写死固定坐标，地图视野是按玩家+航线的联合包围盒算的，
     * 三个点会全部挤到一角互相压标签。改成相对玩家之后，三个点正好围在玩家周围。
     */
    private java.util.List<WpTarget> demoRoute() {
        double bx = 0;
        double bz = 0;
        int by = 70;
        if (this.minecraft != null && this.minecraft.player != null) {
            double px = this.minecraft.player.getX();
            double pz = this.minecraft.player.getZ();
            // Ponder 世界把场景放在离原点很远的位置（实测 ~2900 万），
            // 直接用会让界面显示一串天文数字，超出合理范围就用原点。
            if (Math.abs(px) < 100000.0 && Math.abs(pz) < 100000.0) {
                bx = px;
                bz = pz;
                by = (int) Math.floor(this.minecraft.player.getY());
            }
        }
        int x0 = (int) Math.round(bx);
        int z0 = (int) Math.round(bz);
        return java.util.List.of(
                new WpTarget("示例1", x0 - 40, by,     z0 - 60, "minecraft:overworld"),
                new WpTarget("示例2", x0 + 60, by - 6, z0 + 10, "minecraft:overworld"),
                new WpTarget("示例3", x0 - 10, by + 10, z0 + 70, "minecraft:overworld"));
    }

    /** 航线数据源：思索镜像里用演示航线，正常游戏用真实航线 */
    private java.util.List<WpTarget> routeOf(NavigationTerminalBlockEntity be) {
        return this.isPonderScene() ? this.demoRoute() : be.getRoute();
    }

    /**
     * 类名 → 是否 Ponder 世界的判定缓存。
     *
     * <p>原实现每次调用都做 {@code getClass().getName().toLowerCase(Locale.ROOT)}，
     * 而 isPonderScene() 在渲染路径里被调用 6 次以上/帧，等于每帧十几次字符串分配。
     * 该判定只与 {@link Class} 有关，天然可缓存（用 IdentityHashMap，按类对象身份查）。
     */
    private static final java.util.Map<Class<?>, Boolean> PONDER_CLASS_CACHE =
            new java.util.IdentityHashMap<>(4);

    private static boolean isPonderClass(Class<?> type) {
        Boolean cached = PONDER_CLASS_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        boolean ponder = type.getName().toLowerCase(java.util.Locale.ROOT).contains("ponder");
        PONDER_CLASS_CACHE.put(type, ponder);
        return ponder;
    }

    private boolean isPonderScene() {
        if (this.minecraft == null) {
            return false;
        }
        if (this.minecraft.screen != this) {
            return true;
        }
        NavigationTerminalBlockEntity be = this.terminal();
        if (be != null && be.getLevel() != null) {
            if (be.getLevel() != this.minecraft.level) {
                return true;
            }
            if (isPonderClass(be.getLevel().getClass())) {
                return true;
            }
        }
        return this.minecraft.level != null && isPonderClass(this.minecraft.level.getClass());
    }

    /** 设置面板是否展开（左侧「设置」按钮切换） */
    private boolean showSettings = false;

    /** 切换页签（0=导航 1=参数 2=航线），会重建全部控件。 */
    private void switchPage(int page) {
        if (this.currentPage == page) {
            return;
        }
        this.currentPage = page;
        this.scroll = 0;
        this.menuIdx = -1;
        this.loopButton = null;
        this.rebuildWidgets();
    }

    /** 每 tick 刷新候选路点列表，供导航页与右键菜单使用。 */
    protected void containerTick() {
        super.containerTick();
        if (this.loopButton != null) {
            NavigationTerminalBlockEntity be = this.terminal();
            boolean loop = be == null || be.isRouteLoop();
            this.loopButton.setMessage((Component)Component.translatable((String)(loop ? "create_navigation_terminal.nav.route_loop_on" : "create_navigation_terminal.nav.route_loop_off")));
        }
    }

    /** 参数页：给某一行加「-」「+」按钮。 */
    private void addCycleRow(int cx, int relY, int rowIndex) {
        this.addRenderableWidget(NavButton.of((Component)Component.literal((String)"\u25c0"), b -> this.cycle(rowIndex, -1)).bounds(cx + 10, this.topPos + relY, 18, 18).build());
        this.addRenderableWidget(NavButton.of((Component)Component.literal((String)"\u25b6"), b -> this.cycle(rowIndex, 1)).bounds(cx + 320 - 28, this.topPos + relY, 18, 18).build());
    }

    /**
     * 参数页的加减逻辑。
     *
     * 行号与 BlockEntity 字段一一对应：
     *   1 模式  2 强度  3 覆盖距离  4 到达半径  5 到达后行为
     */
    /** 面名（Index 对应 Direction.ordinal：0=下 1=上 2=北 3=南 4=西 5=东） */
    private String faceName(int idx) {
        return switch (idx) {
            case 0 -> "\u4e0b";
            case 1 -> "\u4e0a";
            case 2 -> "\u5317";
            case 3 -> "\u5357";
            case 4 -> "\u897f";
            case 5 -> "\u4e1c";
            default -> "?";
        };
    }

    /** 设置面板第 6 行：切换正在编辑的面 */
    private void cycleFace(int delta) {
        this.editingFace = Math.floorMod(this.editingFace + delta, 6);
    }

    /** 设置面板第 7 行：切换当前面的信号源 */
    private void cycleFaceSource(int delta) {
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return;
        }
        int cur = be.getFaceSource(this.editingFace);
        int next = Math.floorMod(cur + delta, 8);
        PacketDistributor.sendToServer(new com.hzldm.createnavigationterminal.network
                .SetFaceSourcePayload(this.pos(), this.editingFace, next));
        be.setFaceSource(this.editingFace, next);
    }

    private String driveModeLabel() {
        NavigationTerminalBlockEntity be = this.terminal();
        return be != null
                ? NavigationTerminalBlockEntity.driveModeName(be.getDriveMode())
                : "\u81ea\u52a8";
    }

    /** pidLabel 的三槽缓存：数值没变就直接复用上次的字符串（String.format 每帧 3 次） */
    private final double[] pidCacheValue = {Double.NaN, Double.NaN, Double.NaN};
    private final String[] pidCacheText = new String[3];

    private String pidLabel(int which) {
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return "-";
        }
        int slot = which == 0 ? 0 : (which == 1 ? 1 : 2);
        double v = switch (which) {
            case 0 -> be.getKp();
            case 1 -> be.getKi();
            default -> be.getKd();
        };
        // 用位比较判断"同一个数值"，-0.0 与 0.0 走不同槽位，保证输出与原来逐字一致
        if (Double.doubleToRawLongBits(v) == Double.doubleToRawLongBits(pidCacheValue[slot])
                && pidCacheText[slot] != null) {
            return pidCacheText[slot];
        }
        String text = String.format("%.2f", v);
        pidCacheValue[slot] = v;
        pidCacheText[slot] = text;
        return text;
    }

    /** 缩放倍率标签：倍率不变就不重新 format（原实现每帧一次 String.format） */
    private double zoomLabelValue = Double.NaN;
    private String zoomLabelText = "x1";

    private String mapZoomLabel() {
        if (Double.doubleToRawLongBits(this.mapZoom) == Double.doubleToRawLongBits(this.zoomLabelValue)) {
            return this.zoomLabelText;
        }
        String text = String.format("x%.1f", this.mapZoom);
        this.zoomLabelValue = this.mapZoom;
        this.zoomLabelText = text;
        return text;
    }

    /** 右键菜单三项文案：按当前语言单例缓存，语言切换（实例变化）时自动重建 */
    private net.minecraft.locale.Language menuLabelLang;
    private String[] menuLabelCache;

    private String[] menuLabels() {
        net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();
        if (this.menuLabelCache != null && lang == this.menuLabelLang) {
            return this.menuLabelCache;
        }
        this.menuLabelCache = new String[]{
                Component.translatable("create_navigation_terminal.menu.set_target").getString(),
                Component.translatable("create_navigation_terminal.menu.add_route").getString(),
                Component.translatable("create_navigation_terminal.menu.remove_route").getString()
        };
        this.menuLabelLang = lang;
        return this.menuLabelCache;
    }

    private String headingOffsetLabel() {
        NavigationTerminalBlockEntity be = this.terminal();
        return be == null ? "0\u00b0"
                : NavigationTerminalBlockEntity.headingOffsetName(be.getHeadingOffsetDeg());
    }

    private String steerInvertLabel() {
        NavigationTerminalBlockEntity be = this.terminal();
        return be == null ? "\u5173" : (be.isSteerInverted() ? "\u5f00" : "\u5173");
    }

    private String headingLabel() {
        NavigationTerminalBlockEntity be = this.terminal();
        return be == null ? "\u81ea\u52a8"
                : NavigationTerminalBlockEntity.headingName(be.getHeadingOverride());
    }

    private String driveLockLabel() {
        NavigationTerminalBlockEntity be = this.terminal();
        return be == null ? "\u5f00" : (be.isDriveLocked() ? "\u5f00" : "\u5173");
    }

    private String strategyLabel() {
        NavigationTerminalBlockEntity be = this.terminal();
        return be != null
                ? NavigationTerminalBlockEntity.strategyName(be.getFlightStrategy())
                : "\u8fb9\u98de\u8fb9\u4fee";
    }

    private void cycle(int row, int delta) {
        switch (row) {
            case 1: {
                this.localMode = NavigationTerminalScreen.wrap(this.localMode + delta, 0, 2);
                break;
            }
            case 2: {
                this.localPower = NavigationTerminalScreen.clamp(this.localPower + delta, 1,
                        NavigationTerminalBlockEntity.MAX_POWER);
                break;
            }
            case 3: {
                this.localRange = NavigationTerminalScreen.clamp(this.localRange + delta * 16, 8, 512);
                break;
            }
            case 4: {
                this.localRadius = NavigationTerminalScreen.clamp(this.localRadius + delta, 0, 64);
                break;
            }
            case 12: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.cycleKp(delta);
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .SetPidPayload(this.pos(), 0, be.getKp()));
                }
                break;
            }
            case 13: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.cycleKi(delta);
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .SetPidPayload(this.pos(), 1, be.getKi()));
                }
                break;
            }
            case 14: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.cycleKd(delta);
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .SetPidPayload(this.pos(), 2, be.getKd()));
                }
                break;
            }
            case 11: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.cycleHeadingOffset(delta);
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .CycleHeadingOffsetPayload(this.pos(), delta));
                }
                break;
            }
            case 10: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.toggleSteerInverted();
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .ToggleSteerInvertPayload(this.pos()));
                }
                break;
            }
            case 9: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.cycleHeadingOverride(delta);
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .CycleHeadingPayload(this.pos(), delta));
                }
                break;
            }
            case 8: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.toggleDriveLock();
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .ToggleDriveLockPayload(this.pos()));
                }
                break;
            }
            case 6: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.toggleDriveMode();
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .ToggleDriveModePayload(this.pos()));
                }
                break;
            }
            case 7: {
                NavigationTerminalBlockEntity be = this.terminal();
                if (be != null) {
                    be.toggleFlightStrategy();
                    PacketDistributor.sendToServer(
                            new com.hzldm.createnavigationterminal.network
                                    .ToggleFlightStrategyPayload(this.pos()));
                }
                break;
            }
            case 5: {
                this.localAction = NavigationTerminalScreen.wrap(this.localAction + delta, 0, 2);
                break;
            }
            case 16: {
                this.localSteerScale = NavigationTerminalScreen.clamp(this.localSteerScale + delta * 5, 10, 90);
                break;
            }
            case 15: {
                // 0=关闭；其余按 8 格步进，上限 512
                this.localDecel = NavigationTerminalScreen.clamp(this.localDecel + delta * 8, 0, 512);
                break;
            }
            default: {
                return;
            }
        }
        if (this.localRange <= this.localRadius) {
            this.localRange = Math.min(512, this.localRadius + 16);
        }
        PacketDistributor.sendToServer((CustomPacketPayload)new SetNavigationSettingsPayload(this.pos(), this.localMode, this.localPower, this.localRange, this.localRadius, this.localAction, this.localDecel, this.localSteerScale), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    /** 编队频道加减（0-15 循环，设了即入队）。 */
    private void cycleChannel(int delta) {
        NavigationTerminalBlockEntity be = this.terminal();
        int cur = be != null ? be.getFleetChannel() : 0;
        int next = (cur + delta + 16) % 16;
        PacketDistributor.sendToServer(new SetFleetChannelPayload(this.pos(), next));
    }

    /** 两个 ARGB 颜色按 t(0..1) 线性插值。 */
    private static int lerpColor(int a, int b, float t) {
        float u = Math.max(0f, Math.min(1f, t));
        int aa = (int) (((a >> 24 & 0xFF) * (1 - u)) + ((b >> 24 & 0xFF) * u));
        int ar = (int) (((a >> 16 & 0xFF) * (1 - u)) + ((b >> 16 & 0xFF) * u));
        int ag = (int) (((a >> 8 & 0xFF) * (1 - u)) + ((b >> 8 & 0xFF) * u));
        int ab = (int) (((a & 0xFF) * (1 - u)) + ((b & 0xFF) * u));
        return aa << 24 | ar << 16 | ag << 8 | ab;
    }

    /** 把值夹到 [lo, hi]。 */
    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** 在 [lo, hi] 内循环（越界绕回另一端），用于枚举型参数。 */
    private static int wrap(int v, int lo, int hi) {
        int span = hi - lo + 1;
        int norm = ((v - lo) % span + span) % span;
        return lo + norm;
    }

    /** 读取卡槽里路径点卡上的路点（没卡或没数据返回空表）。 */
    private List<WpTarget> readCardPoints() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return List.of();
        }
        ItemStack stack = this.minecraft.player.getMainHandItem();
        WaypointCardData data = (WaypointCardData)stack.get(ModDataComponents.WAYPOINT_CARD);
        return data == null ? List.of() : data.entries();
    }

    /** 当前维度 id。 */
    private String currentDim() {
        return this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.dimension().location().toString() : "minecraft:overworld";
    }

    /**
     * 背景层：按新版线框图的三栏布局。
     *
     *   +--+--------------+------+--------+
     *   |卡|              | 导航 | 目标点 |
     *   |槽|  航线地图    | 路径 +--------+
     *   |设|              | 点   | 路径点 |
     *   +--+--------------+------+--------+
     *   |        物品栏         [按钮组]  |
     *   +--------------------------------+
     */
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int bx = this.leftPos;
        int by = this.topPos;

        // 机身：投影 + 浅钢灰底 + 深描边 + 白高光
        NavGuiStyle.body(g, bx, by, W, H);

        // ---- 三段式之一：顶部标题栏 ----
        NavGuiStyle.header(g, bx + 1, by + 1, W - 2, 22, "导航终端", this.font);
        g.fill(bx + 1, by + 22, bx + W - 1, by + 23, 0xFF8A8A8A);

        // ---- 三段式之二：内容区 ----
        // 航线地图（带脊线，视觉锚点）
        panelSpine(g, 30, 22, 220, 165);
        // 导航路径点（当前航线）
        panel(g, 256, 22, 74, 165);
        // 目标点
        panel(g, 336, 22, 128, 54);
        // 路径点（自由选择）
        panel(g, 336, 80, 128, 107);
        // 物品栏：**不要黑底**，改成与机身同色的浅底板 + 凹陷槽

        // ---- 三段式之三：底部状态栏分隔线 ----
        g.fill(bx + 1, by + H - 30, bx + W - 1, by + H - 29, 0xFF8A8A8A);

        // 航线卡槽（左栏）
        int slotX = this.leftPos + com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu.CARD_SLOT_X - 1;
        int slotY = this.topPos + com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu.CARD_SLOT_Y - 1;
        NavGuiStyle.slot(g, slotX, slotY);
        if (this.isPonderScene()) {
            // 思索镜像不共享容器，槽位是空的：补画一张示例卡图标
            g.renderItem(new net.minecraft.world.item.ItemStack(
                    com.hzldm.createnavigationterminal.registry.ModItems.WAYPOINT_CARD.get()),
                    slotX + 1, slotY + 1);
        }

        // 玩家背包 27 格 + 快捷栏 9 格：凹陷槽
        int invY = this.topPos
                + com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu.PLAYER_INV_Y;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                NavGuiStyle.slot(g, this.leftPos + 59 + col * 18 - 1, invY + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; ++col) {
            NavGuiStyle.slot(g, this.leftPos + 59 + col * 18 - 1, invY + 58 - 1);
        }
    }

    /**
     * 覆盖层：在所有槽位与控件之上重画设置面板。
     *
     * 必须放在 render() 的最后调用，否则背包物品会盖在面板上。
     */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 记下鼠标位置，供列表行做悬停高亮（render 与 renderLabels 同一帧）
        this.hoverGuiX = mouseX - this.leftPos;
        this.hoverGuiY = mouseY - this.topPos;
        // 设置面板打开时，设置按钮变绿（开关态反馈）
        if (this.settingsBtn != null) {
            this.settingsBtn.withToggled(this.showSettings);
        }
        boolean hasCard = this.currentCard() != null;
        if (this.renameBtn != null) {
            this.renameBtn.visible = hasCard;
            this.renameBtn.active = hasCard;
        }
        if (this.readOnlyBtn != null) {
            this.readOnlyBtn.visible = hasCard;
            this.readOnlyBtn.active = hasCard;
            this.readOnlyBtn.withToggled(hasCard && this.currentCardReadOnly());
        }
        // 可见性：常驻按钮跟随面板开关；参数行箭头按「本页是否包含该行」逐个决定
        // 设置面板里的分页/预设/频道按钮改由 drawSettingsOverlay 自绘 + mouseClicked 处理，
        // 这里统一隐藏，避免"控件被覆盖层挡住却又隐约透出"的错位观感
        for (Button b : this.chromeButtons) {
            if (b != null) {
                b.visible = false;
            }
        }
        for (int row = 1; row <= ROW_COUNT; row++) {
            boolean vis = this.showSettings && this.rowOnPage(row);
            for (int side = 0; side < 2; side++) {
                Button b = this.rowArrows[(row - 1) * 2 + side];
                if (b != null) {
                    b.visible = false;      // 同上：由覆盖层自绘
                    b.active = vis;
                }
            }
        }
        if (this.headingOffsetBox != null) {
            int slot = this.slotOfRow(this.settingsPage, ROW_HEADING_OFFSET);
            boolean vis = this.showSettings && slot >= 0;
            this.headingOffsetBox.visible = vis;
            this.headingOffsetBox.active = vis;
            if (vis) {
                this.headingOffsetBox.setX(this.leftPos + CFG_X + 74);
                this.headingOffsetBox.setY(this.topPos + CFG_Y + CFG_ROW_Y0 + slot * CFG_ROW_DY + 1);
            }
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (this.showSettings) {
            this.drawSettingsOverlay(g);
        }
        // 导航卡槽的悬浮提示：空槽时说明用法，有物品时交给原版 tooltip
        if (!this.showSettings
                && NavGuiStyle.inside(this.hoverGuiX, this.hoverGuiY,
                        com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu.CARD_SLOT_X,
                        com.hzldm.createnavigationterminal.menu.NavigationTerminalMenu.CARD_SLOT_Y, 18, 18)
                && !this.menu.slots.isEmpty()
                && this.menu.slots.get(0).getItem().isEmpty()) {
            g.renderTooltip(this.font,
                    java.util.List.of(
                            net.minecraft.network.chat.Component.literal("导航卡").getVisualOrderText(),
                            net.minecraft.network.chat.Component.literal(
                                    "放入导航点卡：用左侧的 写入 / 读取 保存与加载航线")
                                    .getVisualOrderText()),
                    mouseX, mouseY);
        }
    }

    /** 设置面板（绝对坐标，覆盖物品栏区域）——所有控件都在这里自绘 */
    private void drawSettingsOverlay(GuiGraphics g) {
        int x0 = this.leftPos + CFG_X;
        int y0 = this.topPos + CFG_Y;
        int w = CFG_W;
        int h = CFG_H;

        NavGuiStyle.insetPanel(g, x0, y0, w, h);

        // 标题栏：只写页名，右侧一个翻页箭头（原来和分页按钮的文字叠在一起，糊成一团）
        NavGuiStyle.header(g, x0 + 1, y0 + 1, w - 2, 15, CFG_PAGE_NAMES[this.settingsPage], this.font);
        int pageBtnX = x0 + w - 18;
        NavGuiStyle.raisedPanel(g, pageBtnX, y0 + 3, 14, 11,
                isHover(pageBtnX, y0 + 3, 14, 11) ? NavGuiStyle.PANEL_HOVER : NavGuiStyle.PANEL_ALT);
        drawTriangle(g, pageBtnX + 5, y0 + 8, false, NavGuiStyle.ACCENT);

        int[] rows = PAGE_ROWS[this.settingsPage];
        NavigationTerminalBlockEntity beCfg = this.terminal();
        for (int slot = 0; slot < rows.length; slot++) {
            int rowId = rows[slot];
            if (rowId < 1 || rowId > ROW_COUNT) {
                continue;
            }
            SettingRow row = this.settingRows[rowId];
            if (row == null) {
                continue;
            }
            int ry = y0 + CFG_ROW_Y0 + slot * CFG_ROW_DY;
            g.drawString(this.font, row.label(), x0 + 8, ry + 4, NavGuiStyle.MUTED, false);
            String value = row.value() == null ? "" : row.value().get();
            if (value != null && !value.isEmpty()) {
                g.drawCenteredString(this.font, value, x0 + CFG_VAL_CX, ry + 4, NavGuiStyle.ACCENT);
            }
            if (rowId != ROW_HEADING_OFFSET) {
                int lx = x0 + CFG_ARROW_LX;
                int rx = x0 + CFG_ARROW_RX;
                drawTriangleButton(g, lx, ry, true);
                drawTriangleButton(g, rx, ry, false);
            }
        }

        // 底栏：预设 1/2/3 + 频道加减
        int fy = y0 + h - 18;
        g.fill(x0 + 4, fy - 3, x0 + w - 4, fy - 2, NavGuiStyle.GRID);
        g.drawString(this.font, "预设", x0 + 6, fy + 4, NavGuiStyle.MUTED, false);
        for (int i = 0; i < 3; i++) {
            int bx = x0 + 30 + i * 18;
            boolean sel = this.lastPresetSlot == i;
            NavGuiStyle.raisedPanel(g, bx, fy, 16, 15,
                    sel ? NavGuiStyle.WASH_CURRENT
                        : isHover(bx, fy, 16, 15) ? NavGuiStyle.PANEL_HOVER : NavGuiStyle.PANEL_ALT);
            g.drawCenteredString(this.font, String.valueOf(i + 1), bx + 8, fy + 4,
                    sel ? NavGuiStyle.ACCENT : NavGuiStyle.TEXT);
        }
        NavigationTerminalBlockEntity tb = this.terminal();
        int ch = tb != null ? tb.getFleetChannel() : 0;
        g.drawString(this.font, "频道 " + (ch < 0 ? "独占" : ch),
                x0 + 110, fy + 4, NavGuiStyle.MUTED, false);
        drawTriangleButton(g, x0 + w - 40, fy, true);
        drawTriangleButton(g, x0 + w - 22, fy, false);
    }

    // ---- 设置面板自绘控件的小工具 ----
    private boolean isHover(int x, int y, int w, int h) {
        return NavGuiStyle.inside(this.hoverGuiX, this.hoverGuiY, x, y, w, h);
    }

    /** 实心三角（4 行阶梯），比字体里的 ◀ ▶ 清楚得多 */
    private void drawTriangle(GuiGraphics g, int cx, int cy, boolean left, int color) {
        // 第 i 列越高越靠外：left 时高列在右 → 尖在左；right 时高列在左 → 尖在右
        for (int i = 0; i < 4; i++) {
            if (left) {
                g.fill(cx + i, cy - i, cx + i + 1, cy + i + 1, color);
            } else {
                g.fill(cx - i, cy - i, cx - i + 1, cy + i + 1, color);
            }
        }
    }

    private void drawTriangleButton(GuiGraphics g, int x, int y, boolean left) {
        boolean hover = isHover(x, y, 16, 15);
        NavGuiStyle.raisedPanel(g, x, y, 16, 15,
                hover ? NavGuiStyle.PANEL_HOVER : NavGuiStyle.PANEL_ALT);
        drawTriangle(g, x + 8, y + 7, left, NavGuiStyle.ACCENT);
    }

    /** 画一个分区底板：深灰内嵌 + 上/左内阴影 + 下/右亮边 + 竖向细纹 */
    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        NavGuiStyle.insetPanel(g, this.leftPos + x, this.topPos + y, w, h, false);
    }

    /** 带左侧铜橙脊线的分区底板（呼应时刻表中间那条时刻线） */
    private void panelSpine(GuiGraphics g, int x, int y, int w, int h) {
        NavGuiStyle.insetPanel(g, this.leftPos + x, this.topPos + y, w, h, true);
    }

    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // 左栏不放文字：导航卡槽的说明走悬浮提示

        // 各分区内容
        this.renderChannelPanel(g);
        this.renderMapPanel(g);
        // 地图相框必须画在贴图之后：drawTerrain 的 blit 会把这个区域整个盖住，
        // 画在前面等于没画（这就是"边框没显示"的原因）。
        if (!this.showSettings) {
            NavGuiStyle.frame(g, 30, 22, 220, 165, NavGuiStyle.GRID);
            NavGuiStyle.frame(g, 31, 23, 218, 163, NavGuiStyle.INNER);
        }
        this.renderRoutePanel(g);
        this.renderTargetPanel(g);
        this.renderCandidatePanel(g);

        this.drawContextMenu(g);
    }



    private String modeName(int m) {
        return switch (m) {
            case 1 -> "\u5230\u8fbe\u5f00\u5173";
            case 2 -> "\u56fa\u5b9a\u5f3a\u5ea6";
            default -> "\u8ddd\u79bb\u6e10\u53d8";
        };
    }

    /**
     * 到达动作的显示名。**必须与服务端常量对齐**：
     * 0 = ARRIVE_HOLD（保持）、1 = ARRIVE_SILENT（静默）、2 = ARRIVE_CLEAR（清除）。
     *
     * <p>1.1.22 之前这里把 1 写成"清除目标"、2 写成"暂停"，与后端正好错位 ——
     * 玩家选"清除目标"实际发的是 1（静默），所以「到达后自动清除」永远不生效，
     * 日志里会看到 `[Arrive] 动作=1 → 锁存并静默`。
     */
    private String actionName(int a) {
        return switch (a) {
            case 1 -> "\u9759\u9ed8";
            case 2 -> "\u6e05\u9664\u76ee\u6807";
            default -> "\u4fdd\u6301";
        };
    }

    /** 航线地图（中央大区） */
    /**
     * 编队频道面板：右上按钮组下方那块空位。
     *
     * 显示当前频道号、是否在编队、同频友机数。
     */
    private void renderChannelPanel(GuiGraphics g) {
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return;
        }
        int x0 = 256;
        int y0 = 252;
        int w = 208;   // 全宽：CH / 在队 / 同频友机 三列都要放得下
        int h = 34;

        // 编队频道条：与其它分区同一套米色内嵌皮
        NavGuiStyle.insetPanel(g, x0, y0, w, h);

        boolean inFleet = this.isPonderScene() || be.isInFleet();
        int ch = this.isPonderScene() ? 3 : be.getFleetChannel();

        g.drawString(this.font, "\u7f16\u961f\u9891\u9053", x0 + 6, y0 + 5, NavGuiStyle.MUTED, false);
        String chText = ch < 0 ? "\u72ec\u5360" : ("CH " + ch);
        g.drawString(this.font, chText, x0 + 6, y0 + 17,
                ch < 0 ? NavGuiStyle.AMBER : (inFleet ? NavGuiStyle.GREEN : NavGuiStyle.MUTED), false);

        int dotColor = inFleet ? NavGuiStyle.GREEN : NavGuiStyle.GRID;
        g.fill(x0 + 52, y0 + 21, x0 + 58, y0 + 27, dotColor);
        g.drawString(this.font, inFleet ? "\u5728\u7f16\u961f" : "\u672a\u5165\u961f",
                x0 + 62, y0 + 17, dotColor, false);

        com.hzldm.createnavigationterminal.network.NavigationHudPayload data =
                NavigationHudOverlay.getCurrent();
        // 服务端按 FleetRegistry 算出的同频道终端总数（不受绑定座椅/距离限制）
        int mates = this.isPonderScene() ? 2 : (data != null ? data.fleetPeerCount() : 0);
        g.drawString(this.font, "\u540c\u9891\u53cb\u673a " + mates,
                x0 + 140, y0 + 17, NavGuiStyle.MUTED, false);
    }

    private void renderMapPanel(GuiGraphics g) {
        // 设置面板展开时盖住地图区，这里的文字与地图一并跳过
        if (this.showSettings) {
            return;
        }
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return;
        }
        if (dbgMapPanel < 3) {
            dbgMapPanel++;
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Map] renderMapPanel route={} tex={}", be.getRoute().size(),
                    this.mapTexture == null ? "null" : "ok");
        }
        if (this.isPonderScene() || be.hasTarget() || !be.getRoute().isEmpty()) {
            this.drawRouteMap(g, be);
        }
    }

    /** 当前航线列表（中右栏） */
    private void renderRoutePanel(GuiGraphics g) {
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return;
        }
        NavGuiStyle.sectionTitle(g, this.font, 258, 28, 70, "\u5bfc\u822a\u8def\u5f84\u70b9");
        g.drawCenteredString(this.font, "(\u5f53\u524d\u822a\u7ebf)", 293, 41, NavGuiStyle.MUTED);
        List<WpTarget> route = this.routeOf(be);
        if (route.isEmpty()) {
            // 没有多段航线时，把当前目标当作唯一航点
            if (be.hasTarget()) {
                g.drawString(this.font, this.trim(be.getTargetName(), 8), 260, 58, -16711936, false);
            } else {
                g.drawString(this.font, "\u822a\u7ebf\u4e3a\u7a7a", 260, 58, NavGuiStyle.MUTED, false);
            }
            return;
        }
        int y = 58;
        int curIdx = be.getRouteIndex();
        // 呼吸高亮：当前站的颜色在亮绿 / 暗绿之间脉动
        float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 260.0));
        int curColor = lerpColor(0xFF20FF50, 0xFF0A6E22, pulse);
        int visible = 8;
        int maxScroll = Math.max(0, route.size() - visible);
        // 让当前站尽量留在可视范围
        if (curIdx < this.routeScroll) {
            this.routeScroll = curIdx;
        } else if (curIdx >= this.routeScroll + visible) {
            this.routeScroll = curIdx - visible + 1;
        }
        this.routeScroll = Math.max(0, Math.min(maxScroll, this.routeScroll));
        for (int row = 0; row < visible; ++row) {
            int i = this.routeScroll + row;
            if (i >= route.size()) {
                break;
            }
            WpTarget wp = route.get(i);
            int ry = y + row * 18;
            if (i == curIdx) {
                // 当前站：左侧三角指示 + 呼吸色 + 高亮底
                g.fill(256, ry - 1, 331, ry + 10, 0x3320FF50);
                g.drawString(this.font, "\u25b6", 259, ry, curColor, false);
                g.drawString(this.font, this.trim(wp.name(), 7), 269, ry, curColor, false);
            } else {
                boolean hovered = NavGuiStyle.inside(this.hoverGuiX, this.hoverGuiY, 256, ry - 2, 76, 14);
                if (hovered) {
                    g.fill(256, ry - 2, 332, ry + 12, NavGuiStyle.PANEL_HOVER);
                    g.fill(256, ry - 2, 258, ry + 12, NavGuiStyle.ACCENT);
                }
                g.drawString(this.font, this.trim(wp.name(), 8), 269, ry, NavGuiStyle.TEXT, false);
            }
            // 拖拽中：正在拖的行加橙色底，落点画一条插入线
            if (this.dragFrom >= 0) {
                if (i == this.dragFrom) {
                    g.fill(256, ry - 1, 332, ry + 10, 0x66FF8C1A);
                    g.drawString(this.font, "=", 256, ry, NavGuiStyle.AMBER, false);
                }
                if (i == this.dragTo) {
                    boolean below = this.dragTo > this.dragFrom;
                    int lineY = below ? ry + 10 : ry - 1;
                    g.fill(256, lineY, 332, lineY + 2, NavGuiStyle.AMBER);
                }
            }
        }
        // 超出可视范围时显示当前位置
        if (route.size() > visible) {
            String hint = (this.routeScroll + 1) + "-"
                    + Math.min(route.size(), this.routeScroll + visible)
                    + "/" + route.size();
            g.drawString(this.font, hint, 296, y + visible * 18 + 2, NavGuiStyle.MUTED, false);
            // 右侧滚动条
            int barX = 331;
            int barTop = y;
            int barH = visible * 18;
            g.fill(barX, barTop, barX + 2, barTop + barH, NavGuiStyle.INNER);
            int thumbH = Math.max(8, barH * visible / route.size());
            int thumbY = barTop + (barH - thumbH) * this.routeScroll / Math.max(1, maxScroll);
            g.fill(barX, thumbY, barX + 2, thumbY + thumbH, NavGuiStyle.ACCENT);
        }
    }

    /** 目标点信息（右上栏） */
    private void renderTargetPanel(GuiGraphics g) {
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return;
        }
        if (this.isPonderScene()) {
            // 思索镜像：固定演示目标
            WpTarget first = this.demoRoute().get(0);
            g.drawCenteredString(this.font, first.name(), 400, 30, -1);
            g.drawCenteredString(this.font,
                    "(" + first.x() + ", " + first.y() + ", " + first.z() + ")", 400, 46,
                    NavGuiStyle.MUTED);
            g.drawCenteredString(this.font, "72 格", 400, 62, NavGuiStyle.ACCENT);
            return;
        }
        if (!be.hasTarget()) {
            g.drawCenteredString(this.font, "\u672a\u8bbe\u7f6e\u76ee\u6807", 400, 46, NavGuiStyle.MUTED);
            return;
        }
        g.drawCenteredString(this.font, this.trim(be.getTargetName(), 10), 400, 30, -1);
        g.drawCenteredString(this.font,
                "(" + be.getTargetX() + ", " + be.getTargetY() + ", " + be.getTargetZ() + ")",
                400, 46, NavGuiStyle.MUTED);
        int dist = be.getSyncedDistance();
        g.drawCenteredString(this.font,
                dist >= 0 ? dist + " \u683c" : "\u4e0d\u540c\u7ef4\u5ea6",
                400, 62, NavGuiStyle.ACCENT);
    }

    /** 路径点（自由选择，右下栏） */
    private void renderCandidatePanel(GuiGraphics g) {
        NavGuiStyle.sectionTitle(g, this.font, 338, 86, 124, "\u8def\u5f84\u70b9");
        int y = 106;
        int vis = CANDIDATE_VISIBLE;
        int maxScroll = Math.max(0, this.candidates.size() - vis);
        this.scroll = Math.max(0, Math.min(maxScroll, this.scroll));
        for (int row = 0; row < vis; ++row) {
            int idx = this.scroll + row;
            if (idx >= this.candidates.size()) {
                break;
            }
            WpTarget wp = this.candidates.get(idx);
            int rowY = y + row * 15;
            boolean hovered = NavGuiStyle.inside(this.hoverGuiX, this.hoverGuiY, 338, rowY - 3, 126, 14);
            if (hovered) {
                g.fill(338, rowY - 3, 464, rowY + 11, NavGuiStyle.PANEL_HOVER);
                g.fill(338, rowY - 3, 340, rowY + 11, NavGuiStyle.ACCENT);
            }
            g.drawString(this.font, this.trim(wp.name(), 11), 342, rowY,
                    hovered ? NavGuiStyle.TEXT : NavGuiStyle.TEXT, false);
        }
        // 滚动条 + 进度
        if (this.candidates.size() > vis) {
            int barX = 462;
            int barTop = y - 2;
            int barH = vis * 15 + 4;
            g.fill(barX, barTop, barX + 2, barTop + barH, NavGuiStyle.INNER);
            int thumbH = Math.max(8, barH * vis / this.candidates.size());
            int thumbY = barTop + (barH - thumbH) * this.scroll / Math.max(1, maxScroll);
            g.fill(barX, thumbY, barX + 2, thumbY + thumbH, NavGuiStyle.ACCENT);
            g.drawString(this.font, (this.scroll + 1) + "/" + this.candidates.size(),
                    344, 158, NavGuiStyle.MUTED, false);
        }
        // 提示放在候选面板底部（原来在 190，正好压在「循环 / 清除目标」那排按钮上）
        NavGuiStyle.divider(g, 340, 168, 120);
        g.drawString(this.font, "\u5de6\u952e\u8bbe\u76ee\u6807 \u00b7 \u53f3\u952e\u52a0\u822a\u7ebf",
                340, 172, NavGuiStyle.MUTED, false);
    }

    /** 导航页：目标名 / 坐标 / 状态行 / 候选列表。 */
    private void renderNavPage(GuiGraphics g) {
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return;
        }
        if (!be.hasTarget()) {
            g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.no_target"), 160, 48, NavGuiStyle.MUTED);
        } else {
            g.drawCenteredString(this.font, (Component)Component.literal((String)be.getTargetName()), 160, 46, -1);
            g.drawCenteredString(this.font, (Component)Component.literal((String)("(" + be.getTargetX() + ", " + be.getTargetY() + ", " + be.getTargetZ() + ")")), 160, 60, NavGuiStyle.MUTED);
            this.drawStatusLine(g, be);
        }
        this.drawCandidateList(g);
    }

    /**
     * 状态行：距离 + 进度条 + 高度差。
     *
     * 跨维度时不显示距离（改显示「不同维度」/「目标在天界」），
     * 同维度优先用服务端同步的距离，没有再用客户端本地算的。
     */
    private void drawStatusLine(GuiGraphics g, NavigationTerminalBlockEntity be) {
        double dist;
        boolean sameDim = this.isSameDimension(be);
        int synced = be.getSyncedDistance();
        if (sameDim) {
            dist = synced >= 0 ? (double) synced : this.clientDistance(be);
        } else {
            dist = -1.0;
        }
        String distText = dist < 0.0
                ? (be.isCrossDimension()
                        ? Component.translatable("create_navigation_terminal.nav.cross_dim",
                                NavigationTerminalScreen.shortDim(be.getTargetDimension())).getString()
                        : Component.translatable("create_navigation_terminal.nav.other_dimension").getString())
                : Component.translatable("create_navigation_terminal.nav.distance",
                        (int) Math.round(dist)).getString();
        int barW = 150;
        int barLeft = 308 - barW;
        int maxDistWidth = barLeft - 26;
        if (this.font.width((String)distText) > maxDistWidth) {
            while (!((String)distText).isEmpty() && this.font.width((String)distText) > maxDistWidth) {
                distText = ((String)distText).substring(0, ((String)distText).length() - 1);
            }
            distText = (String)distText + "\u2026";
        }
        g.drawString(this.font, (String)distText, 12, 76, NavGuiStyle.MUTED, false);
        int power = this.previewPower(dist, be.isArrived());
        int filled = (int)Math.round((double)power / 15.0 * (double)barW);
        g.fill(barLeft, 76, barLeft + barW, 84, -15395563);
        if (filled > 0) {
            g.fill(barLeft, 76, barLeft + filled, 84, power >= 15 ? -8323200 : -2056128);
        }
        g.drawString(this.font, String.valueOf(power), barLeft - 16, 76, NavGuiStyle.TEXT, false);
        if (be.isArrived()) {
            g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.arrived"), 160, 88, -8323200);
        } else if (dist >= 0.0) {
            int dy = be.getHeightDelta();
            g.drawString(this.font, "\u0394Y " + (dy >= 0 ? "+" : "") + dy, 12, 88, NavGuiStyle.MUTED, false);
        }
    }

    /** 候选路点列表：每行显示名字与坐标，左键设为目标、右键加入航线。 */
    private void drawCandidateList(GuiGraphics g) {
        if (this.candidates.isEmpty() && !this.xaeroLoaded) {
            g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.loading"), 160, 142, NavGuiStyle.MUTED);
            return;
        }
        if (this.candidates.isEmpty()) {
            g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.no_candidates"), 160, 132, NavGuiStyle.ACCENT);
            g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.no_candidates_hint"), 160, 150, NavGuiStyle.MUTED);
            return;
        }
        int end = Math.min(this.candidates.size(), this.scroll + 8);
        for (int i = this.scroll; i < end; ++i) {
            WpTarget t = this.candidates.get(i);
            int y = 102 + (i - this.scroll) * 16;
            g.drawString(this.font, NavigationTerminalScreen.trim(t.name(), 26), 16, y + 4, NavGuiStyle.TEXT, false);
            String coord = t.x() + ", " + t.y() + ", " + t.z();
            int w = this.font.width(coord);
            g.drawString(this.font, coord, 310 - w - 6, y + 4, NavGuiStyle.MUTED, false);
        }
        g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.hint_click"), 160, 384, NavGuiStyle.MUTED);
    }

    /** 参数页：五个可调参数 + 预设槽 + 编队频道输入框。 */
    private void renderParamPage(GuiGraphics g) {
        this.drawParam(g, 56, (Component)Component.translatable((String)"create_navigation_terminal.nav.param.mode"), (Component)Component.translatable((String)MODE_NAMES[this.localMode]), NavGuiStyle.ACCENT);
        this.drawParam(g, 82, (Component)Component.translatable((String)"create_navigation_terminal.nav.param.power"), (Component)Component.literal((String)String.valueOf(this.localPower)), NavGuiStyle.ACCENT_BRIGHT);
        this.drawParam(g, 108, (Component)Component.translatable((String)"create_navigation_terminal.nav.param.range"), (Component)Component.translatable((String)"create_navigation_terminal.nav.unit_blocks", (Object[])new Object[]{this.localRange}), NavGuiStyle.ACCENT_BRIGHT);
        this.drawParam(g, 134, (Component)Component.translatable((String)"create_navigation_terminal.nav.param.radius"), (Component)Component.translatable((String)"create_navigation_terminal.nav.unit_blocks", (Object[])new Object[]{this.localRadius}), NavGuiStyle.ACCENT_BRIGHT);
        this.drawParam(g, 160, (Component)Component.translatable((String)"create_navigation_terminal.nav.param.arrive"), (Component)Component.translatable((String)ACTION_NAMES[this.localAction]), -32608);
        g.drawString(this.font, Component.translatable((String)"create_navigation_terminal.nav.preset").getString(), 12, 202, -5197648, false);
        NavigationTerminalBlockEntity beCh = this.terminal();
        g.drawString(this.font, "\u7f16\u961f\u9891\u9053", 12, 224, -5197648, false);
        if (beCh != null) {
            g.drawString(this.font, "0-15\uff0c\u540c\u9891\u9053\u624d\u663e\u793a\u4e3a\u7f16\u961f", 180, 224, -9408400, false);
        }
    }

    /** 画一行「标签 值」。 */
    private void drawParam(GuiGraphics g, int y, Component label, Component value, int valueColor) {
        MutableComponent line = label.copy().append(": ").append(value);
        g.drawCenteredString(this.font, (Component)line, 160, y + 5, valueColor);
    }

    /** 航线页：左侧航点列表（可拖拽排序）+ 右侧地图预览。 */
    private void renderRoutePage(GuiGraphics g) {
        NavigationTerminalBlockEntity be = this.terminal();
        if (be == null) {
            return;
        }
        this.drawRouteMap(g, be);
        List<WpTarget> route = this.routeOf(be);
        if (route.isEmpty()) {
            g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.route_empty"), 160, 192, NavGuiStyle.ACCENT);
            return;
        }
        int cur = be.getRouteIndex();
        int end = Math.min(route.size(), 4 + this.scroll);
        for (int i = this.scroll; i < end; ++i) {
            boolean isCur;
            WpTarget t = (WpTarget)route.get(i);
            int y = 188 + (i - this.scroll) * 16;
            boolean bl = isCur = i == cur;
            if (isCur) {
                g.fill(10, y, 310, y + 16, -11900374);
            }
            String prefix = isCur ? "\u25b6 " : "  ";
            g.drawString(this.font, prefix + (i + 1) + ". " + NavigationTerminalScreen.trim(t.name(), 22), 15, y + 4, NavGuiStyle.TEXT, false);
            String coord = t.x() + ", " + t.z();
            int w = this.font.width(coord);
            g.drawString(this.font, coord, 310 - w - 6, y + 4, NavGuiStyle.MUTED, false);
        }
        g.drawCenteredString(this.font, (Component)Component.translatable((String)"create_navigation_terminal.nav.route_status", (Object[])new Object[]{cur + 1, route.size(), be.isRouteLoop() ? Component.translatable((String)"create_navigation_terminal.nav.route_loop_on").getString() : Component.translatable((String)"create_navigation_terminal.nav.route_loop_off").getString()}), 160, 250, NavGuiStyle.ACCENT);
        if (this.dragFrom >= 0 && this.dragTo >= 0) {
            int lineY = 188 + (this.dragTo - this.scroll) * 16;
            if (this.dragTo > this.dragFrom) {
                lineY += 16;
            }
            if (lineY >= 172 && lineY <= 252) {
                g.fill(10, lineY - 1, 310, lineY + 1, -22016);
            }
            int fromY = 188 + (this.dragFrom - this.scroll) * 16;
            if (this.dragFrom >= this.scroll && this.dragFrom < this.scroll + 4) {
                g.fill(10, fromY, 310, fromY + 16, 1090497024);
            }
        }
    }

    /**
     * 航线地图预览：把整条航线的范围投影到右侧面板，画连线与站点。
     *
     * 视野取 X/Z 中较大的跨度并等比缩放，否则长条形航线会把地形横向拉伸。
     */
    private void drawRouteMap(GuiGraphics g, NavigationTerminalBlockEntity be) {
        int i;
        List<WpTarget> route = this.routeOf(be);
        // 只有「既没航线也没目标」才什么都不画；
        // 单设目标时 route 为空，但目标标记仍然要画出来
        if (!this.isPonderScene() && route.isEmpty() && !be.hasTarget()) {
            return;
        }
        double minX = Double.MAX_VALUE;
        double maxX = -1.7976931348623157E308;
        double minZ = Double.MAX_VALUE;
        double maxZ = -1.7976931348623157E308;
        for (WpTarget t : route) {
            minX = Math.min(minX, (double)t.x());
            maxX = Math.max(maxX, (double)t.x());
            minZ = Math.min(minZ, (double)t.z());
            maxZ = Math.max(maxZ, (double)t.z());
        }
        // 单独设置的目标（左键/K 键）不在航线里，也要纳入视野
        boolean targetInRoute = false;
        if (be.hasTarget()) {
            minX = Math.min(minX, (double) be.getTargetX());
            maxX = Math.max(maxX, (double) be.getTargetX());
            minZ = Math.min(minZ, (double) be.getTargetZ());
            maxZ = Math.max(maxZ, (double) be.getTargetZ());
            for (WpTarget w : route) {
                if (w.x() == be.getTargetX() && w.z() == be.getTargetZ()) {
                    targetInRoute = true;
                    break;
                }
            }
        }
        // 思索镜像里玩家在 2900 万格之外，直接参与包围盒会把示例点全挤到角落，
        // 所以镜像里按原点算（演示点就在原点附近）。
        boolean pondering = this.isPonderScene();
        double px = (!pondering && this.minecraft != null && this.minecraft.player != null)
                ? this.minecraft.player.getX() : 0.0;
        double pz = (!pondering && this.minecraft != null && this.minecraft.player != null)
                ? this.minecraft.player.getZ() : 0.0;
        minX = Math.min(minX, px);
        maxX = Math.max(maxX, px);
        minZ = Math.min(minZ, pz);
        maxZ = Math.max(maxZ, pz);
        // 只有单个站点、且玩家就在附近时，包围盒会接近 0，
        // 视野缩到 1 格就什么都画不出来了。这里强制一个最小跨度。
        // 先按实际范围留 12% 边距，再把最小跨度兜住 256 格：
        // 否则最远的那个点会正好贴在面板边缘，被 scissor 裁掉看不见
        double rawSpanX = Math.max(1.0, maxX - minX);
        double rawSpanZ = Math.max(1.0, maxZ - minZ);
        double padX = rawSpanX * 0.12;
        double padZ = rawSpanZ * 0.12;
        minX -= padX;
        maxX += padX;
        minZ -= padZ;
        maxZ += padZ;
        double spanX0 = Math.max(256.0, maxX - minX);
        double spanZ0 = Math.max(256.0, maxZ - minZ);
        double perBlock = Math.max(spanX0 / 220.0, spanZ0 / 165.0) / Math.max(1.0, this.mapZoom);
        double viewW = perBlock * 220.0;
        double viewH = perBlock * 165.0;
        this.lastSpanX = viewW;
        this.lastSpanZ = viewH;
        double cx = (minX + maxX) / 2.0 + this.mapPanX;
        double cz = (minZ + maxZ) / 2.0 + this.mapPanY;
        double vMinX = cx - viewW / 2.0;
        double vMinZ = cz - viewH / 2.0;
        this.refreshTerrainIfNeeded(vMinX, vMinX + viewW, vMinZ, vMinZ + viewH);
        // 裁剪必须先开：拖动时会用 pose 平移贴图，不裁剪就会溢出到别的面板上
        g.enableScissor(this.leftPos + 30, this.topPos + 22,
                this.leftPos + 30 + 220, this.topPos + 22 + 165);
        this.drawTerrain(g);
        double scale = 220.0 / viewW;
        int[] xs = new int[route.size()];
        int[] ys = new int[route.size()];
        for (i = 0; i < route.size(); ++i) {
            WpTarget t = (WpTarget)route.get(i);
            xs[i] = (int)Math.round(30.0 + ((double)t.x() - vMinX) * scale);
            ys[i] = (int)Math.round(22.0 + ((double)t.z() - vMinZ) * scale);
        }
        if (dbgMapDraw < 4) {
            dbgMapDraw++;
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[Map] route={} hasTarget={} tgt=({},{}) view=[{},{}]x[{},{}] player=({},{})",
                    route.size(), be.hasTarget(), be.getTargetX(), be.getTargetZ(),
                    (int) vMinX, (int) (vMinX + viewW),
                    (int) vMinZ, (int) (vMinZ + viewH), (int) px, (int) pz);
            for (int k = 0; k < route.size(); k++) {
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                        "[Map]   site[{}] world=({},{}) screen=({},{}) targetInRoute={}",
                        k, route.get(k).x(), route.get(k).z(), xs[k], ys[k], targetInRoute);
            }
        }
        i = 0;
        while (i + 1 < route.size()) {
            NavigationTerminalScreen.drawLine(g, xs[i], ys[i], xs[i + 1], ys[i + 1], -10444576);
            ++i;
        }
        if (be.isRouteLoop() && route.size() > 1) {
            NavigationTerminalScreen.drawLine(g, xs[route.size() - 1], ys[route.size() - 1], xs[0], ys[0], -6258496);
        }
        // ① 其他路点（不在航线里的）—— 灰白小方点 + 名字
        for (WpTarget c : this.candidates) {
            if (be.isInRoute(c.x(), c.y(), c.z())) {
                continue;
            }
            // 当前目标已经单独画了橙色准星，这里跳过以免两套名字叠在一起
            if (be.hasTarget()
                    && c.x() == be.getTargetX() && c.z() == be.getTargetZ()) {
                continue;
            }
            int ox = (int) Math.round(30.0 + ((double) c.x() - vMinX) * scale);
            int oy = (int) Math.round(22.0 + ((double) c.z() - vMinZ) * scale);
            if (ox < 32 || ox > 246 || oy < 24 || oy > 184) {
                continue;
            }
            g.fill(ox - 1, oy - 1, ox + 2, oy + 2, 0xFF202020);
            g.fill(ox, oy, ox + 1, oy + 1, 0xFFB0B8C0);
            String nm = this.trim(c.name(), 5);
            if (this.font.width(nm) > 0) {
                g.fill(ox + 2, oy - 9, ox + 6 + this.font.width(nm), oy + 1, 0xA0000000);
            }
            g.drawString(this.font, nm, ox + 4, oy - 8, 0xFFC0C8D0, false);
        }

        // ② 航线站点 —— 当前站绿色高亮，其余黄色；都带序号与名字
        for (i = 0; i < route.size(); ++i) {
            int c = i == be.getRouteIndex() ? -8323200 : NavGuiStyle.ACCENT_BRIGHT;
            g.fill(xs[i] - 3, ys[i] - 3, xs[i] + 4, ys[i] + 4, -16777216);
            g.fill(xs[i] - 2, ys[i] - 2, xs[i] + 3, ys[i] + 3, c);
            // 序号
            g.drawString(this.font, String.valueOf(i + 1), xs[i] + 4, ys[i] + 1, c, false);
            // 名字（当前站用亮绿，其余用淡黄）
            String nm = this.trim(((WpTarget) route.get(i)).name(), 6);
            int nmColor = i == be.getRouteIndex() ? 0xFF30FF60 : 0xFFD8C060;
            int tw = this.font.width(nm);
            // 半透明底：地形颜色随机，纯文字容易糊在一起
            g.fill(xs[i] + 3, ys[i] - 11, xs[i] + 7 + tw, ys[i] - 1, 0xB0000000);
            g.drawString(this.font, nm, xs[i] + 5, ys[i] - 10, nmColor, false);
        }

        // ③ 单独设置的目标（不在航线里时）—— 橙色准星
        if (be.hasTarget() && !targetInRoute) {
            int tx = (int) Math.round(30.0 + ((double) be.getTargetX() - vMinX) * scale);
            int ty = (int) Math.round(22.0 + ((double) be.getTargetZ() - vMinZ) * scale);
            int c = 0xFFFF9A20;
            // 外框
            g.fill(tx - 4, ty - 4, tx + 5, ty - 2, c);
            g.fill(tx - 4, ty + 3, tx + 5, ty + 5, c);
            g.fill(tx - 4, ty - 4, tx - 2, ty + 5, c);
            g.fill(tx + 3, ty - 4, tx + 5, ty + 5, c);
            // 中心点
            g.fill(tx - 1, ty - 1, tx + 2, ty + 2, c);
            // 名字
            String nm = this.trim(be.getTargetName(), 6);
            int tw = this.font.width(nm);
            g.fill(tx + 5, ty - 11, tx + 9 + tw, ty - 1, 0xB0000000);
            g.drawString(this.font, nm, tx + 7, ty - 10, c, false);
        }

        int plx = (int)Math.round(30.0 + (px - vMinX) * scale);
        int ply = (int)Math.round(22.0 + (pz - vMinZ) * scale);
        g.fill(plx - 2, ply - 2, plx + 3, ply + 3, -16777216);
        g.fill(plx - 1, ply - 1, plx + 2, ply + 2, -1);
        g.disableScissor();
        if (this.mapZoom > 1.001) {
            g.drawString(this.font, this.mapZoomLabel(), 34, 26, -256, false);
        }
    }

    /**
     * 当前视野的世界跨度。
     *
     * lastSpanX/lastSpanZ 记录的就是「已经除以 zoom 后的实际视野」，
     * 这里不能再除一次，否则放大后拖动灵敏度会变成 1/zoom，手感发飘。
     */
    private double spanXEstimate() {
        return this.lastSpanX;
    }

    private double spanZEstimate() {
        return this.lastSpanZ;
    }

    /** 地形底图按需重采样（范围变化或超过刷新间隔才重算）。 */
    private void refreshTerrainIfNeeded(double minX, double maxX, double minZ, double maxZ) {
        // 思索镜像里没有 Xaero 数据，采样会每秒扫上百维度、数万 region 全部 miss，
        // 直接把渲染线程拖死（日志里 [Xaero] region stats 刷屏就是这个）。
        if (this.isPonderScene()) {
            return;
        }
        // 拖动/缩放进行中不重采样：每帧重算 36300 像素会卡死，松手后再刷新
        if (this.draggingMap) {
            return;
        }
        boolean stale;
        long now = System.currentTimeMillis();
        boolean rangeChanged = this.mapTexture == null || Math.abs(minX - this.mapMinX) > 32.0 || Math.abs(maxX - this.mapMaxX) > 32.0 || Math.abs(minZ - this.mapMinZ) > 32.0 || Math.abs(maxZ - this.mapMaxZ) > 32.0;
        boolean bl = stale = now - this.mapSampleTime > 15000L;
        if (!rangeChanged && !stale) {
            return;
        }
        this.mapMinX = minX;
        this.mapMaxX = maxX;
        this.mapMinZ = minZ;
        this.mapMaxZ = maxZ;
        this.mapSampleTime = now;
        this.sampleTerrain(minX, maxX, minZ, maxZ);
    }

    /** 采样一片区域的地表颜色，写入贴图缓存。 */
    private void sampleTerrain(double minX, double maxX, double minZ, double maxZ) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        int w = 220;
        int h = 165;
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        double spanX = Math.max(1.0, maxX - minX);
        double spanZ = Math.max(1.0, maxZ - minZ);
        if (dbgSample < 3) {
            dbgSample++;
            com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                    "[MapSample] range X[{},{}] Z[{},{}] span=({},{}) size={}x{} tex={}",
                    minX, maxX, minZ, maxZ, spanX, spanZ, w, h,
                    this.mapTexture == null ? "null" : "ok");
        }
        int curRx = Integer.MIN_VALUE;
        int curRz = Integer.MIN_VALUE;
        int[] curImg = null;
        for (int py = 0; py < h; ++py) {
            for (int px = 0; px < w; ++px) {
                double wx = minX + spanX * ((double)px + 0.5) / (double)w;
                double wz = minZ + spanZ * ((double)py + 0.5) / (double)h;
                int bx = (int)Math.floor(wx);
                if (dbgPt < 6) {
                    dbgPt++;
                    com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                            "[MapSample] p({},{}) -> block({},{})", px, py, bx, (int)Math.floor(wz));
                }
                int bz = (int)Math.floor(wz);
                // ① 优先用 Xaero 的离线缓存
                //    按 region 分块：同一 region 内只查一次 Map，之后纯数组索引
                int rgx = bx >> 9;
                int rgz = bz >> 9;
                if (rgx != cachedRgX || rgz != cachedRgZ) {
                    cachedRgImg = com.hzldm.createnavigationterminal.integration.xaero.XaeroMapSource
                            .getRegionArray(rgx, rgz);
                    cachedRgX = rgx;
                    cachedRgZ = rgz;
                }
                if (cachedRgImg != null) {
                    int xaeroCol = cachedRgImg[((bz & 511) << 9) | (bx & 511)];
                    int rr = (xaeroCol >> 16) & 0xFF;
                    int gg = (xaeroCol >> 8) & 0xFF;
                    int bb = xaeroCol & 0xFF;
                    if (rr >= 16 || gg >= 16 || bb >= 16) {
                        img.setPixelRGBA(px, py, NavigationTerminalScreen.toAbgr(
                                0xFF000000 | (rr << 16) | (gg << 8) | bb));
                        continue;
                    }
                }
                // ② 本地已加载区块
                if (this.minecraft.level.getChunkSource().getChunk(bx >> 4, bz >> 4, false) != null) {
                    img.setPixelRGBA(px, py,
                            NavigationTerminalScreen.toAbgr(this.surfaceColor(bx, bz)));
                    continue;
                }
                // ③ 都没数据：深色网格，表示雷达未覆盖
                int grid = (px % 8 == 0 || py % 8 == 0) ? 0xFF2C3844 : 0xFF1E2830;
                img.setPixelRGBA(px, py, NavigationTerminalScreen.toAbgr(grid));
            }
        }
        this.releaseMapTexture();
        this.mapTexture = new DynamicTexture(img);
        ++this.mapSerial;
        this.mapLocation = ResourceLocation.fromNamespaceAndPath((String)"create_navigation_terminal", (String)("map/terrain_" + this.mapSerial));
        this.minecraft.getTextureManager().register(this.mapLocation, (AbstractTexture)this.mapTexture);
    }

    /**
     * 用 Xaero 的离线缓存取地表颜色。
     *
     * @return 0 表示缓存里没有这一格
     */
    /**
     * 用 Xaero 的离线缓存取地表颜色。
     *
     * 走我们自己的 .xwmc 解码器（格式已完整逆出，103 个文件验证通过），
     * 不依赖 Xaero 的运行时 API —— 它的 region 需要 mapRunner 线程调度，
     * 从 GUI 线程直接调永远拿不到。
     *
     * @return 0 表示缓存里没有这一格
     */
    private int xaeroSurfaceColor(int bx, int bz) {
        try {
            return com.hzldm.createnavigationterminal.integration.xaero.XaeroMapSource
                    .colorAt(bx, bz);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 取某个水平坐标的地表颜色（向下跳过草/雪/花等覆盖物）。 */
    private int surfaceColor(int bx, int bz) {
        try {
            int y = this.minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
            if (y <= this.minecraft.level.getMinBuildHeight() + 1) {
                return -15724528;
            }
            int yy = y - 1;
            BlockState st = this.minecraft.level.getBlockState(new BlockPos(bx, yy, bz));
            for (int i = 0; i < 4 && NavigationTerminalScreen.isCover(st); ++i) {
                st = this.minecraft.level.getBlockState(new BlockPos(bx, --yy, bz));
            }
            BlockPos pos = new BlockPos(bx, yy, bz);
            int base = NavigationTerminalScreen.colorForBlock(st);
            if (base == 0) {
                MapColor mc = st.getMapColor((BlockGetter)this.minecraft.level, pos);
                base = mc == MapColor.NONE ? -9803158 : 0xFF000000 | mc.calculateRGBColor(MapColor.Brightness.NORMAL) & 0xFFFFFF;
            } else if (NavigationTerminalScreen.isTreeCanopy(st)) {
                int lift = Math.min(14, Math.max(0, (y - 64) / 4));
                int r2 = NavigationTerminalScreen.clamp255((base >> 16 & 0xFF) + lift);
                int g2 = NavigationTerminalScreen.clamp255((base >> 8 & 0xFF) + lift);
                int b2 = NavigationTerminalScreen.clamp255((base & 0xFF) + lift / 2);
                base = 0xFF000000 | r2 << 16 | g2 << 8 | b2;
            }
            base = this.applyBiomeTint(base, bx, y, bz);
            double f = 1.0 + (double)(y - 64) / 256.0;
            if (f < 0.75) {
                f = 0.75;
            }
            if (f > 1.25) {
                f = 1.25;
            }
            int r = NavigationTerminalScreen.clamp255((int)((double)(base >> 16 & 0xFF) * f));
            int g = NavigationTerminalScreen.clamp255((int)((double)(base >> 8 & 0xFF) * f));
            int b = NavigationTerminalScreen.clamp255((int)((double)(base & 0xFF) * f));
            return 0xFF000000 | r << 16 | g << 8 | b;
        }
        catch (Throwable t) {
            return -15724528;
        }
    }

    /** 按高度给明暗等级（与原版地图一致：越高越亮）。 */
    private static MapColor.Brightness brightnessFor(int y) {
        if (y < 48) {
            return MapColor.Brightness.LOWEST;
        }
        if (y < 62) {
            return MapColor.Brightness.LOW;
        }
        if (y < 92) {
            return MapColor.Brightness.NORMAL;
        }
        return MapColor.Brightness.HIGH;
    }

    /** 是否是植被（草、花等），取地表色时跳过。 */
    private static boolean isVegetation(BlockState st) {
        return st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.MOSS_BLOCK) || st.is(Blocks.OAK_LEAVES) || st.is(Blocks.BIRCH_LEAVES) || st.is(Blocks.SPRUCE_LEAVES) || st.is(Blocks.JUNGLE_LEAVES) || st.is(Blocks.DARK_OAK_LEAVES) || st.is(Blocks.ACACIA_LEAVES) || st.is(Blocks.MANGROVE_LEAVES) || st.is(Blocks.CHERRY_LEAVES);
    }

    /** 是否是覆盖物（草/雪/花），取地表色时跳过。 */
    private static boolean isCover(BlockState st) {
        return st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS) || st.is(Blocks.FERN) || st.is(Blocks.LARGE_FERN) || st.is(Blocks.SNOW) || st.is(Blocks.DEAD_BUSH);
    }

    /**
     * 是否是树冠类方块（树叶 / 原木）。
     *
     * 原木不算「可跳过」—— 跳过会让树冠取到地面色，树就和草地融成一片。
     */
    private static boolean isTreeCanopy(BlockState st) {
        return st.is(Blocks.OAK_LEAVES) || st.is(Blocks.BIRCH_LEAVES) || st.is(Blocks.SPRUCE_LEAVES) || st.is(Blocks.JUNGLE_LEAVES) || st.is(Blocks.DARK_OAK_LEAVES) || st.is(Blocks.ACACIA_LEAVES) || st.is(Blocks.MANGROVE_LEAVES) || st.is(Blocks.CHERRY_LEAVES) || st.is(Blocks.OAK_LOG) || st.is(Blocks.BIRCH_LOG) || st.is(Blocks.SPRUCE_LOG) || st.is(Blocks.JUNGLE_LOG) || st.is(Blocks.DARK_OAK_LOG) || st.is(Blocks.ACACIA_LOG);
    }

    /**
     * 按生物群系给地表色加色调偏移。
     *
     * 只调整绿色系（草地/树叶）；沙漠、恶地、雪原各有独立处理，
     * 避免把所有绿地都染成同一种黄。
     */
    private int applyBiomeTint(int base, int bx, int y, int bz) {
        try {
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder =
                    this.minecraft.level.getBiome(new BlockPos(bx, y, bz));
            String path = holder.unwrapKey().map(k -> k.location().getPath()).orElse("");
            int r = base >> 16 & 0xFF;
            int g = base >> 8 & 0xFF;
            int b = base & 0xFF;
            if (g <= r || g < b) {
                return base;
            }
            if (path.contains("desert") || path.contains("badlands") || path.contains("mesa")) {
                r = NavigationTerminalScreen.clamp255(r + 50);
                g = NavigationTerminalScreen.clamp255(g - 30);
                b = NavigationTerminalScreen.clamp255(b - 30);
            } else if (path.contains("savanna") || path.contains("plains") || path.contains("meadow") || path.contains("steppe") || path.contains("prairie")) {
                r = NavigationTerminalScreen.clamp255(r + 16);
                g = NavigationTerminalScreen.clamp255(g + 8);
                b = NavigationTerminalScreen.clamp255(b - 12);
            } else if (path.contains("forest") || path.contains("taiga") || path.contains("jungle") || path.contains("grove") || path.contains("wood")) {
                r = NavigationTerminalScreen.clamp255(r - 25);
                g = NavigationTerminalScreen.clamp255(g - 8);
                b = NavigationTerminalScreen.clamp255(b - 20);
            } else if (path.contains("snow") || path.contains("frozen") || path.contains("ice")) {
                r = NavigationTerminalScreen.clamp255(r + 60);
                g = NavigationTerminalScreen.clamp255(g + 60);
                b = NavigationTerminalScreen.clamp255(b + 60);
            } else if (path.contains("swamp") || path.contains("mangrove")) {
                r = NavigationTerminalScreen.clamp255(r - 15);
                g = NavigationTerminalScreen.clamp255(g - 5);
                b = NavigationTerminalScreen.clamp255(b - 25);
            }
            return 0xFF000000 | r << 16 | g << 8 | b;
        }
        catch (Throwable t) {
            return base;
        }
    }

    /** 把颜色分量夹到 0-255。 */
    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** 释放地图贴图资源。 */
    private void releaseMapTexture() {
        if (this.mapTexture != null && this.mapLocation != null && this.minecraft != null) {
            try {
                this.minecraft.getTextureManager().release(this.mapLocation);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        this.mapTexture = null;
        this.mapLocation = null;
    }

    /** 兜底色：贴图还没采样到时的占位颜色。 */
    private int fallbackColor(int bx, int bz) {
        try {
            int y = this.minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
            if (y <= this.minecraft.level.getMinBuildHeight() + 1) {
                return -15724528;
            }
            return NavigationTerminalScreen.colorForBlock(this.minecraft.level.getBlockState(new BlockPos(bx, y - 1, bz)));
        }
        catch (Throwable t) {
            return -15724528;
        }
    }

    /** ARGB 转 ABGR（Minecraft 原生贴图格式）。 */
    private static int toAbgr(int argb) {
        int a = argb >>> 24 & 0xFF;
        int r = argb >>> 16 & 0xFF;
        int g = argb >>> 8 & 0xFF;
        int b = argb & 0xFF;
        return a << 24 | b << 16 | g << 8 | r;
    }

    /** 手写调色表：常用方块的地表颜色。 */
    private static int colorForBlock(BlockState st) {
        if (st.is(Blocks.OAK_LEAVES) || st.is(Blocks.BIRCH_LEAVES) || st.is(Blocks.SPRUCE_LEAVES) || st.is(Blocks.JUNGLE_LEAVES) || st.is(Blocks.DARK_OAK_LEAVES) || st.is(Blocks.ACACIA_LEAVES) || st.is(Blocks.MANGROVE_LEAVES) || st.is(Blocks.CHERRY_LEAVES)) {
            return -8746454;
        }
        if (st.is(Blocks.DIRT_PATH) || st.is(Blocks.GRAVEL) || st.is(Blocks.COBBLESTONE)) {
            return -6649238;
        }
        if (st.is(Blocks.WATER) || st.is(Blocks.KELP) || st.is(Blocks.SEAGRASS)) {
            return -13742450;
        }
        if (st.is(Blocks.LAVA)) {
            return -3121120;
        }
        if (st.is(Blocks.SNOW_BLOCK) || st.is(Blocks.POWDER_SNOW) || st.is(Blocks.ICE)) {
            return -2234128;
        }
        if (st.is(Blocks.SAND) || st.is(Blocks.SANDSTONE) || st.is(Blocks.RED_SAND)) {
            return -2570102;
        }
        if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS) || st.is(Blocks.MOSS_BLOCK)) {
            return -9527742;
        }
        if (st.is(Blocks.DIRT) || st.is(Blocks.COARSE_DIRT) || st.is(Blocks.PODZOL)) {
            return -8758728;
        }
        if (st.is(Blocks.STONE) || st.is(Blocks.ANDESITE) || st.is(Blocks.GRAVEL)) {
            return -7435648;
        }
        if (st.is(Blocks.DEEPSLATE) || st.is(Blocks.BLACKSTONE) || st.is(Blocks.BASALT)) {
            return -11908526;
        }
        if (st.is(Blocks.NETHERRACK)) {
            return -7718344;
        }
        if (st.is(Blocks.END_STONE)) {
            return -2565976;
        }
        if (st.is(Blocks.OAK_LOG) || st.is(Blocks.OAK_PLANKS) || st.is(Blocks.OAK_LEAVES)) {
            return -11900374;
        }
        return 0;
    }

    /** 把地形贴图贴到面板上；没有贴图（思索镜像 / 无 Xaero 数据）时画占位地图。 */
    private void drawTerrain(GuiGraphics g) {
        if (this.mapLocation == null) {
            this.drawMapPlaceholder(g);
            return;
        }
        if (this.dragRenderOffsetX != 0 || this.dragRenderOffsetY != 0) {
            // 拖动中：把已渲染的贴图整体平移，画面跟手且零采样
            g.pose().pushPose();
            g.pose().translate(this.dragRenderOffsetX, this.dragRenderOffsetY, 0);
            g.blit(this.mapLocation, 30, 22, 0.0f, 0.0f, 220, 165, 220, 165);
            g.pose().popPose();
        } else {
            g.blit(this.mapLocation, 30, 22, 0.0f, 0.0f, 220, 165, 220, 165);
        }
    }

    /**
     * 占位地图：没有任何地形贴图时（思索场景里没有 Xaero 数据）画一张示意底图，
     * 避免面板一片纯黑。网格 + 中心十字 + 右下角「示例地图」，航线点会照常叠在上面。
     */
    private void drawMapPlaceholder(GuiGraphics g) {
        final int x0 = 30;
        final int y0 = 22;
        final int w = 220;
        final int h = 165;
        g.fill(x0, y0, x0 + w, y0 + h, 0xFF262626);
        for (int gx = x0 + 20; gx < x0 + w; gx += 20) {
            g.fill(gx, y0, gx + 1, y0 + h, 0xFF2E2E2E);
        }
        for (int gy = y0 + 20; gy < y0 + h; gy += 20) {
            g.fill(x0, gy, x0 + w, gy + 1, 0xFF2E2E2E);
        }
        // 中心十字
        int cx = x0 + w / 2;
        int cy = y0 + h / 2;
        g.fill(cx - 6, cy, cx + 7, cy + 1, 0xFF454545);
        g.fill(cx, cy - 6, cx + 1, cy + 7, 0xFF454545);
        // 角标
        String tag = "示例地图";
        int tw = this.font.width(tag);
        g.drawString(this.font, tag, x0 + w - tw - 4, y0 + h - 11, 0xFF6E6E6E, false);
    }

    /** Bresenham 画线（GUI 里没有原生画线）。 */
    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int guard = 0;
        while (guard++ < 3000) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 >= dx) continue;
            err += dx;
            y0 += sy;
        }
    }

    /**
     * 点击处理：
     *   航线页 —— 左键按住某行开始拖拽排序（仿 PS 图层）
     *   导航页 —— 左键设目标、右键加航线
     *   右键菜单 —— 选中某一项
     */
    /** 弹出居中的命名对话框（关闭后自动回到本界面） */
    private void openCardNameScreen() {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.setScreen(new CardNameScreen(this, this.pos(),
                this.currentCardName(), this.currentCardReadOnly()));
    }

    // ---- 卡片（槽 0）读取辅助 ----
    private com.hzldm.createnavigationterminal.item.WaypointCardData currentCard() {
        if (this.menu == null || this.menu.slots.isEmpty()) {
            return null;
        }
        var stack = this.menu.slots.get(0).getItem();
        if (stack.isEmpty() || !(stack.getItem()
                instanceof com.hzldm.createnavigationterminal.item.WaypointCardItem)) {
            return null;
        }
        return stack.get(com.hzldm.createnavigationterminal.registry.ModDataComponents.WAYPOINT_CARD);
    }

    private String currentCardName() {
        var d = this.currentCard();
        return d == null || d.name() == null ? "" : d.name();
    }

    private boolean currentCardReadOnly() {
        var d = this.currentCard();
        return d != null && d.readOnly();
    }

    /** 设置覆盖层里自绘控件的点击命中（分页 / 行箭头 / 预设 / 频道） */
    private boolean handleSettingsClick(double mx, double my, int button) {
        if (button != 0 || this.minecraft == null) {
            return false;
        }
        int x0 = this.leftPos + CFG_X;
        int y0 = this.topPos + CFG_Y;

        // 分页
        if (NavGuiStyle.inside(mx, my, x0 + CFG_W - 18, y0 + 3, 14, 11)) {
            this.settingsPage = (this.settingsPage + 1) % PAGE_ROWS.length;
            return true;
        }
        // 行箭头
        int[] rows = PAGE_ROWS[this.settingsPage];
        for (int slot = 0; slot < rows.length; slot++) {
            int rowId = rows[slot];
            if (rowId < 1 || rowId > ROW_COUNT || rowId == ROW_HEADING_OFFSET) {
                continue;
            }
            int ry = y0 + CFG_ROW_Y0 + slot * CFG_ROW_DY;
            if (NavGuiStyle.inside(mx, my, x0 + CFG_ARROW_LX, ry, 16, 15)) {
                this.cycle(rowId, -1);
                return true;
            }
            if (NavGuiStyle.inside(mx, my, x0 + CFG_ARROW_RX, ry, 16, 15)) {
                this.cycle(rowId, 1);
                return true;
            }
        }
        // 预设 1/2/3
        int fy = y0 + CFG_H - 18;
        for (int i = 0; i < 3; i++) {
            int bx = x0 + 30 + i * 18;
            if (NavGuiStyle.inside(mx, my, bx, fy, 16, 15)) {
                this.lastPresetSlot = i;
                PacketDistributor.sendToServer(new PresetPayload(this.pos(), 0, i));
                return true;
            }
        }
        // 频道 -/+
        if (NavGuiStyle.inside(mx, my, x0 + CFG_W - 40, fy, 16, 15)) {
            this.cycleChannel(-1);
            return true;
        }
        if (NavGuiStyle.inside(mx, my, x0 + CFG_W - 22, fy, 16, 15)) {
            this.cycleChannel(1);
            return true;
        }
        return false;
    }

    /** 某行在指定页里的序号（不在该页返回 -1），与覆盖层绘制用的是同一套映射 */
    private int slotOfRow(int page, int rowId) {
        int[] rows = PAGE_ROWS[page];
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == rowId) {
                return i;
            }
        }
        return -1;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 设置面板展开时，先处理面板里自绘控件的命中
        if (this.showSettings && handleSettingsClick(mouseX, mouseY, button)) {
            return true;
        }
        int row;
        int idx;
        NavigationTerminalBlockEntity be;
        int row2;
        int idx2;
        double relX = mouseX - (double)this.leftPos;
        double relY = mouseY - (double)this.topPos;

        // 右键菜单已打开：优先处理菜单点击
        if (this.menuIdx >= 0) {
            this.handleMenuClick(relX, relY, button);
            return true;
        }

        // 航线区：左键按住某一行开始拖拽重排
        if (!this.showSettings && button == 0
                && relX >= 256 && relX <= 334
                && relY >= 56 && relY < 56 + 8 * 18) {
            NavigationTerminalBlockEntity rb = this.terminal();
            if (rb != null) {
                int dragRow = (int) ((relY - 56) / 18);
                int dragIdx = this.routeScroll + dragRow;
                if (dragIdx >= 0 && dragIdx < rb.getRoute().size()) {
                    this.dragFrom = dragIdx;
                    this.dragTo = dragIdx;
                    this.setDragging(true);
                    com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                            "[Drag] start at idx={} relX={} relY={}", dragIdx, (int) relX, (int) relY);
                    return true;
                }
            }
        }

        // 地图区：左键拖动平移
        if (!this.showSettings && button == 0
                && relX >= VIEW_X && relX < VIEW_X + VIEW_W
                && relY >= VIEW_Y && relY < VIEW_Y + VIEW_H) {
            this.draggingMap = true;
            this.mapDragStartX = relX;
            this.mapDragStartY = relY;
            this.mapPanStartX = this.mapPanX;
            this.mapPanStartY = this.mapPanY;
            this.dragRenderOffsetX = 0;
            this.dragRenderOffsetY = 0;
            this.setDragging(true);
            return true;
        }

        if (relX >= 340.0 && relX <= 464.0 && relY >= 105.0 && relY < 185.0 && !this.candidates.isEmpty() && (idx2 = this.scroll + (row2 = (int)((relY - 105.0) / 16.0))) >= 0 && idx2 < this.candidates.size()) {
            if (button == 1) {
                this.menuIdx = idx2;
                int items = this.menuItemCount(idx2);
                // 菜单左上角跟随鼠标；只在超出 GUI 时贴边，避免把菜单拽到别处
                this.menuX = (int) relX;
                if (this.menuX + 140 > 470) {
                    this.menuX = 470 - 140;
                }
                if (this.menuX < 0) {
                    this.menuX = 0;
                }
                this.menuY = (int) relY;
                if (this.menuY + items * 16 > 322) {
                    this.menuY = 322 - items * 16;
                }
                if (this.menuY < 0) {
                    this.menuY = 0;
                }
                return true;
            }
            WpTarget t = this.candidates.get(idx2);
            PacketDistributor.sendToServer((CustomPacketPayload)new SetNavigationTargetPayload(this.pos(), t.name(), t.x(), t.y(), t.z(), t.dimension()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return true;
        }
        if (relX >= 258.0 && relX <= 330.0 && relY >= 56.0 && relY < 185.0 && (be = this.terminal()) != null && button == 0 && (idx = (row = (int)((relY - 56.0) / 18.0))) >= 0 && idx < be.getRoute().size()) {
            this.dragFrom = idx;
            this.dragTo = idx;
            return true;
        }
        this.menuIdx = -1;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 拖拽中：实时计算插入位置，松手时提交重排。 */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragFrom >= 0) {
            double relY = mouseY - (double) this.topPos;
            if (dbgDrag < 20) {
                dbgDrag++;
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                        "[Drag] dragged relY={} from={} to={}", (int) relY, this.dragFrom, this.dragTo);
            }
            NavigationTerminalBlockEntity rb = this.terminal();
            if (rb != null) {
                int row = (int) Math.floor((relY - 56) / 18);
                row = Math.max(0, Math.min(7, row));
                int idx = this.routeScroll + row;
                idx = Math.max(0, Math.min(rb.getRoute().size() - 1, idx));
                this.dragTo = idx;
            }
            return true;
        }
        if (this.draggingMap) {
            double relX = mouseX - (double) this.leftPos;
            double relY = mouseY - (double) this.topPos;
            // 像素位移换算成世界格位移；拖动方向与地图移动方向相反
            double perPixelX = this.spanXEstimate() / VIEW_W;
            double perPixelY = this.spanZEstimate() / VIEW_H;
            this.mapPanX = this.mapPanStartX - (relX - this.mapDragStartX) * perPixelX;
            this.mapPanY = this.mapPanStartY - (relY - this.mapDragStartY) * perPixelY;
            // 贴图跟着鼠标走（不重采样，靠画面平移过渡）
            this.dragRenderOffsetX = (float) (relX - this.mapDragStartX);
            this.dragRenderOffsetY = (float) (relY - this.mapDragStartY);
            return true;
        }
        if (this.dragFrom >= 0) {
            double relY = mouseY - (double)this.topPos;
            NavigationTerminalBlockEntity be = this.terminal();
            if (be != null) {
                int row = (int)((relY - 188.0) / 16.0);
                int target = this.scroll + row;
                this.dragTo = Math.max(0, Math.min(be.getRoute().size() - 1, target));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** 松开鼠标：把拖拽结果发给服务端执行重排。 */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 航线拖拽结束：提交重排
        if (this.dragFrom >= 0) {
            int from = this.dragFrom;
            int to = this.dragTo;
            this.dragFrom = -1;
            this.dragTo = -1;
            this.setDragging(false);
            if (from != to) {
                PacketDistributor.sendToServer(
                        new ReorderRoutePayload(this.pos(), from, to));
                com.hzldm.createnavigationterminal.CreateNavigationTerminal.LOGGER.info(
                        "[Drag] commit from={} to={}", from, to);
            }
            return true;
        }
        // 地图平移结束
        if (this.draggingMap) {
            this.draggingMap = false;
            this.setDragging(false);
            this.dragRenderOffsetX = 0;
            this.dragRenderOffsetY = 0;
            // 松手后刷新一次贴图
            this.mapTexture = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** 右键菜单某一页的条目数。 */
    private int menuItemCount(int idx) {
        if (idx < 0 || idx >= this.candidates.size()) {
            return 2;
        }
        WpTarget t = this.candidates.get(idx);
        NavigationTerminalBlockEntity be = this.terminal();
        boolean inRoute = be != null && be.isInRoute(t.x(), t.y(), t.z());
        return inRoute ? 3 : 2;
    }

    /** 处理右键菜单点击。 */
    private void handleMenuClick(double relX, double relY, int button) {
        boolean inside;
        if (this.menuIdx < 0 || this.menuIdx >= this.candidates.size()) {
            this.menuIdx = -1;
            return;
        }
        WpTarget t = this.candidates.get(this.menuIdx);
        int items = this.menuItemCount(this.menuIdx);
        int mh = items * 16;
        boolean bl = inside = relX >= (double)this.menuX && relX <= (double)(this.menuX + 140) && relY >= (double)this.menuY && relY <= (double)(this.menuY + mh);
        if (!inside) {
            this.menuIdx = -1;
            return;
        }
        int item = (int)((relY - (double)this.menuY) / 16.0);
        switch (item) {
            case 0: {
                PacketDistributor.sendToServer((CustomPacketPayload)new SetNavigationTargetPayload(this.pos(), t.name(), t.x(), t.y(), t.z(), t.dimension()), (CustomPacketPayload[])new CustomPacketPayload[0]);
                break;
            }
            case 1: {
                PacketDistributor.sendToServer((CustomPacketPayload)new AddRoutePayload(this.pos(), t.name(), t.x(), t.y(), t.z(), t.dimension()), (CustomPacketPayload[])new CustomPacketPayload[0]);
                break;
            }
            case 2: {
                PacketDistributor.sendToServer((CustomPacketPayload)new RemoveRoutePayload(this.pos(), t.x(), t.y(), t.z()), (CustomPacketPayload[])new CustomPacketPayload[0]);
                break;
            }
        }
        this.menuIdx = -1;
    }

    /** 绘制右键菜单（分页：设为目标 / 加入航线 / 从航线移除）。 */
    private void drawContextMenu(GuiGraphics g) {
        if (this.menuIdx < 0 || this.menuIdx >= this.candidates.size()) {
            return;
        }
        WpTarget t = this.candidates.get(this.menuIdx);
        NavigationTerminalBlockEntity be = this.terminal();
        boolean inRoute = be != null && be.isInRoute(t.x(), t.y(), t.z());
        int items = this.menuItemCount(this.menuIdx);
        int mh = items * 16;
        g.fill(this.menuX, this.menuY, this.menuX + 140, this.menuY + mh, -267382768);
        g.renderOutline(this.menuX, this.menuY, 140, mh, -7303024);
        String[] labels = this.menuLabels();
        for (int i = 0; i < items; ++i) {
            int y = this.menuY + i * 16;
            g.drawString(this.font, labels[i], this.menuX + 6, y + 4, i == 2 ? -28528 : NavGuiStyle.TEXT, false);
        }
    }

    /** 滚轮：列表滚动 / 地图缩放。 */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double relX = mouseX - this.leftPos;
        double relY = mouseY - this.topPos;
        // 地图区内滚轮 = 缩放（放大以鼠标处为中心）
        if (relX >= VIEW_X && relX < VIEW_X + VIEW_W
                && relY >= VIEW_Y && relY < VIEW_Y + VIEW_H
                && !this.showSettings) {
            double oldZoom = this.mapZoom;
            if (scrollY > 0.0) {
                this.mapZoom = Math.min(MAP_ZOOM_MAX, this.mapZoom * MAP_ZOOM_STEP);
            } else if (scrollY < 0.0) {
                this.mapZoom = Math.max(1.0, this.mapZoom / MAP_ZOOM_STEP);
            }
            if (this.mapZoom != oldZoom) {
                this.mapTexture = null;
            }
            return true;
        }
        // 路径点（自由选择）区：滚轮翻候选
        if (relX >= 340 && relX <= 464 && relY >= 100 && relY <= 190) {
            int max = Math.max(0, this.candidates.size() - CANDIDATE_VISIBLE);
            if (scrollY > 0.0) {
                this.scroll = Math.max(0, this.scroll - 1);
            } else if (scrollY < 0.0) {
                this.scroll = Math.min(max, this.scroll + 1);
            }
            return true;
        }

        // 导航路径点（当前航线）区：滚轮翻航线
        if (relX >= 256 && relX <= 334 && relY >= 22 && relY <= 190) {
            NavigationTerminalBlockEntity be = this.terminal();
            if (be != null) {
                int total = Math.max(1, be.getRoute().size());
                int max = Math.max(0, total - 8);
                if (scrollY > 0.0) {
                    this.routeScroll = Math.max(0, this.routeScroll - 1);
                } else if (scrollY < 0.0) {
                    this.routeScroll = Math.min(max, this.routeScroll + 1);
                }
            }
            return true;
        }

        if (this.currentPage != 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int max = Math.max(0, this.candidates.size() - 8);
        if (scrollY > 0.0) {
            this.scroll = Math.max(0, this.scroll - 1);
            return true;
        }
        if (scrollY < 0.0) {
            this.scroll = Math.min(max, this.scroll + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 关闭界面时释放贴图。 */
    public void onClose() {
        this.releaseMapTexture();
        super.onClose();
    }

    @Nullable
    /** 取当前界面绑定的终端方块实体。 */
    private NavigationTerminalBlockEntity terminal() {
        NavigationTerminalBlockEntity be;
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity blockEntity = this.minecraft.level.getBlockEntity(((NavigationTerminalMenu)this.menu).getTerminalPos());
        return blockEntity instanceof NavigationTerminalBlockEntity ? (be = (NavigationTerminalBlockEntity)blockEntity) : null;
    }

    /** 终端方块坐标。 */
    private BlockPos pos() {
        NavigationTerminalBlockEntity be = this.terminal();
        return be != null ? be.getBlockPos() : ((NavigationTerminalMenu)this.menu).getTerminalPos();
    }

    /** 目标是否与玩家在同一维度。 */
    private boolean isSameDimension(NavigationTerminalBlockEntity be) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return false;
        }
        return this.minecraft.level.dimension().location().toString().equals(be.getTargetDimension());
    }

    /** 客户端本地算的距离（服务端没同步时兜底）。 */
    private double clientDistance(NavigationTerminalBlockEntity be) {
        BlockPos p = be.getBlockPos();
        double dx = (double)p.getX() + 0.5 - (double)be.getTargetX();
        double dz = (double)p.getZ() + 0.5 - (double)be.getTargetZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 预览信号强度（按距离线性衰减，用于导航页的进度条）。 */
    private int previewPower(double dist, boolean arrived) {
        if (dist < 0.0) {
            return 0;
        }
        if (arrived && (this.localAction == 1 || this.localAction == 2)) {
            return 0;
        }
        switch (this.localMode) {
            case 2: {
                return this.localPower;
            }
            case 1: {
                return dist <= (double)this.localRadius ? this.localPower : 0;
            }
        }
        if (dist >= (double)this.localRange) {
            return 0;
        }
        if (dist <= (double)this.localRadius) {
            return this.localPower;
        }
        double ratio = ((double)this.localRange - dist) / (double)(this.localRange - this.localRadius);
        return NavigationTerminalScreen.clamp((int)Math.round(ratio * (double)this.localPower), 1, this.localPower);
    }

    /** 截断过长文本，超出补省略号。 */
    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max - 1) + "\u2026" : s;
    }

    /** 维度 id 缩写成短名（minecraft:overworld -> 主世界）。 */
    private static String shortDim(String dim) {
        if (dim == null || dim.isEmpty()) {
            return "?";
        }
        return switch (dim) {
            case "minecraft:overworld" -> "\u4e3b\u4e16\u754c";
            case "minecraft:the_nether" -> "\u4e0b\u754c";
            case "minecraft:the_end" -> "\u672b\u5730";
            default -> dim.contains(":") ? dim.substring(dim.indexOf(58) + 1) : dim;
        };
    }
}
