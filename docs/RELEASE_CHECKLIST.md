# 发布清单（CurseForge / Modrinth）

版本：**1.0.0** ｜ 目标：Minecraft **1.21.1** ｜ 加载器：**NeoForge 21.1.244+** ｜ 许可：**MIT License**

---

## 0. 两份产物（已就绪）

| 文件 | 大小 | SHA-256（前 16 位）| 说明 |
|---|---|---|---|
| `dist/create_navigation_terminal-1.0.0.jar` | 408265 B | `b0de4057a818cb45` | 完整版（含 Xaero 只读互操作）|
| `dist/create_navigation_terminal-noxaero-1.0.0.jar` | 388389 B | `f0ede6ac63c98cab` | **无 Xaero 互操作类** |

> 完整 SHA-256 见 `docs/file_changelog_1.0.0_full.md` / `docs/file_changelog_1.0.0_noxaero.md`。

重新构建：

```bat
gradlew.bat build                 :: 完整版
gradlew.bat build -PnoXaero       :: noxaero 变体
```

如需把 issue tracker 写进 `mods.toml`：

```bat
gradlew.bat build -PissueTracker=https://github.com/<你>/<仓库>/issues
```

---

## 1. 上传前自检（已实测通过）

| 检查 | 结果 | 依据 |
|---|---|---|
| jar 内是否含第三方资产 | 否 | `unzip -l` 无 Xaero/Create/Sable 贴图或类 |
| jar 内是否含残留文件（`.opt-bak` / `_backup` / `.md`）| 否 | `unzip -l` 过滤为空 |
| 许可证字段 | `MIT License` | jar 内 `META-INF/neoforge.mods.toml` |
| logo | 已声明 | `logoFile = "create_navigation_terminal_logo.png"` |
| noxaero 变体是否真的剔除 | 是 | 该 jar 内 `XaeroCacheReader` / `XaeroWaypointApi` / `XaeroMapCache` 计数 = 0 |
| noxaero 变体的依赖声明 | 无 Xaero | 其 `mods.toml` 仅有 neoforge / minecraft / drivebywire |

---

## 2. 项目级字段（两站通用）

| 字段 | 值 |
|---|---|
| 名称 | Create: Navigation Terminal（若担心暗示官方，可改 `Navigation Terminal (Create Addon)`）|
| 类型 | Mod |
| 加载器 | NeoForge |
| 游戏版本 | 1.21.1 |
| 许可证 | MIT License |
| 图标 | `docs/modrinth_icon_512.png`（512×512，已裁白边）|
| Issue tracker | 你的 GitHub Issues 地址（必需，当前 `mods.toml` 里为空）|
| 源码地址 | 你的 GitHub 仓库（两站都会显示“Source”入口）|
| 描述正文 | 中文：`docs/curseforge_body_zh.md` 或 `docs/modrinth_body_zh.md`<br>英文：对应 `_en.md` |

---

## 3. 依赖关系（必填，两站都要设）

| 目标项目 | 关系 | 说明 |
|---|---|---|
| **Drive-By-Wire with Sable**（Modrinth slug `drive-by-wire-sable`，MIT）| **Required** | `mods.toml` 里是 `drivebywire` required |
| Xaero's Minimap（`xaeros-minimap`，ARR）| Optional | 仅完整版需要挂 |
| Xaero's World Map（`xaeros-world-map`，ARR）| Optional | 同上 |
| Ponderer（MIT）| Optional | 未装则思索功能自动跳过 |

> **noxaero 文件不要挂 Xaero 关系**——该变体不声明也不读取 Xaero。
> CurseForge 需手动搜索目标项目；若某依赖在 CF 无对应页，用 “Other” 或仅在描述中给链接。

---

## 4. 文件级字段

| 字段 | 完整版 | noxaero 变体 |
|---|---|---|
| 游戏版本 | 1.21.1 | 1.21.1 |
| 加载器 | NeoForge | NeoForge |
| Release type | **Release**（1.0.0 为正式首发）| **Release** |
| Changelog | `docs/file_changelog_1.0.0_full.md` | `docs/file_changelog_1.0.0_noxaero.md` |
| 主文件 | 是 | 否（作为附加文件）|

---

## 5. 合规相关（发布时保留、勿删）

1. 项目描述**顶部**必须保留合规提示：Xaero 可选只读、不包含不分发其资产、**异议即移除**、提供 noxaero 变体；
2. 描述中说明 Xaero 为 **ARR**，集成属善意互操作；
3. 不要在任何地方声称“与 Xaero 官方合作/授权”；
4. 若收到权利方诉求：先下架完整版文件，只保留 noxaero 变体，再评估移除互操作代码。

---

## 6. 发布后验证（真实执行，非推测）

1. 两站项目页能否正常打开、图标是否方形无拉伸；
2. 依赖卡片是否显示 Drive-By-Wire（Required）；
3. 从项目页下载 jar，比对 SHA-256 与第 0 节一致；
4. 干净实例（1.21.1 + NeoForge 21.1.244 + Drive-By-Wire）装 jar 启动，进世界放方块、开界面、给卡上色；
5. 卸掉 Drive-By-Wire 启动，确认只是线缆功能缺失、不崩。

> 上述 1–3 需你在浏览器侧确认；4–5 需要真实游戏环境，我无法代替执行，故此处只列步骤。

---

## 7. 未完成 / 待你提供

- [ ] GitHub 仓库地址 → 用于 `issueTrackerURL` 与“Source”链接（我可用 `-PissueTracker=` 重新出包）
- [ ] 确认 Drive-By-Wire 在 CurseForge 上的项目页是否存在（CF 搜索接口被 Cloudflare 拦，我无法核）
- [ ] 决定项目显示名是否保留 `Create:` 前缀
