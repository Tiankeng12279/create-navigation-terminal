### 变更摘要（1.1.93-beta）

- 移除 Xaero **写入**通道（`XaeroReflect.addWaypoint` / `saveWorld`），集成严格只读；该路径此前无任何 UI 入口与调用方
- README（中/英）补全：顶部合规提示、13.2.1 只读反射调用清单、13.3 精确反射签名、14.1 与 MIT 许可对齐
- 新增 `CHANGELOG.md`，统一为单版本序列

### 本文件（-noxaero 变体）

- **jar 内不含任何 Xaero 互操作类**（`XaeroCacheReader` / `XaeroWaypointApi` / `XaeroMapCache` 均未打入）
- 同名 `XaeroReflect` / `XaeroMapSource` / `XaeroWaypointService` 为空实现（返回空列表 / 0 / null）
- `mods.toml` **不声明** `xaerominimap` / `xaeroworldmap` 依赖
- 其余功能与完整版一致；候选路点列表为空、地图退回网格示意

---

文件：`create_navigation_terminal-noxaero-1.1.93-beta.jar`
大小：388393 字节
SHA-256：`c5f60dea35e22921c00f587a76f787d5f112b0c6054ff59e6da4ccc1543d8d9b`
