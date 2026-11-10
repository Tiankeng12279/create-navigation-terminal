# Create: Navigation Terminal

> ### ⚠️ 合规提示（TL;DR）
> **Xaero 集成是可选的只读消费**：不包含、不分发 Xaero 的任何代码、类文件、反编译产物或资源文件；
> 只读取玩家本机 `xaero/world-map/` 缓存，以及反射只读其公开 API 类获取路点列表。
> **本模组不要求安装 Xaero**，未安装时全部核心功能（终端/卡片/红石/HUD/编队）照常工作。
> **若 Xaero 作者提出异议，将立即移除该集成并回退到无 Xaero 的实现。**
> Xaero 采用 **ARR（保留所有权利）** 许可，因此该集成在严格解释下属于需要善意协商的灰色地带——
> 详见 [附录 A](#13-附录-axaero-集成与逆向分析)。

> 机械动力风格的红石导航终端 —— 读 Xaero 地图标记点（可选），按距离输出红石信号，附带载具 HUD 导航。

| 项 | 值 |
|---|---|
| mod id | `create_navigation_terminal` |
| 当前版本 | `1.0.0`（见 `gradle.properties`）|
| Minecraft | **1.21.1** |
| NeoForge | **21.1.244**（编译期）、运行时 21.1.252 已验证 |
| Java | **21** |
| 客户端/服务端 | 双端（`side = BOTH`）|

---

## 目录

- [1. 项目简介](#1-项目简介)
- [2. 功能特性](#2-功能特性)
- [3. 环境与依赖](#3-环境与依赖)
- [4. 安装](#4-安装)
- [5. 从源码构建](#5-从源码构建)
- [6. 使用说明](#6-使用说明)
- [7. HUD 说明](#7-hud-说明)
- [8. 配置项](#8-配置项)
- [9. 技术要点](#9-技术要点)
- [10. 目录结构](#10-目录结构)
- [11. 常见问题](#11-常见问题)
- [12. 更新记录](#12-更新记录)
- [13. 附录 A：Xaero 集成与逆向分析](#13-附录-axaero-集成与逆向分析)
- [14. 免责声明与使用条款](#14-免责声明与使用条款)

---

## 1. 项目简介

**Create: Navigation Terminal** 添加一台**导航终端**方块：你把地图上的地点设为目标，它按"离目标还有多远"输出 0–15 的红石信号；坐进绑定了终端的载具座椅后，还会在屏幕上叠一层战斗机风格的导航 HUD。

设计上分三层：

1. **终端方块**：选目标、编航线、调参数，输出红石；
2. **导航点卡**：把一组路点存成物品，跨玩家、跨载具搬运；
3. **载具 HUD**：只在你坐的那台载具上显示，下车即消失。

Xaero's Minimap / World Map 是**可选**的数据来源；不装也能用（详情见 [附录 A](#13-附录-axaero-集成与逆向分析)）。

---

## 2. 功能特性

### 内容物

| 注册 ID | 类型 | 说明 |
|---|---|---|
| `create_navigation_terminal:navigation_terminal` | 方块 + 方块实体 + 容器界面 | 选导航目标、调参数、按距离输出 0–15 红石 |
| `create_navigation_terminal:waypoint_card` | 物品 | 存一组路点（最多 32 个），可命名、可设只读、可用染料分类 |
| `create_navigation_terminal:mod_icon` | 物品（隐藏）| 仅用于创造模式标签页图标，不进入任何物品栏 |

### 导航

- 目标 / 航线 / 参数三套数据，全部在方块实体内；
- 距离**只算水平**（见 [9.1](#91-距离只算水平)）；
- 跨维度时输出满信号并提示换维度；
- 航线支持多站、站序调整、单次 / 循环两种末尾行为。

### 导航点卡

- 从终端「写」入当前航线、「读」出到终端；
- 卡片可**命名**、可提为**只读**（只读卡不允许被当前航线覆盖）；
- 合成台里**用染料给卡上色**（16 种染料 = 16 种颜色），用于按航线分类；上色**不会**丢失已存的航线、名字和只读标记。

### 载具 HUD

- 绑定座椅后，坐上去才显示，下车立即消失；
- 8 方向箭头、航向刻度、目标标记、距离游标、航线进度；
- 方位用**相机坐标 + 相机姿态**计算，第一人称 / F5 三种视角 / 自定义相机都无需校准。

### 思索（Ponder）

- 6 个内置思索场景随模组发布；
- 检测到 **Ponderer** 时自动安装到其脚本目录，未安装则跳过（不影响其它功能）。

---

## 3. 环境与依赖

### 必需

| 依赖 | 说明 |
|---|---|
| Minecraft **1.21.1** | `minecraft_version_range = [1.21.1]` |
| NeoForge **21.1.244** 及以上 | 声明为 required |
| **DriveByWire** | 声明为 required（`versionRange = [0,)`）|
| Java **21** | 编译与运行 |

> 依赖声明见 `src/main/templates/META-INF/neoforge.mods.toml`。

### 可选

| 依赖 | 作用 |
|---|---|
| **Xaero's Minimap** | 汇入路点、提供候选列表 |
| **Xaero's World Map** | 读取本地地图缓存作为终端底图 |
| **Ponderer** | 承载思索场景脚本（未安装则该功能跳过）|

### 关于 Create

本模组的**依赖声明中不含 Create**，代码里也不引用 `com.simibubi.*` / `net.createmod.*` 的类。与载具座椅、铭牌的交互通过 DriveByWire 与反射完成（见 [13.3](#133-create--create-aeronautics-simulated)）。因此它可以在没有 Create 的整合包里运行，只是"载具相关"玩法无从发挥。

---

## 4. 安装

1. 安装 **Minecraft 1.21.1 + NeoForge 21.1.244+**；
2. 把 DriveByWire 放进 `mods/`；
3. 把 `create_navigation_terminal-<version>.jar` 放进 `mods/`；
4. 需要路点 / 地图时再放 Xaero's Minimap、Xaero's World Map；
5. 启动游戏。

服务端同样需要安装（含方块实体与网络包）；纯客户端安装会因缺少注册项无法进入服务器。

---

## 5. 从源码构建

```bat
cd C:\模组构建\create_navigation_terminal
gradlew.bat build
```

产物：

```
build\libs\create_navigation_terminal-<version>.jar
```

离线构建（依赖已缓存时）：

```bat
gradlew.bat build --offline
```

---

## 6. 使用说明

界面是**三个页签**：`导航` / `参数` / `航线`，底部固定 `重置到达` `清除目标` `绑定座椅`；左栏另有卡槽与 `写` `读` `设置` `改名` `只读` 五个按钮。

### 6.1 导航页：设目标 / 加航线

列表里是 Xaero 标记点（装了才有）+ 主手卡片里的点。

- **左键** 一个点 → 直接设为当前目标（清空航线）
- **右键** 一个点 → 弹出菜单：

```
┌──────────────────┐
│ 设为导航目标      │
│ 加入航线          │
│ 从航线移除        │  ← 仅当该点已在航线时出现
└──────────────────┘
```

### 6.2 参数页：调参数 + 预设

| 参数 | 范围 | 说明 |
|---|---|---|
| 模式 | 距离渐变 / 到达开关 / 固定强度 | 红石输出逻辑 |
| 强度上限 | 0–15 | 最大红石强度 |
| 覆盖距离 | 8–512 格 | 超出该距离输出 0 |
| 到达半径 | 1–64 格 | 进入后判定到达 |
| 到达后 | 保持输出 / 静默停止 / 清除目标 | 到达行为 |

**距离渐变**：

```
ratio = (覆盖距离 - 距离) / (覆盖距离 - 到达半径)
输出  = ratio × 强度上限
```

**预设**：三个槽，各存一组 5 参数。

```
点 [1]  → 应用预设 1
调参数  → 点 [存] → 覆盖刚才点选的那个槽
```

### 6.3 航线页：地图预览 + 站点管理

上半是**实时缩略地图**：

```
┌────────────────────────────────┐
│      ●───────●                 │  ● 黄 = 站点
│     ╱         ╲                │  ● 绿 = 当前站
│    ●           ●───────●       │  ● 白 = 玩家位置
│                                │  ─ 蓝 = 航线走向
│           ○                    │  ─ 紫 = 循环闭合段
└────────────────────────────────┘
```

- 按「所有站点 + 玩家位置」的包围盒等比缩放，站点再远也能全塞进来；
- 相邻站之间画蓝线；**循环模式**额外画一条从末站回首站的紫线；
- 装了 Xaero 时底图是地形贴图；没有 Xaero 数据时退回**示意网格**（右下角标「示例地图」）。

下半是站点列表 + 控制按钮：

```
  1. 铁矿营地        413, -143
▶ 2. 下界门          -372, -149     ← 当前站高亮
  3. 终点            33, -8
第 2/3 站 · 循环
[◀] [▶] [×] [循环]
```

- `◀` / `▶`：手动切上一站 / 下一站
- `×`：删除当前站（删空后清目标）
- `循环` / `单次`：切换航线末尾行为
  - **循环**：到最后一站后回到第一站，无限跑
  - **单次**：到最后一站就停住并标记到达
- HUD 右上角同步显示 `2/3`

### 6.4 导航点卡：存 / 读 / 改名 / 只读 / 上色

```
1. 把卡放进左栏卡槽
2. [写]  当前航线 → 卡
3. [读]  卡 → 当前航线
4. [改名] 弹出输入框，回车保存
5. [只读] 切换只读：只读卡可读、可改名，但拒绝被写入
6. 合成台：卡 + 任意染料 → 同色的卡（保留航线/名字/只读）
```

### 6.5 绑定座椅

```
1. 坐到载具座椅上
2. 打开载具上的导航终端界面
3. 点「绑定座椅」（普通右键，不要潜行 —— 潜行会下车）
```

---

## 7. HUD 说明

```
         ↑ 铁矿营地  2/3
   距离 187 格   ΔY +12
       已到达 · 导航停止
```

- 箭头：8 方向，按目标相对**你当前视角**计算；
- 只在绑定的座椅上显示，下车立即消失；
- 跨维度时显示 `目标在下界`（随目标维度变化）；
- 默认需要佩戴**工程师护目镜**（配置 `hud.requireGoggles`）。

---

## 8. 配置项

文件：`config/create_navigation_terminal-client.toml`，全部键位于 `[hud]` 段（共 24 项，下表与代码 `ClientConfig.java` 一致）。

| 键 | 默认 | 说明 |
|---|---|---|
| `colorMain` | `3CFF5A` | HUD 主色（RRGGBB，不带 `#`）|
| `colorDim` | `28C040` | HUD 次色 |
| `colorOther` | `FFA500` | 航线其它站点颜色 |
| `colorFleet` | `4FA3FF` | 编队标记颜色 |
| `colorBeacon` | `00E5FF` | 信标颜色 |
| `markerStyle` | `box` | 标记形状（6 种）|
| `markerYOffset` | `-6` | 标记纵向偏移（像素）|
| `showSpatialMarker` | `true` | 显示空间标记（方框）|
| `showOtherWaypoints` | `true` | 显示航线其它站点 |
| `showAllWaypoints` | `true` | 显示 Xaero 全部路点 |
| `hiddenWaypoints` | 空 | 隐藏的路点名，逗号分隔 |
| `showHorizon` | `true` | 显示地平线指示线 |
| `showFleet` | `true` | 显示编队标记 |
| `showBeacons` | `true` | 显示只有铭牌、没有终端的载具 |
| `requireGoggles` | `true` | 是否要求佩戴护目镜 |
| `showRadar` | `true` | 显示雷达 |
| `showDebug` | `false` | 显示左上角调试行 |
| `headingTapeY` | `46` | 航向刻度 Y |
| `distScaleBottom` | `82` | 距离游标距底 |
| `crosshairSize` | `10` | 准星尺寸（半长像素）|
| `smoothing` | `0.6` | 位置平滑（0=原始，越高越平滑越滞后）|
| `deadZone` | `2.0` | 死区像素（只挡亚像素抖动）|
| `relOffsetDeg` | `0.0` | **遗留项**，3D 投影方案后不再影响标记位置 |
| `fovHalfDeg` | `100.0` | **遗留项**，同上 |

> 早期版本另有一批键（`invertMarker`、`invertVertical`、`followVehicleYaw`、`vehicleYawOffset`、`viewWeight`、`thirdPersonYawOffset`、`verticalScale`）在当前代码中已不再注册，仅为兼容旧配置文件而保留说明，不再生效。

改完重进世界生效，不需要重启游戏。

---

## 9. 技术要点

### 9.1 距离只算水平

```
d = sqrt((x1-x2)² + (z1-z2)²)
```

高度差 `ΔY` 仅作参考显示，不参与信号与到达判定。

### 9.2 Sable SubLevel 兼容

Sable 物理结构（SubLevel）内的方块，`worldPosition` 是虚拟坐标
（实测 `20481034, 128, 20483095`），直接拿来算距离会得到 2.9e7 这种天文数字。

因此终端维护一组**世界参考坐标**（`refX/refY/refZ`）：

- 绑定座椅时写入玩家真实坐标；
- 玩家坐在绑定载具上时持续刷新；
- 距离计算优先使用参考坐标。

### 9.3 客户端不自己算距离

服务端每 8 tick 算好 `syncedDistance`，随 `getUpdateTag` 下发。
客户端只负责显示 —— 保证界面数字与红石信号永远同源。

### 9.4 HUD 方位用相机坐标而不是玩家坐标

第三人称（尤其是 Create Aeronautics 的自定义相机）会绕载具旋转，
相机离玩家可能有几十格。此时用玩家坐标算 `atan2` 会偏几十度。

所以 HUD 的**方位**用相机位置 + 相机 yaw：

```java
double cdx = targetX - camera.getPosition().x;
double cdz = targetZ - camera.getPosition().z;
double targetYaw = Math.atan2(-cdx, cdz);
float heading = camera.getYRot();
```

而**距离 / ΔY** 仍用玩家位置（你实际在哪）。

这样第一人称、F5 三种视角、Aeronautics 客制相机都无需任何偏移校准。

### 9.5 目标标记用 3D 投影

投影公式（MC 左手系：+X 东、+Y 上、+Z 南）：

```
forward = (-sin(yaw)·cos(pitch), -sin(pitch),  cos(yaw)·cos(pitch))
right   = (-cos(yaw),            0,          -sin(yaw))
up      = right × forward

side  = (target - cam) · right
upv   = (target - cam) · up
depth = (target - cam) · forward

screenX = centerX + side  / depth × fovScale
screenY = centerY - upv   / depth × fovScale
```

`fovScale = (screenHeight / 2) / tan(35°)`，与游戏画面透视一致。

相机数据每帧微变仍会表现为轻微抖动，因此加了可调指数平滑（`smoothing`）。

### 9.6 实现演进（踩过的坑）

| 做法 | 问题 |
|---|---|
| 玩家坐标 + `getYRot` | 第三人称/Aeronautics 相机绕载具转，方位偏 |
| 加载具朝向 | Create 座椅 yaw 与 MC 差 90°，更乱 |
| 相机坐标 + 相机 yaw | 方向对了，但用两个角度拼位置会漂 |
| **相机基向量 3D 投影** | **一次几何变换，无累积误差** |

---

## 10. 目录结构

```
create_navigation_terminal/
├─ build.gradle                       # NeoForge ModDev 构建脚本
├─ gradle.properties                  # 版本与依赖坐标
├─ README.md                          # 本文档
├─ README.legacy.md                   # 上一版 README（留档）
├─ libs/                              # compileOnly 的第三方 jar（Xaero / DriveByWire / Sable）
└─ src/main/
   ├─ templates/META-INF/neoforge.mods.toml   # mods.toml 模板
   ├─ java/com/hzldm/createnavigationterminal/
   │  ├─ CreateNavigationTerminal.java        # 入口 @Mod
   │  ├─ block/            (1 个文件)          # 导航终端方块
   │  ├─ blockentity/      (1)                 # 方块实体：航线/参数/卡槽/红石
   │  ├─ item/             (3)                 # 导航点卡、卡片数据、点位记录
   │  ├─ menu/             (1)                 # 容器菜单（卡槽）
   │  ├─ client/           (17)                # 界面、HUD、样式、图标、场景安装器
   │  ├─ config/           (1)                 # 客户端配置
   │  ├─ fleet/            (1)                 # 编队频道
   │  ├─ integration/      (7)                 # Xaero / DriveByWire / Sable 适配
   │  ├─ network/          (28)                # 26 个 payload + 注册与 HUD 服务
   │  ├─ recipe/           (1)                 # 卡片染料合成
   │  ├─ registry/         (7)                 # 物品/方块/组件/菜单/配方序列化器
   │  └─ server/           (2)                 # 服务端逻辑
   └─ resources/
      ├─ assets/create_navigation_terminal/
      │  ├─ blockstates/  models/  textures/   # 方块与物品模型、16 色卡面、图标
      │  ├─ lang/                               # zh_cn / en_us
      │  └─ ponder_scenes/                      # 6 个内置思索场景（发布用）
      └─ data/create_navigation_terminal/
         └─ recipe/card_dye.json                # 卡片染色配方（自定义类型）
```

---

## 11. 常见问题

**Q：没装 Xaero 能用吗？**
能。终端、卡片、红石、HUD、编队全部独立可用；只是路点候选列表为空、地图底图退回示意网格。

**Q：为什么地图是一片网格？**
当前没有可用的 Xaero 地形数据（未安装 Xaero、或处于思索镜像场景）。网格是占位底图，航线点仍会正常画在上面，右下角标有「示例地图」。

**Q：HUD 不显示？**
依次检查：① 是否坐在**绑定了终端**的座椅上；② 配置 `hud.requireGoggles` 默认为 `true`，需要佩戴护目镜；③ 是否已设置目标。

**Q：打开创造模式物品栏崩溃，报 `create:minecart_contraption` 未绑定？**
这是 **Create 侧**的问题，与本模组无关。日志证据（本仓库排查记录）：

```
Parsing error loading recipe create:crafting/kinetics/minecart_from_contraption_cart:
  Failed to parse either. First: Not a json array: {"item":"create:minecart_contraption"}
...
NullPointerException: Trying to access unbound value:
  ResourceKey[minecraft:item / create:minecart_contraption]
  at create@6.0.10…AllCreativeModeTabs$RegistrateDisplayItemsGenerator.makeExclusionPredicate
```

即 Create 自己的配方解析失败导致该物品未注册，构建创造栏内容时空指针。排查方向：停用会改写 Ingredient 解析的模组（该环境为 Polymorph）、检查 KubeJS 数据包覆盖。

**Q：卡片染色后航线丢了？**
不会。染色用自定义配方（`CardDyeRecipe`）复制输入卡的组件，只改颜色字段。

**Q：思索场景在哪？**
随模组发布于 `assets/create_navigation_terminal/ponder_scenes/`；检测到 Ponderer 时启动自动安装到 `config/ponderer/scripts/`，之后对着方块/卡片按 `W` 即可查看。

**Q：改配置要重启游戏吗？**
不需要，重进世界生效。

---

## 12. 更新记录

### 历史记录（原始表，保留原样）

| 版本 | 内容 |
|---|---|
| 1.0.0 | 导航终端方块（红石输出 + 到达即停）|
| 1.1.0 | 导航点卡 |
| 1.1.1 | 卡片点并入候选列表 |
| 1.1.2 | 距离改水平计算 |
| 1.1.3 | 座椅绑定 + HUD |
| 1.1.4 | 绑定改为普通右键（潜行会下车）|
| 1.1.5 | HUD 状态日志 |
| 1.1.6 | 距离与 ΔY 拆行，防挤压 |
| 1.1.7 | 距离由服务端算并同步 |
| 1.1.8 | SubLevel 世界参考坐标 + 界面绑定按钮 |
| 1.1.9 | 移除诊断行 |
| 1.2.0 | 多目标循环 + 跨维度 |
| 1.2.1 | HUD 显示航线进度 |
| 1.3.0 | 航线编辑（切站/删除）+ 参数预设 |
| 1.3.1 | 航线单次 / 循环切换 |
| 1.4.0 | 界面分页（导航 / 参数 / 航线）|
| 1.5.0 | 右键菜单 + 航线地图预览 |
| 1.6.0 | 地图改用地形采样上色（尝试读 Xaero 缓存，纹理内容为占位色，弃用）|
| 1.9.0 | GUI 放大到 320×300 |
| 2.1.0 | 1:1 采样 + 跳过树冠 + 生物群系着色 |
| 2.2.x | 地图缩放（滚轮）；等比视野修正长宽比 |
| 2.3.x | 树冠改黄绿独立上色；MapColor 兜底未知方块 |
| 2.4.x | 战斗机式 HUD：准星 / 航向刻度 / 目标标记 / 距离游标 |
| 2.5.x | 载具朝向实验（因 yaw 偏移问题回退）|
| 2.6.x | 客户端配置文件；角度归一化；相机插值；第三人称分支 |
| 2.7.x | HUD 方位改用相机坐标 —— 兼容 F5 与 Aeronautics 第三人称 |
| 2.8.x | 方框标记 + 6 种样式；真实俯仰角定位；上下贴边箭头 |
| **2.9.2** | **改用 3D 投影（相机基向量）；修正 MC 左手系；位置平滑滤波** |

> 公开发布起重新编号：首个公开版本为 **`1.0.0`**（见 `gradle.properties`）。上表是公开前的内部迭代记录，编号已废弃，仅作留档。

### 当前版本（`1.0.0`）已有能力
> 完整的逐版本变更见仓库根目录的 [`CHANGELOG.md`](CHANGELOG.md)；下面只列当前版本的关键能力。


- 导航点卡：**命名**、**只读**锁定、**染料 16 色分类**（合成台），三者都不影响卡内已存航线；
- 卡片使用 16 套独立卡面贴图（按颜色选模型，非 tint 染色）；
- 界面提供卡槽与 `写 / 读 / 设置 / 改名 / 只读` 按钮组；
- 6 个内置**思索场景**随模组发布，检测到 Ponderer 时自动安装；
- 模组图标与创造标签页图标已替换为项目图标；
- 界面 / HUD 渲染路径做了字符串与对象分配的缓存优化（`String.format` 与类名判定结果复用）。

---

## 13. 附录 A：Xaero 集成与逆向分析

> 本章节为**原文档内容，完整保留**，仅调整了标题层级以便对外发布阅读。

### 13.1 关于 Xaero 集成是「可选项」

**本模组不要求安装 Xaero's World Map 或 Minimap。**

- 未安装时：地图显示为视距内的实时地块（网格标示未加载区），路点列表为空
- 已安装时：额外汇入 Xaero 的路点、以及其离线缓存里的已探索地形

载具绑定、编队、红石信号输出等核心功能**完全独立于 Xaero**。

### 13.2 Xaero's World Map / Minimap

**做法**：读取玩家本机 `<gameDir>/xaero/world-map/` 下的地图缓存（`.xwmc`），
解码成图像用作导航终端的底图。

**边界**：

- **不包含** Xaero 的任何代码、类文件、反编译产物
- **不包含** Xaero 的任何资源文件（贴图、模型、语言文件、`vanilla_states.dat` 等）
- **不分发** Xaero 的任何数据；只读取**玩家自己机器上、自己游戏过程中产生**的缓存
- 缓存文件格式的知识来自**对公开下载的 mod jar 与本地文件的独立观察与结构分析**，
  未复制其实现代码

解码器（`XaeroCacheReader`）为独立实现：

- 零依赖，仅用 `java.util.zip` 与 `java.io`
- 布局由字节流反推 + 多文件交叉验证得出（见内部复核记录：103 个文件解析到 EOF，`trailing == 0`）

**关于 Xaero 自身的条款**：Xaero 的 mod 许可以其发布页为准。本模组**不修改、不重分发**
Xaero 本体，仅以只读方式消费其输出文件；若作者认为此用法不妥，可通过 issue 联系，
我们会移除该集成并回退到「仅显示视距内地块」的实现。

#### 13.2.1 反射读取的具体面（可自行核验）

除了解码缓存文件，路点列表是通过**只读反射**访问 Xaero 的公开类得到的。全部调用均在
`integration/xaero/XaeroReflect.java` 内，特征是：`Class.forName` + `getMethod(...).invoke(...)`，
**不修改字段、不调用任何 `set*`/`save*` 写方法、不使用 `setAccessible`**。

| 类（全限定名）| 调用的公开方法 |
|---|---|
| `xaero.hud.minimap.BuiltInHudModules` | 读取静态字段 `MINIMAP`（`getField(...).get(null)`）|
| `xaero.hud.module.HudModule` | `getCurrentSession()` |
| `xaero.hud.minimap.module.MinimapSession` | `getWorldManager()` |
| `xaero.hud.minimap.world.MinimapWorldManager` | `getCurrentWorld()` |
| `xaero.hud.minimap.world.MinimapWorld` | `getCurrentWaypointSet()` |
| `xaero.hud.minimap.waypoint.set.WaypointSet` | `getWaypoints()` |
| `xaero.hud.minimap.waypoint.WaypointColor` / `WaypointPurpose` | 读取颜色 / 用途枚举值 |
| `xaero.common.minimap.waypoints.Waypoint` | `getName()`，以及公开字段 `x` / `y` / `z` 的读取 |

所有调用都包在 `try/catch(Throwable)` 里：Xaero 未安装或内部结构变动时，仅记录一次警告并降级为空列表，
不会抛给玩家、也不会影响其它功能。

**关于写入**：本模组**不写入** Xaero 的任何数据。早期版本曾存在「把导航目标回写成 Xaero 路点」的代码路径
（`addWaypoint` + `MinimapWorldManagerIO#saveWorld`，需 `setAccessible` 读取私有字段），因**没有任何 UI 入口**
（无调用方）且与只读声明冲突，已整段移除；移除前的版本留档于 `_backup/XaeroReflect.java.before-readonly`。

### 13.3 Create / Create Aeronautics（Simulated）

**做法**：

- 座椅绑定：读取实体 UUID 或方块坐标（公开 API）
- 铭牌名称：**按类名后缀识别**再反射调用其公开方法，具体实现如下（`network/NavigationHudService.java`）：
  ```java
  if (!be.getClass().getName().endsWith(".NameplateBlockEntity")) continue;  // 按后缀识别，不硬引用类
  java.lang.reflect.Method m = be.getClass().getMethod("getName");           // 公开方法，无 setAccessible
  Object v = m.invoke(be);                                                  // 只读，不回写
  ```
  即：`<providing-mod>.…NameplateBlockEntity#getName()`，完整包名由提供该方块的模组决定
  （本仓库不写死包名，因此换版本/换提供者都不会编译失败）。
- 不做任何字节码修改，不 `setAccessible` 任何成员

**边界**：不包含 Create 系模组的代码或资源；所有交互通过其公开 API 或反射只读访问。

### 13.4 其他第三方

- **joml**（`org.joml`）：Minecraft 自带依赖，仅使用其公开数学 API
- 本项目**不含**任何反编译代码、破解补丁、注册机或绕过授权验证的功能

### 开发过程说明（AI 参与）

本项目的**代码与文档在 AI 辅助下完成**（Modrinth 披露项：*Contains AI-generated content* 已勾选）。
贴图（方块/物品/卡片/图标）为作者本人绘制。模组**运行时不含任何生成式 AI 功能**，不联网、不调用外部模型。

---

## 14. 免责声明与使用条款

本项目为**技术学习与单人/自建服务器游玩**用途，与上述第三方模组的作者无隶属或授权关系。
所有第三方模组的名称与商标归其各自作者所有，本项目仅以「兼容 xxx」的方式描述互操作性，
不使用其商标作为项目标识。

### 14.1 使用条款

本模组以 **MIT License** 发布，因此下列条款是在 MIT 之上做的**额外说明**，不缩小 MIT 授予你的权利：

1. 允许在整合包中分发本模组的**未修改** jar，保留 `neoforge.mods.toml` 中的作者与许可信息即可；
2. **允许 fork 与二次分发修改版**（MIT 赋予的权利），请一并保留版权声明与许可文本，并说明你的改动；
3. 不允许售卖本模组本体，或以本模组名义收费（MIT 不限制你对自己修改版的商业使用，但请勿冒充原始发布）；
4. 使用本模组造成的存档、红石机械或载具损失，由使用者自行承担（MIT 的免责条款同样适用）。

### 14.2 免责声明

> 说明：以上声明用于**明确技术边界与善意**，不构成对任何法律责任的免除。
> 若你计划将本项目用于商业分发，请自行取得所需的授权或法律意见。

### 14.3 版权

| 项 | 归属 |
|---|---|
| 本模组代码与资源 | 作者 **HZLDM**，许可 **MIT License**（`gradle.properties: mod_license`，构建时填入 `neoforge.mods.toml`）|
| Minecraft | Mojang Studios / Microsoft |
| NeoForge | NeoForged 团队 |
| Xaero's Minimap / World Map | xaero96 及其贡献者 |
| Create / Create Aeronautics | simibubi 及 Create 贡献者 |
| DriveByWire | 其原作者 |
