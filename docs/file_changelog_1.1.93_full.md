### 变更摘要（1.1.93-beta）

- 移除 Xaero **写入**通道（`XaeroReflect.addWaypoint` / `saveWorld`），集成严格只读；该路径此前无任何 UI 入口与调用方
- README（中/英）补全：顶部合规提示、13.2.1 只读反射调用清单、13.3 精确反射签名、14.1 与 MIT 许可对齐
- 新增 `CHANGELOG.md`，统一为单版本序列

### 本文件（完整版）

- 含 Xaero 只读互操作：读取本机 `xaero/world-map/` 缓存 + 反射只读其公开 API 类
- `mods.toml` 声明 `xaerominimap` / `xaeroworldmap` 为 **optional**
- 不含任何 Xaero 代码、类文件、反编译产物或资源文件；不写入 Xaero 数据

---

文件：`create_navigation_terminal-1.1.93-beta.jar`
大小：408271 字节
SHA-256：`7351c9ced694e7b3c6000d9a14a2bb2b384002f4c0a37f0a66618b2cf9c48608`
