# 设计文档索引

这里保存“为什么这样设计”和重要取舍。实现事实以代码和
[API_CONTRACT.md](../guides/API_CONTRACT.md) 为准。

| 文档 | 回答的问题 |
|---|---|
| [core-beliefs.md](core-beliefs.md) | 本项目长期坚持哪些设计判断 |
| [../DESIGN.md](../DESIGN.md) | 产品、状态、失败和安全的一般原则 |
| [../ARCHITECTURE.md](../../ARCHITECTURE.md) | 当前分层、数据流和信任边界 |
| [../adr/001-local-embedding-deepseek.md](../adr/001-local-embedding-deepseek.md) | 为什么本地 Embedding + 云端生成 |
| [../adr/002-dangerous-tools-default-off.md](../adr/002-dangerous-tools-default-off.md) | 为什么高风险工具默认关闭 |
| [../guides/AGENT_DESIGN.md](../guides/AGENT_DESIGN.md) | Agent 四层架构和已知坑 |

新增重要设计决策时：

1. 先写 ADR，记录替代方案和回退条件。
2. 更新本索引。
3. 同步 `ARCHITECTURE.md` 和相关测试。
