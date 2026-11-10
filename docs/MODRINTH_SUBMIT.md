# Modrinth 提交清单（1.0.0）

- 版本：**1.0.0**（正式发布，非 beta）
- 游戏版本：**1.21.1** ｜ 加载器：**NeoForge**
- 许可：**MIT License**
- 源码 / Issue：`https://github.com/Tiankeng12279/create-navigation-terminal`
- 网络实测：`api.modrinth.com` 可达（HTTP 301）、`modrinth.com` 可达（HTTP 200）

---

## 一、项目字段（New project）

| 字段 | 值 |
|---|---|
| Name | `Create: Navigation Terminal` |
| Slug | `create-navigation-terminal`（若被占用：`create-navigation-terminal-hzldm`）|
| Project type | `Mod` |
| Summary（EN，≤256 字符）| `Distance-based redstone navigation for Create-style vehicles: pick a waypoint, output 0-15 redstone, and get a cockpit HUD while seated. Optional read-only Xaero integration.` |
| Summary（ZH，≤256 字符）| `按距离输出红石信号的导航终端：选定路点后输出 0-15 红石，坐上绑定座椅还有战斗机式导航 HUD。Xaero 集成可选、只读。` |
| Categories | `technology`、`utility`、`transportation`（三个都是有效 slug）|
| Client side | `required` |
| Server side | `required` |
| License | `MIT` |
| Icon | `docs/modrinth_icon_512.png`（512×512）|
| Body | 中文：`docs/modrinth_body_zh.md`；英文：`docs/modrinth_body_en.md`（二选一，或先英文后追加中文）|

> Body 顶部已含合规提示（Xaero 可选只读、异议即移除、提供 `-noxaero` 变体），**不要删**。

---

## 二、版本字段（Create version）

| 字段 | 值 |
|---|---|
| Name | `1.0.0` |
| Version number | `1.0.0` |
| Release channel | `release` |
| Loaders | `NeoForge` |
| Game versions | `1.21.1` |
| Changelog | 见下方「三、版本 Changelog」|
| Files | 见下方「四、文件」|

### 依赖（Dependencies）

| 项目 | 关系 | 依据 |
|---|---|---|
| Drive-By-Wire with Sable（`drive-by-wire-sable`，项目 id `KsqvI0hD`，MIT）| **Required** | `mods.toml` 声明 `drivebywire` required |
| Xaero's Minimap（`xaeros-minimap`，id `1bokaNcj`，ARR）| Optional | 仅完整版需要 |
| Xaero's World Map（`xaeros-world-map`，id `NcUtCpym`，ARR）| Optional | 仅完整版需要 |

> **`-noxaero` 文件不要挂 Xaero 依赖** —— 该变体不声明也不读取 Xaero。

---

## 三、版本 Changelog（可直接粘贴）

```markdown
首个公开发布版本，版本号自此重新起算。

- 导航终端（方块）：三页签界面；距离渐变 / 到达开关 / 固定强度；强度上限 0–15、覆盖距离 8–512、到达半径 1–64；三组参数预设
- 航线：多站、站序调整、单次 / 循环；跨维度输出满信号并提示
- 距离仅水平，服务端每 8 tick 计算并同步（界面与红石同源）
- 导航点卡：单卡 32 点、可命名、可设只读；合成台用 16 种染料上色分类
- 载具 HUD：绑定座椅后显示，方位用相机坐标 + 相机姿态计算
- 编队频道：同频共享目标、推送目标 / 航线、独占频道
- 思索：6 个内置场景（检测到 Ponderer 时自动安装）
- Xaero 集成可选、只读；另提供 `-noxaero` 变体（jar 内不含任何互操作类）

构建：`gradlew.bat build`（完整版）/ `gradlew.bat build -PnoXaero`（无 Xaero 变体）
```

### 英文版（Modrinth 提交用）

```markdown
First public release — version numbering restarts at 1.0.0.

- Navigation Terminal (block): three-tab GUI; distance ramp / arrival switch / fixed power; max power 0-15, coverage range 8-512, arrival radius 1-64; three parameter presets
- Routes: multi-stop, reorderable, one-way or loop; cross-dimension targets emit full signal plus a hint
- Distance is horizontal only and computed server-side every 8 ticks (GUI and redstone share one source)
- Waypoint Card: up to 32 points per card, nameable, read-only lockable; dye it with any of 16 dyes in a crafting table to sort routes
- Vehicle HUD: shown while seated on a bound seat; bearing uses camera position + camera attitude
- Fleet channel: share targets within a channel, push target / route, or go solo
- Ponder: 6 built-in scenes (auto-installed when Ponderer is present)
- Xaero integration is optional and read-only; a `-noxaero` variant ships with no interop classes at all

Build: `gradlew.bat build` (full) / `gradlew.bat build -PnoXaero` (no-Xaero variant)
```

---

## 四、文件（同一版本下挂两个文件）

| 文件 | 大小 | 主文件 |
|---|---|---|
| `create_navigation_terminal-1.0.0.jar` | 408,265 B | **是** |
| `create_navigation_terminal-noxaero-1.0.0.jar` | 388,389 B | 否 |

本地路径：

```
dist/create_navigation_terminal-1.0.0.jar
dist/create_navigation_terminal-noxaero-1.0.0.jar
```

完整 SHA-256 见 `docs/file_changelog_1.0.0_full.md` / `docs/file_changelog_1.0.0_noxaero.md`。

---

## 五、发布后检查

1. 项目页图标是方形、无拉伸；
2. 依赖卡片显示 Drive-By-Wire（Required）；
3. 两个文件都能下载，大小与上表一致；
4. 版本页的 game version 显示 1.21.1 / NeoForge；
5. 从 Modrinth 下载 jar，比对 SHA-256。

---

## 六、可选：用 API 创建（需要 PAT）

网络可达，但**创建项目/版本必须带 Modrinth PAT**（`Authorization: <token>`）。有 token 时可直接跑：

```bash
# 1) 建项目（先传 body/icon 可后补）
curl -X POST https://api.modrinth.com/v2/project \
  -H "Authorization: $MODRINTH_TOKEN" -H "Content-Type: application/json" \
  -d '{
    "slug": "create-navigation-terminal",
    "title": "Create: Navigation Terminal",
    "description": "Distance-based redstone navigation for Create-style vehicles: pick a waypoint, output 0-15 redstone, and get a cockpit HUD while seated.",
    "categories": ["technology", "utility", "transportation"],
    "client_side": "required",
    "server_side": "required",
    "project_type": "mod",
    "initial_versions": [],
    "license_id": "MIT",
    "is_draft": true
  }'

# 2) 传版本（注意 -F 的 files 可给多个）
curl -X POST https://api.modrinth.com/v2/version \
  -H "Authorization: $MODRINTH_TOKEN" \
  -F "data={\"project_id\":\"<项目ID>\",\"name\":\"1.0.0\",\"version_number\":\"1.0.0\",
       \"changelog\":\"见第三节\",\"dependencies\":[{\"project_id\":\"KsqvI0hD\",\"dependency_type\":\"required\"}],
       \"game_versions\":[\"1.21.1\"],\"version_type\":\"release\",\"loaders\":[\"neoforge\"],
       \"featured\":true};type=application/json" \
  -F "create_navigation_terminal-1.0.0.jar=@dist/create_navigation_terminal-1.0.0.jar" \
  -F "create_navigation_terminal-noxaero-1.0.0.jar=@dist/create_navigation_terminal-noxaero-1.0.0.jar"
```

> Token 只从环境变量读，不写进任何文件；用完在 <https://modrinth.com/settings/account> 撤销。
