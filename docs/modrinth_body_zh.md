# Create: Navigation Terminal

> 机械动力风格的红石导航终端：选定目标，按距离输出 0–15 红石；坐上绑定座椅后显示战斗机式导航 HUD。

---

## ⚠️ 合规提示（请先读）

- **Xaero 集成是可选的只读消费**：不包含、不分发 Xaero 的任何代码、类文件、反编译产物或资源文件；
  仅读取玩家本机 `xaero/world-map/` 缓存，并反射只读其**公开** API 类获取路点列表。
- **本模组不要求安装 Xaero**。未安装时：终端 / 点卡 / 红石 / HUD / 编队全部照常工作，
  只是候选路点列表为空、地图退回网格示意。
- Xaero 采用 **ARR（All Rights Reserved）** 许可，该集成在严格解释下属于需要善意协商的灰色地带。
  **若 Xaero 作者提出异议，将立即移除该集成并回退到无 Xaero 的实现。**
- 如你所在环境不希望包含任何互操作代码，请使用下文的 **`-noxaero` 变体**（jar 内不含任何读取 Xaero 的类）。

---

## 需要什么

| 类型 | 依赖 | 说明 |
|---|---|---|
| 必需 | Minecraft **1.21.1** + NeoForge **21.1.244+** | — |
| 必需 | [Drive-By-Wire with Sable](https://modrinth.com/mod/drive-by-wire-sable)（`drivebywire`，MIT）| 线缆信号源；缺失则无法被线缆连接，其余功能正常 |
| 可选 | [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) / [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map) | 路点与离线地形缓存 |
| 可选 | Ponderer（MIT）| 承载 6 个内置思索场景；未安装则跳过 |

> 依赖声明见 jar 内 `META-INF/neoforge.mods.toml`。**本模组不把 Create 列为依赖**，
> 与载具座椅/铭牌的交互通过 DriveByWire 与反射（只读公开方法）完成。

---

## 功能

**导航终端（方块）**
- 三页签界面：导航 / 参数 / 航线；底部重置到达、清除目标、绑定座椅
- 四参数：模式（距离渐变 / 到达开关 / 固定强度）、强度上限 0–15、覆盖距离 8–512、到达半径 1–64
- 三组参数预设；航线支持多站、站序调整、单次 / 循环
- 跨维度目标：输出满信号并提示换维度
- 距离**只算水平**，服务端统一计算并同步（界面数字与红石同源）

**导航点卡（物品）**
- 一个卡最多 32 个路点；可命名、可设只读（只读卡拒绝被覆盖，仍可读与改名）
- 合成台**用染料上色**（16 色，各一套卡面贴图），用于按航线分类；上色不影响已存航线

**载具 HUD**
- 绑定座椅后显示，下车即消失；8 方向箭头 + 航向刻度 + 目标标记 + 距离游标 + 航线进度
- 方位用**相机坐标 + 相机姿态**计算，第一人称 / F5 三种视角 / 自定义相机均无需校准

---

## 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.244+；
2. 放入 Drive-By-Wire（必需）；
3. 把本模组 jar 放进 `mods/`；
4. 需要路点/地形时再装 Xaero 两件套；
5. 启动游戏。

**服务端也需要安装**（含方块实体与 26 个网络 payload）；只装客户端无法进入服务器。

---

## 两个构建变体

| jar | 内容 | 适用 |
|---|---|---|
| `create_navigation_terminal-<ver>.jar` | 完整功能（含 Xaero 只读互操作）| 常规使用 |
| `create_navigation_terminal-noxaero-<ver>.jar` | **jar 内不含任何 Xaero 互操作类**，`mods.toml` 也不声明 Xaero 依赖；其余功能完全一致 | 需要零第三方互操作代码的场合 |

构建方式：

```bat
gradlew.bat build                 :: 完整版
gradlew.bat build -PnoXaero       :: 无 Xaero 变体
```

---

## 配置

`config/create_navigation_terminal-client.toml`，全部键在 `[hud]` 段（24 项）。

| 常用键 | 默认 | 说明 |
|---|---|---|
| `colorMain` / `colorDim` | `3CFF5A` / `28C040` | HUD 主/次色（RRGGBB）|
| `markerStyle` | `box` | 标记形状（6 种）|
| `smoothing` / `deadZone` | `0.6` / `2.0` | 位置平滑 / 死区像素 |
| `requireGoggles` | `true` | 是否要求护目镜 |
| `showRadar` / `showHorizon` / `showFleet` / `showBeacons` | `true` | 各类元素开关 |
| `hiddenWaypoints` | 空 | 隐藏的路点名，逗号分隔 |
| `showDebug` | `false` | 左上角调试行 |

改完重进世界生效，不需要重启游戏。完整清单见仓库 `README.md` 第 8 节。

---

## 已知问题

**打开创造模式物品栏崩溃，报 `create:minecart_contraption` 未绑定** —— 属 Create 侧问题，与本模组无关：

```
Parsing error loading recipe create:crafting/kinetics/minecart_from_contraption_cart:
  Failed to parse either. First: Not a json array: {"item":"create:minecart_contraption"}
NullPointerException: Trying to access unbound value:
  ResourceKey[minecraft:item / create:minecart_contraption]
  at create…AllCreativeModeTabs$RegistrateDisplayItemsGenerator.makeExclusionPredicate
```

排查方向：停用会改写 Ingredient 解析的模组（如 Polymorph）、检查 KubeJS 数据包覆盖。

---

## 更新记录

逐版本变更见仓库 `CHANGELOG.md`；本页只列最近版本。

| 版本 | 摘要 |
|---|---|
| 1.0.0 | **首个公开发布**：导航终端 + 航线 + 参数预设 + 载具 HUD + 编队频道 + 导航点卡（命名/只读/染料分类）+ 6 个思索场景；Xaero 集成为可选只读，并提供 `-noxaero` 变体 |

---

## 许可与致谢

- 本模组：**MIT License**（作者 HZLDM）
- 第三方模组名称与商标归各自作者；本页仅以「兼容 xxx」描述互操作性
- 本模组不含任何反编译代码、破解补丁或授权绕过功能

> Xaero 集成部分为善意互操作；权利方如有异议，请联系移除，我们会立即回退到无 Xaero 的实现。
