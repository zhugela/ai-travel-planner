# RELIABILITY.md — 可靠性约束

> 目标不是承诺生产级 SLA，而是明确学习项目当前能保证什么、坏了怎么处理。

## 1. 当前可靠性与目标

| 能力 | 当前边界 | 目标 |
|---|---|---|
| 普通问答 | 依赖 DeepSeek 和网络 | 可重试失败、明确错误，不返回空 |
| 报告 | 依赖结构化模型输出 | 解析失败可被发现，不静默丢字段 |
| RAG | 依赖本地模型和内存向量库 | 启动失败可见；无命中稳定兜底 |
| Agent SSE | 依赖模型和工具 | 每轮有上限、超时、done/error |
| 文件记忆 | 单机文件 | 路径校验、并发策略、损坏恢复 |
| 前端 | 独立仓库 | 断连关闭连接、可取消、无明显重复请求 |

## 2. 超时与上限

建议所有外部边界都有显式值：

| 项 | 当前/建议 |
|---|---|
| Agent SSE emitter | 当前 300 秒 |
| 普通 SseEmitter | 当前 180 秒 |
| Agent maxSteps | 当前 10；成品 Agent 配置可调整 |
| LoopGuard | 同工具 3 次 / 无进展 5 次 |
| HTTP 工具 | 必须设置连接和读取超时 |
| 工具输出 | 有长度上限，当前并非所有工具统一 |
| 输入长度 | 当前缺少统一上限 |

数字不是越大约好。修改时要同步测试和接口契约。

## 3. SSE 生命周期

成功路径：

```text
connect -> step* -> done -> client close -> emitter complete
```

失败路径：

```text
connect -> error -> done?（按契约） -> client close -> release
```

循环路径：

```text
connect -> step* -> loop -> error/state ERROR -> done -> release
```

必须验证：

- 客户端断连后没有继续无限调用工具。
- 同一请求没有因 EventSource 自动重连而重复执行。
- 超时、完成、异常三个回调都已注册。
- `done` 不会被当成普通文本吞掉。

## 4. 外部依赖故障

### DeepSeek 不可用

- 返回明确错误或业务兜底。
- 不把第三方完整错误和密钥写入用户响应。
- 重试必须限制次数，避免放大费用。

### 本地 Embedding 不可用

- 启动日志明确指出模型路径或文件问题。
- 不静默变成“空知识库仍回答”。
- RAG 健康状态应可查询或通过测试确认。

### 搜索能力不可用

- 不注册搜索工具。
- System prompt 说明该能力不可用。
- 模型不得用其他工具拼凑出“已完成搜索”的结果。

## 5. 数据恢复

### 会话文件损坏

- 单个会话反序列化失败不能拖垮其他会话。
- 失败应记录会话 ID 摘要，不记录完整隐私内容。
- 可选择备份后重建空会话，并告知用户上下文已丢失。

### 向量库重建

- 启动时从 `resources/document/*.md` 重建。
- 文档加载失败必须让 RAG 状态可见。
- 当前不承诺跨重启持久化索引。

## 6. 运行排查

```powershell
# 端口
Get-NetTCPConnection -LocalPort 8123 -ErrorAction SilentlyContinue

# 测试
.\mvnw.cmd test -Dtest=LoopGuardTest

# 运行日志
Get-Content .\tmp\run.log -Tail 100
```

日志中如果出现真实 Key，先按泄露处理，不能直接贴进 Issue 或聊天。
