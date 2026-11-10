# 架构决策记录(ADR)

> ADR 记录“为什么这样选”,不重复描述全部实现细节。

| ID | 决策 | 状态 | 日期 |
|---|---|---|---|
| [ADR-001](./001-local-embedding-deepseek.md) | 本地 Embedding + DeepSeek 生成 | 已接受 | 2026-11-10 |
| [ADR-002](./002-dangerous-tools-default-off.md) | 高风险 Agent 工具默认关闭 | 已接受,待完全落地 | 2026-11-10 |

编号规则:
- 新决策按三位数递增,已有编号不重排。
- 被替代的 ADR 保留原文,标记“已替代”并链接新 ADR。
- 只记录会长期影响架构、安全或维护成本的决定。

## Related

- [[knowledge/README]]
- [[knowledge/templates/adr-template]]
