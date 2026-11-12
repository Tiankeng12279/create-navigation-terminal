# Changelog

本文件用**单一版本序列**记录本模组的变更，`README.md` 只保留摘要。

- 版本号来源：`gradle.properties` 的 `mod_version`
- 版本格式：`<major>.<minor>.<patch>`
- **公开发布从 `1.0.1` 起**；`1.0.0`、`1.0.2`–`1.0.4`、`1.1.x` 与更早的 `2.x` 都是公开前的内部迭代，编号已废弃，仅在本文件末尾留档

---

## [1.0.1] — 2026-11-10

**首个公开发布版本**（版本号自此起算）。本版已包含发布前修复的全部问题：

### 内容

- **导航终端（方块）**：三页签界面（导航 / 参数 / 航线）；距离渐变 / 到达开关 / 固定强度；强度上限 0–15、覆盖距离 8–512、到达半径 1–64；三组参数预设
- **航线**：多站、站序调整、单次 / 循环；跨维度输出满信号并提示
- **距离**：仅水平距离，服务端每 8 tick 计算并同步（界面与红石同源）
- **导航点卡**：单卡 32 点、可命名、可设只读；合成台 16 色染料分类，上色不丢航线
- **载具 HUD**：绑定座椅后显示；方位用相机坐标 + 相机姿态计算
- **编队频道**：同频共享目标、推送目标 / 航线、独占频道
- **思索**：6 个内置场景（检测到 Ponderer 时自动安装）
- **Xaero 集成**：可选、只读；另提供 `-noxaero` 变体（jar 内不含任何互操作类）

### 发布前修复（相对内部构建）

- `mods.toml` 占位符未展开导致“不是有效模组文件”的加载失败
- 换存档 / 切维度后静态缓存未清理（编队注册表、信标扫描、HUD 路点与 Xaero 地形缓存）
- 候选项点击命中区与绘制区不一致（行高 15 vs 16），点击落到空白
- 载具（Sable SubLevel）内终端的所有界面操作被距离校验静默拒绝
- **驾驶锁默认改为关闭**（旧存档缺该字段时按未锁处理）
- **模型渲染优化**：删除 34% 被完全遮挡的面（每面 216 → 170，含 `cn_skip` 后 204 → 170）、
  为边界面补 `cullface` 元数据；实测两个模型所有使用到的 UV 区域均为全不透明，
  故渲染层保持 **solid**（最快路径），无需 cutout/translucent

### 构建

```bat
gradlew.bat build                 :: 完整版
gradlew.bat build -PnoXaero       :: 无 Xaero 变体
```

---

## 内部迭代历史（公开前，编号已废弃）

### [1.0.0] — 内部构建（未公开）

内部构建，从未对外发布。

### 内容

- **导航终端（方块）**：三页签界面（导航 / 参数 / 航线）；距离渐变 / 到达开关 / 固定强度三种输出模式；强度上限 0–15、覆盖距离 8–512、到达半径 1–64；三组参数预设
- **航线**：多站、站序调整、单次 / 循环末尾行为；跨维度目标输出满信号并提示
- **距离**：仅水平距离，服务端每 8 tick 统一计算并同步，界面数字与红石信号同源
- **导航点卡（物品）**：单卡最多 32 个路点；可命名、可设只读；合成台用 16 种染料上色分类，上色不丢航线
- **载具 HUD**：绑定座椅后显示；8 方向箭头、航向刻度、目标标记、距离游标、航线进度；方位用相机坐标 + 相机姿态计算
- **编队频道**：同频终端共享目标、推送目标 / 推送航线 / 独占频道
- **思索**：6 个内置场景随模组发布，检测到 Ponderer 时自动安装
- **Xaero 集成（可选、只读）**：读取本机 `xaero/world-map/` 缓存 + 反射只读其公开 API 类；不包含、不分发其任何代码与资源
- **`-noxaero` 变体**：jar 内不含任何 Xaero 互操作类，`mods.toml` 也不声明 Xaero 依赖

### 构建

```bat
gradlew.bat build                 :: 完整版
gradlew.bat build -PnoXaero       :: 无 Xaero 变体
```

### 备注

- 公开前的 `1.1.x` 迭代历史见文末「内部迭代历史」
- 合规边界与第三方集成说明见 `README.md` 附录 A

---


### [1.0.4] — 2026-11-10

### Changed
- **驾驶锁（DriveLock）默认改为关闭**：
  - 字段初值 `driveLock = false`
  - 旧存档里若没有 `DriveLock` 这个 NBT 键，按“未锁”处理（原实现是缺失即视为锁定）

  > 背景：驾驶锁开启时，界面里的设目标 / 加航线等操作会被拒绝，表现极像“导航仪坏了”。
  > 默认关闭后，新放置的终端开箱即用；需要锁的玩家仍可在「设置 → 驾驶锁」里打开。

### [1.0.3] — 2026-11-10

### Fixed
- **可点击区域与绘制不一致**：候选项按 `y=106 / 行高 15` 绘制，点击判定却是 `y=105 / 行高 16`，
  逐行漂移导致点到空白处毫无反应。现两者统一（首行 103，行高 15）。
- **SubLevel（载具内部）里的终端所有操作被静默拒绝**：`resolveTerminal` 用
  `terminal.getBlockPos()`（局部坐标，实测 20481032,128,20481032）与玩家世界坐标做距离校验，
  距离平方恒大于阈值 → 设目标 / 加航线 / 改名等全部丢包。改为：
  先用 `SableTransform.worldCenter()` 换算到世界坐标再校验；玩家不在同一 level 时扫其它已加载 level 查找终端。
- 交互被拒时不再静默：日志输出 `[Nav] 交互被拒：距离平方=…`。

### 说明
- 本次排查中一度怀疑是"候选列表为空"，实测日志证明候选正常（`4 个路点`）；
  最终症状的实际触发条件是**驾驶锁**（`DriveLock`）开启，与上述两处缺陷叠加后表现为"完全没反应"。
- 临时加入的诊断日志（`[Click]` / `[Nav] 候选来源` / `[Xaero] 路点列表`）已全部移除；
  仅保留异常告警。

### [1.0.2] — 2026-11-10

### Fixed
- **修复 `mods.toml` 占位符未展开导致加载失败**：`noXaero` 开关把 Xaero 依赖块放进 Gradle 字符串后，
  其中的 `${mod_id}` 被当成字面量写进了产物，游戏报 `Invalid bare key: '${mod_id}'`。
  改为 Groovy 立即插值，两个变体的 `mods.toml` 均已无占位符（实测残留 = 0）。
- **修复换存档后导航失效**：新增 `WorldStateCleaner` / `ClientCacheCleaner`，
  在服务端停止、维度卸载、客户端断连时清空跨存档静态缓存
  （编队注册表、信标扫描、已加载终端集合、日志去重、HUD 路点/去重/车道缓存、Xaero region 缓存）。

### Changed
- `mods.toml` 的 `issueTrackerURL` 填入仓库 issues 地址。


### [1.1.93-beta] — 2026-11-10

### Changed（合规 / 只读化，无行为变化）
- 移除 Xaero **写入**通道：`XaeroReflect.addWaypoint` / `XaeroReflect.saveWorld`
  （后者曾对 `MinimapSession#worldManagerIO` 使用 `setAccessible` 并调用 `MinimapWorldManagerIO#saveWorld`）。
  该路径**没有任何 UI 入口与调用方**，删除后集成严格只读，与 README 附录 A 的声明一致。
  移除前版本留档：`_backup/XaeroReflect.java.before-readonly`。
- README（中/英）：新增顶部合规提示、13.2.1 反射调用清单、13.3 精确反射签名、14.1 与 MIT 对齐、14.3 去掉模板占位符。

---

### [1.1.92-beta] — 2026-11-10

### Changed
- 创造模式标签页图标：`mod_icon.png` 从 1128×1045 原图改为 **32×32 裁边版**（去掉四周白边后缩放并量化），
  图标在 16×16 的标签格里从约 55% 占满提升到 100%。

### Fixed
- 编译期源码目录中残留的 `*.opt-bak` 备份文件已移出 `src/`，统一放到 `_backup/`。

---

### [1.1.91-beta] — 2026-11-09

### Added
- 模组图标：`create_navigation_terminal_logo.png` + `neoforge.mods.toml` 的 `logoFile`。
- 隐藏物品 `create_navigation_terminal:mod_icon`，专用于创造标签页图标（不进任何物品栏）。

### Changed
- 创造标签页图标由「导航终端方块物品」改为 `mod_icon`。

---

### [1.1.90-beta] — 2026-11-09

### Changed（纯性能，行为等价）
- `isPonderScene()`：类名判定结果按 `Class` 身份缓存（原实现每帧多次 `getClass().getName().toLowerCase()`）。
- `pidLabel()`：Kp/Ki/Kd 字符串三槽缓存（原实现每帧 3 次 `String.format("%.2f")`）。
- HUD 距离标签：`distLabel(dist, dy)` + 上限 128 的 `(米, 高差)` 缓存（原实现每帧多次字符串拼接）。
- 缩放倍率标签、右键菜单三项文案：按数值 / 语言实例缓存。

实测（JDK 21 微基准，5 轮取中位数）：类名判定 17.9 → 3.1 ns、`String.format` 207.0 → 2.9 ns、距离标签 13.6 → 5.3 ns。

---

### [1.1.89-beta] — 2026-11-09

### Fixed
- 颜色模型加载：`ModelResourceLocation` 的 key 用物品 id（不带 `item/`），且 16 个颜色模型需先用
  `ModelEvent.RegisterAdditional` 排进烘焙队列，否则取不到 → 日志报「找不到基础模型」并退回单模型。

---

### [1.1.88-beta] — 2026-11-09

### Added
- **合成台用染料给导航点卡上色**：自定义配方 `create_navigation_terminal:card_dye`
  （`CardDyeRecipe`），复制输入卡的组件只改 `color`，航线/名字/只读标记全部保留。
- 移除右键上色的旧交互（与「用染料合成」的定位冲突）。

---

### [1.1.87-beta] — 2026-11-09

### Added
- 16 套卡面贴图（`waypoint_card_<colour>.png`）+ 16 个物品模型；
- `CardModelOverride`：按卡片 `color` 组件选择模型（`BakedModelWrapper` + `ItemOverrides`）。

### Removed
- 基于 tint 的染色渲染（改为整图替换，保留渐变与描边）。

---

### [1.1.86-beta] — 2026-11-09

### Added
- 导航点卡**染料分类**：`WaypointCardData` 增加 `color`（-1 = 未上色，0–15 = `DyeColor` id），
  codec 为 optional、旧存档兼容；提示框显示「分类：<颜色>」。

---

### [1.1.85-beta] — 2026-11-09

### Changed
- 导航点卡贴图由 `minecraft:item/paper` 换成专用 16×16 软盘贴图。

---

### [1.1.84-beta] — 2026-11-09

### Changed
- **思索内容只在装有 Ponderer 时加载**：`PonderSceneInstaller` 增加 `isLoaded("ponderer")` 前置判断；
- 移除原生 Ponder 集成（`NavPonderPlugin` / `NavPonderBridge` / `NavPonderScenes` / 演示面板 / 最小结构 nbt）
  与 `ponder-compileonly` 依赖。

---

### [1.1.83-beta] — 2026-11-09

### Fixed
- 「路点卡」思索场景补挂到方块组件上（此前只在卡物品上，导致终端思索列表少一条）；
- 原生 Ponder 的演示面板从屏幕右下移到左上，避免与居中文字重叠。

---

### [1.1.82-beta] — 2026-11-09

### Fixed
- 界面示意板绘制时机：`RenderGuiEvent.Post` → `ScreenEvent.Render.Post`，不再被思索界面的模糊/压暗层覆盖。

---

### [1.1.81-beta] — 2026-11-09

### Fixed
- Ponder 结构文件：位置从 `data/<ns>/ponder/` 改为 **`assets/<ns>/ponder/`**；
- 结构 NBT 布局对齐 Create（`size` 为 `TAG_List<Int>`、补 `DataVersion`）。
- 三个场景各自使用独立结构名（原先两个共用 `empty`，被 Ponder 以「组件+结构」为键覆盖，只剩 2 条）。

---

### [1.1.80-beta] 及更早

- 1.1.63–1.1.80：思索场景数据占位化（路由/目标/编队/候选点/地图底图全部改为演示数据）、
  面板示意渲染、Xaero 采样在镜像场景中跳过、日志限流等迭代。
- 更早的功能演进见 `README.md` 的「历史更新记录」表（早期内部编号）。

---

