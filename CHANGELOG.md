# Changelog

本文件用**单一版本序列**记录本模组的变更，`README.md` 只保留摘要。

- 版本号来源：`gradle.properties` 的 `mod_version`
- 版本格式：`<major>.<minor>.<patch>[-beta]`
- `1.1.x` 系列之后不再使用 README 历史表中的 `2.x` 编号（那批编号来自早期内部迭代，仅作留档）

---

## [1.1.93-beta] — 2026-11-10

### Changed（合规 / 只读化，无行为变化）
- 移除 Xaero **写入**通道：`XaeroReflect.addWaypoint` / `XaeroReflect.saveWorld`
  （后者曾对 `MinimapSession#worldManagerIO` 使用 `setAccessible` 并调用 `MinimapWorldManagerIO#saveWorld`）。
  该路径**没有任何 UI 入口与调用方**，删除后集成严格只读，与 README 附录 A 的声明一致。
  移除前版本留档：`_backup/XaeroReflect.java.before-readonly`。
- README（中/英）：新增顶部合规提示、13.2.1 反射调用清单、13.3 精确反射签名、14.1 与 MIT 对齐、14.3 去掉模板占位符。

---

## [1.1.92-beta] — 2026-11-10

### Changed
- 创造模式标签页图标：`mod_icon.png` 从 1128×1045 原图改为 **32×32 裁边版**（去掉四周白边后缩放并量化），
  图标在 16×16 的标签格里从约 55% 占满提升到 100%。

### Fixed
- 编译期源码目录中残留的 `*.opt-bak` 备份文件已移出 `src/`，统一放到 `_backup/`。

---

## [1.1.91-beta] — 2026-11-09

### Added
- 模组图标：`create_navigation_terminal_logo.png` + `neoforge.mods.toml` 的 `logoFile`。
- 隐藏物品 `create_navigation_terminal:mod_icon`，专用于创造标签页图标（不进任何物品栏）。

### Changed
- 创造标签页图标由「导航终端方块物品」改为 `mod_icon`。

---

## [1.1.90-beta] — 2026-11-09

### Changed（纯性能，行为等价）
- `isPonderScene()`：类名判定结果按 `Class` 身份缓存（原实现每帧多次 `getClass().getName().toLowerCase()`）。
- `pidLabel()`：Kp/Ki/Kd 字符串三槽缓存（原实现每帧 3 次 `String.format("%.2f")`）。
- HUD 距离标签：`distLabel(dist, dy)` + 上限 128 的 `(米, 高差)` 缓存（原实现每帧多次字符串拼接）。
- 缩放倍率标签、右键菜单三项文案：按数值 / 语言实例缓存。

实测（JDK 21 微基准，5 轮取中位数）：类名判定 17.9 → 3.1 ns、`String.format` 207.0 → 2.9 ns、距离标签 13.6 → 5.3 ns。

---

## [1.1.89-beta] — 2026-11-09

### Fixed
- 颜色模型加载：`ModelResourceLocation` 的 key 用物品 id（不带 `item/`），且 16 个颜色模型需先用
  `ModelEvent.RegisterAdditional` 排进烘焙队列，否则取不到 → 日志报「找不到基础模型」并退回单模型。

---

## [1.1.88-beta] — 2026-11-09

### Added
- **合成台用染料给导航点卡上色**：自定义配方 `create_navigation_terminal:card_dye`
  （`CardDyeRecipe`），复制输入卡的组件只改 `color`，航线/名字/只读标记全部保留。
- 移除右键上色的旧交互（与「用染料合成」的定位冲突）。

---

## [1.1.87-beta] — 2026-11-09

### Added
- 16 套卡面贴图（`waypoint_card_<colour>.png`）+ 16 个物品模型；
- `CardModelOverride`：按卡片 `color` 组件选择模型（`BakedModelWrapper` + `ItemOverrides`）。

### Removed
- 基于 tint 的染色渲染（改为整图替换，保留渐变与描边）。

---

## [1.1.86-beta] — 2026-11-09

### Added
- 导航点卡**染料分类**：`WaypointCardData` 增加 `color`（-1 = 未上色，0–15 = `DyeColor` id），
  codec 为 optional、旧存档兼容；提示框显示「分类：<颜色>」。

---

## [1.1.85-beta] — 2026-11-09

### Changed
- 导航点卡贴图由 `minecraft:item/paper` 换成专用 16×16 软盘贴图。

---

## [1.1.84-beta] — 2026-11-09

### Changed
- **思索内容只在装有 Ponderer 时加载**：`PonderSceneInstaller` 增加 `isLoaded("ponderer")` 前置判断；
- 移除原生 Ponder 集成（`NavPonderPlugin` / `NavPonderBridge` / `NavPonderScenes` / 演示面板 / 最小结构 nbt）
  与 `ponder-compileonly` 依赖。

---

## [1.1.83-beta] — 2026-11-09

### Fixed
- 「路点卡」思索场景补挂到方块组件上（此前只在卡物品上，导致终端思索列表少一条）；
- 原生 Ponder 的演示面板从屏幕右下移到左上，避免与居中文字重叠。

---

## [1.1.82-beta] — 2026-11-09

### Fixed
- 界面示意板绘制时机：`RenderGuiEvent.Post` → `ScreenEvent.Render.Post`，不再被思索界面的模糊/压暗层覆盖。

---

## [1.1.81-beta] — 2026-11-09

### Fixed
- Ponder 结构文件：位置从 `data/<ns>/ponder/` 改为 **`assets/<ns>/ponder/`**；
- 结构 NBT 布局对齐 Create（`size` 为 `TAG_List<Int>`、补 `DataVersion`）。
- 三个场景各自使用独立结构名（原先两个共用 `empty`，被 Ponder 以「组件+结构」为键覆盖，只剩 2 条）。

---

## [1.1.80-beta] 及更早

- 1.1.63–1.1.80：思索场景数据占位化（路由/目标/编队/候选点/地图底图全部改为演示数据）、
  面板示意渲染、Xaero 采样在镜像场景中跳过、日志限流等迭代。
- 更早的功能演进见 `README.md` 的「历史更新记录」表（早期内部编号）。

---

