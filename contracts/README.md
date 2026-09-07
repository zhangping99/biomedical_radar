# 静态数据契约

- 当前主版本为 `1`，`schemaVersion` 采用 `1.x`。
- V1 到 V3 的 Article 字段只能兼容新增；删除、改名或改变语义必须提升主版本并提供迁移器。
- 前端遇到非 `1.x` 数据时拒绝加载并显示升级提示。
- `article.schema.json` 是稳定 Article 定义；`feed.schema.json` 同时校验列表文件和 manifest；`source-health.schema.json` 校验脱敏后的来源状态。
- 时间统一为 UTC ISO 8601；发布JSON固定使用UTF-8与LF换行，manifest中的文件哈希统一为对应发布字节的小写SHA-256，确保Windows与Linux检出后结果一致。

CI 使用 `frontend/scripts/validate-data.mjs` 校验契约样例和静态导出，并确认无效样例确实被拒绝。
