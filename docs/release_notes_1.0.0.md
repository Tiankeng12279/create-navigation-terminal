首个公开发布版本，版本号自此重新起算。

**Minecraft 1.21.1 · NeoForge 21.1.244+ · Java 21 · MIT**

---

## 下载哪个文件

| 文件 | 内容 |
|---|---|
| `create_navigation_terminal-1.0.0.jar` | 完整版（含 Xaero 只读互操作） |
| `create_navigation_terminal-noxaero-1.0.0.jar` | **jar 内不含任何 Xaero 互操作类**，`mods.toml` 也不声明 Xaero 依赖；其余功能一致 |

## 内容

- **导航终端（方块）**：三页签界面、距离渐变 / 到达开关 / 固定强度、三组参数预设
- **航线**：多站、站序调整、单次 / 循环、跨维度提示
- **导航点卡**：单卡 32 点、可命名、可设只读、合成台用 16 种染料上色分类
- **载具 HUD**：绑定座椅后显示；方位用相机坐标 + 相机姿态计算
- **编队频道**：同频共享目标、推送目标 / 航线、独占频道
- **思索**：6 个内置场景（检测到 Ponderer 时自动安装）
- **Xaero 集成**：可选、只读；不包含不分发其任何代码与资源

## 依赖

- 必需：Minecraft 1.21.1、NeoForge 21.1.244+、**Drive-By-Wire with Sable**
- 可选：Xaero's Minimap / World Map、Ponderer

## 校验

```
create_navigation_terminal-1.0.0.jar
  大小   408265 字节
  SHA256 b0de4057a818cb452c4105f20c2ae076fac8255212701ac19131ecabff126483

create_navigation_terminal-noxaero-1.0.0.jar
  大小   388389 字节
  SHA256 f0ede6ac63c98cab559a84682874457801e3eb0397c01823e2307c513c2cf078
```

## 合规

Xaero 集成是可选的只读消费：仅读取玩家本机 `xaero/world-map/` 缓存，并反射只读其公开 API 类；
**若 Xaero 作者提出异议，将立即移除该集成**并回退到无 Xaero 的实现（即本页的 `-noxaero` 变体）。
