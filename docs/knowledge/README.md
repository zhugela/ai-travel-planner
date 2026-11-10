# 可复用知识库

> 目标:把本项目踩过的坑沉淀成其他 AI 项目也能直接使用的规范、检查清单和模板。
>
> 最后更新:2026-11-10

## 一、怎么使用

1. 遇到问题时先按分类查规范,不要从课程笔记里逐篇翻。
2. 规范里的“验收清单”可以直接复制到任务或 PR。
3. 发生新事故后,先补知识卡片,再决定是否新增 ADR。
4. 每次新增或实质修改知识页,同步更新 [log.md](./log.md)。

状态说明:
- `已采用`:项目已经按该规范实现并验证。
- `部分采用`:已有基础实现,仍有明确缺口。
- `提议`:建议方案,尚未落地,不能当作当前能力对外承诺。

## 二、分类索引

### 安全

| 文档 | 当前状态 | 解决的问题 |
|---|---|---|
| [密钥生命周期与泄露响应](./security/secret-management.md) | 部分采用 | 密钥硬编码、泄露后的轮换与历史清理 |
| [Agent 高风险工具安全](./security/agent-tool-security.md) | 部分采用 | 终端、下载、抓取等工具的最小权限 |
| [安全文件路径](./security/safe-file-paths.md) | 提议 | 路径穿越、任意文件读写 |
| [API 安全基线](./security/api-security-baseline.md) | 提议 | 鉴权、限流、输入边界、日志脱敏 |

### RAG

| 文档 | 当前状态 | 解决的问题 |
|---|---|---|
| [本地 RAG 架构](./rag/local-rag-architecture.md) | 已采用/待调优 | 本地向量化、云端生成、检索质量与隐私边界 |

### Agent

| 文档 | 当前状态 | 解决的问题 |
|---|---|---|
| [SSE 与 Agent 生命周期](./agent/sse-agent-lifecycle.md) | 部分采用 | 死循环、断连、取消、错误与结束信号 |

### 工程化

| 文档 | 当前状态 | 解决的问题 |
|---|---|---|
| [AI 应用测试与交付](./engineering/ai-testing-and-delivery.md) | 部分采用 | “接口通了但页面不可用”、外部模型测试不稳定 |
| [知识沉淀工作流](./engineering/knowledge-capture-workflow.md) | 已采用 | 如何把一次问题变成长期可复用成果 |
| [引导式学习协作规范](./engineering/guided-learning-rules.md) | 已采用 | 如何教会用户而不是替用户完成 |

### 模板

| 模板 | 用途 |
|---|---|
| [知识卡片模板](./templates/knowledge-card-template.md) | 记录事故、结论、证据和复用边界 |
| [ADR 模板](./templates/adr-template.md) | 记录重要技术决策和替代方案 |

## 三、架构决策

- [ADR-001:本地 Embedding + DeepSeek 生成](../adr/001-local-embedding-deepseek.md)
- [ADR-002:高风险工具默认关闭](../adr/002-dangerous-tools-default-off.md)

## 四、收录标准

满足任一条件才写进这里:
- 同类问题可能在其他项目再次出现。
- 结论包含可执行规则或验收方法,不只是过程记录。
- 决策存在明显取舍,未来可能需要回看原因。

不收录:
- 一次性运行日志、临时地址、个人环境噪音。
- 只适用于某一行代码的修复细节。
- 未验证且没有标注“提议”的猜测。

## Related

- [[engineering/knowledge-capture-workflow]]
- [[templates/knowledge-card-template]]
- [[adr/README]]
