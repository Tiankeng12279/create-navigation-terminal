### 变更摘要（1.0.0 —— 首个公开发布）

- 导航终端：距离渐变 / 到达开关 / 固定强度三种输出；强度上限 0–15、覆盖距离 8–512、到达半径 1–64；三组参数预设
- 航线：多站、站序调整、单次 / 循环；跨维度输出满信号并提示
- 距离仅水平，服务端每 8 tick 计算并同步（界面与红石同源）
- 导航点卡：单卡 32 点、可命名、可设只读；合成台用 16 种染料上色分类
- 载具 HUD：绑定座椅后显示，方位用相机坐标 + 相机姿态计算
- 编队频道：同频共享目标、推送目标 / 航线、独占频道
- 思索：6 个内置场景，检测到 Ponderer 时自动安装

### 本文件（-noxaero 变体）

- **jar 内不含任何 Xaero 互操作类**（`XaeroCacheReader` / `XaeroWaypointApi` / `XaeroMapCache` 均未打入）
- 同名 `XaeroReflect` / `XaeroMapSource` / `XaeroWaypointService` 为空实现
- `mods.toml` **不声明** `xaerominimap` / `xaeroworldmap` 依赖
- 其余功能与完整版一致；候选路点列表为空、地图退回网格示意

---

文件：`create_navigation_terminal-noxaero-1.0.0.jar`
大小：388389 字节
SHA-256：`f0ede6ac63c98cab559a84682874457801e3eb0397c01823e2307c513c2cf078`
