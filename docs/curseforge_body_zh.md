# Create: Navigation Terminal

> 机械动力风格的红石导航终端：选定目标，按距离输出 0–15 红石；坐上绑定座椅后显示战斗机式导航 HUD。

**⚠️ 合规提示**：Xaero 集成是**可选的只读消费**——不包含、不分发 Xaero 的任何代码、类文件、反编译产物或资源文件，仅读取玩家本机 `xaero/world-map/` 缓存并反射只读其**公开** API 类。**本模组不要求安装 Xaero**；若 Xaero 作者提出异议，将立即移除该集成并回退到无 Xaero 的实现。另提供 **`-noxaero` 文件**：jar 内不含任何 Xaero 互操作类。

---

## 下载哪个文件

| 文件 | 内容 | 适用 |
|---|---|---|
| `create_navigation_terminal-1.1.93-beta.jar` | 完整功能（含 Xaero 只读互操作） | 常规使用 |
| `create_navigation_terminal-noxaero-1.1.93-beta.jar` | **不含任何 Xaero 互操作类**，`mods.toml` 也不声明 Xaero 依赖；其余功能完全一致 | 需要零第三方互操作代码的场合 |

---

## 依赖

| 类型 | 依赖 | 说明 |
|---|---|---|
| 必需 | Minecraft **1.21.1** + NeoForge **21.1.244+** | — |
| 必需 | **Drive-By-Wire with Sable**（`drivebywire`，MIT） | 线缆信号源；缺失则无法被线缆连接，其余功能正常 |
| 可选 | **Xaero's Minimap** / **Xaero's World Map** | 路点与离线地形缓存 |
| 可选 | **Ponderer**（MIT） | 承载 6 个内置思索场景；未安装则跳过 |

> 本模组**不把 Create 列为依赖**；与载具座椅/铭牌的交互通过 Drive-By-Wire 与只读反射公开方法完成。
> 服务端也需要安装（含方块实体与 26 个网络 payload）。

---

## 功能

**导航终端（方块）**
- 三页签：导航 / 参数 / 航线；底部：重置到达、清除目标、绑定座椅
- 参数：模式（距离渐变 / 到达开关 / 固定强度）、强度上限 0–15、覆盖距离 8–512、到达半径 1–64
- 三组参数预设；航线多站、站序调整、单次 / 循环；跨维度输出满信号并提示
- 距离**只算水平**，服务端统一计算并同步（界面与红石同源）

**导航点卡（物品）**
- 单卡最多 32 个路点；可命名、可设只读（只读拒绝被覆盖，仍可读与改名）
- 合成台**用染料上色**（16 色，各一套卡面贴图）用于分类；上色不丢航线

**载具 HUD**
- 绑定座椅后显示、下车消失；8 方向箭头 + 航向刻度 + 目标标记 + 距离游标 + 航线进度
- 方位用**相机坐标 + 相机姿态**计算，第一人称 / F5 / 自定义相机均无需校准

---

## 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.244+；
2. 放入 Drive-By-Wire（必需）；
3. 把本模组 jar 放进 `mods/`；
4. 需要路点/地形时再装 Xaero 两件套；
5. 启动游戏。

---

## 配置

`config/create_navigation_terminal-client.toml`，全部键在 `[hud]` 段（24 项）。常用：

| 键 | 默认 | 说明 |
|---|---|---|
| `colorMain` / `colorDim` | `3CFF5A` / `28C040` | HUD 主/次色（RRGGBB） |
| `markerStyle` | `box` | 标记形状（6 种） |
| `smoothing` / `deadZone` | `0.6` / `2.0` | 位置平滑 / 死区像素 |
| `requireGoggles` | `true` | 是否要求护目镜 |
| `showRadar` / `showHorizon` / `showFleet` / `showBeacons` | `true` | 各类元素开关 |
| `hiddenWaypoints` | 空 | 隐藏路点名，逗号分隔 |
| `showDebug` | `false` | 左上角调试行 |

改完重进世界生效，无需重启游戏。

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

排查：停用会改写 Ingredient 解析的模组（如 Polymorph）、检查 KubeJS 数据包覆盖。

---

## 许可

**MIT License**（作者 HZLDM）。第三方模组名称与商标归各自作者；本模组不含任何反编译代码、破解补丁或授权绕过功能。
