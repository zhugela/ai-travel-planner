# 工程 SOP 索引

这些流程把关键原则变成可重复动作。按任务选择，不要求每次全部执行。

| SOP | 何时使用 | 产物 |
|---|---|---|
| [layered-domain-architecture.md](layered-domain-architecture.md) | 新增模块、拆分职责、审查依赖方向 | 领域边界、接口和测试位置 |
| [encode-knowledge-into-repo.md](encode-knowledge-into-repo.md) | 发生事故、做重要取舍、反复解释同一规则 | 知识卡片、ADR、入口链接 |
| [observability-feedback-loop.md](observability-feedback-loop.md) | 排查稳定性、引入指标、做性能优化 | 日志/指标/trace 与反馈闭环 |
| [chrome-devtools-validation-loop.md](chrome-devtools-validation-loop.md) | 声称“前端完成”前、SSE/UI 联调 | Console/Network/DOM/截图证据 |

## 使用原则

1. 先用最小流程解决问题。
2. 每个 SOP 结束时留下仓库内的证据。
3. 流程本身也要清理；连续多轮无收益的步骤可以移除并做对比。
