# SSE 与 Agent 生命周期

> 状态:部分采用
> 来源:Agent SSE 联调与代码审查
> 最后验证:2026-11-10

## 问题

SSE 接口返回 200 只说明连接建立,不代表后台任务能正确结束。客户端断开后仍推理、自动重连导致任务重复、错误被当正常文本、死循环检测器未接入真实状态,都会造成费用和资源泄漏。

## 生命周期模型

```text
CREATED → RUNNING → SUCCEEDED
                  ↘ FAILED
                  ↘ CANCELLED
                  ↘ TIMED_OUT
```

每个终态只能进入一次。连接关闭与任务结束是两个不同事件:
- SSE 连接结束:传输层不再可用。
- Agent 任务结束:后台推理和工具执行全部停止并释放资源。

## 可复用规则

### 1. 每个请求拥有独立任务上下文

任务上下文至少包含 requestId、取消标志、截止时间、当前步骤、后台 Future/Disposable、资源清理函数。不要用共享可变 Agent 实例承载多个用户。

### 2. 结束事件有明确语义

建议统一事件:

| 事件 | 含义 |
|---|---|
| `step` | 中间步骤,可多次出现 |
| `error` | 不可恢复错误,包含稳定错误码 |
| `done` | 唯一终止事件,带 `status=success/failed/cancelled/timeout` |

发送终止事件后再关闭连接。前端收到 `done` 必须主动关闭 EventSource,避免自动重连。错误后不能再伪装成成功结束。

### 3. 断连必须传播取消

- 保存 `CompletableFuture`、Reactor `Disposable` 或线程任务句柄。
- `onTimeout/onError/onCompletion` 进入统一的幂等清理函数。
- 清理函数设置取消标志、取消上游订阅、终止后续工具调用并释放并发配额。
- 每个 Agent 循环和长耗时工具在安全点检查取消标志与截止时间。

注意:`onCompletion` 既可能是正常结束,也可能由客户端关闭触发,清理必须幂等,不能依赖单一回调判断业务成功。

### 4. 循环保护使用同一份状态

- think/act 记录工具和进展的检测器,必须与外层循环查询的是同一个实例。
- 设置最大步骤、总时长、单工具重复次数和累计模型调用次数。
- “无工具调用”通常应视为最终文本,而不是继续空转。
- 工具失败要返回结构化失败结果,不能只拼成普通字符串让模型误以为成功。

### 5. 前后端超时协调

浏览器的空闲超时必须大于后端正常步骤间隔,且小于用户可接受等待。前端停止按钮应调用真正的取消能力,仅关闭页面连接不等于可靠取消后台任务。

## 验收用例

- [ ] 正常任务只产生一个 `done(status=success)`。
- [ ] 模型异常产生 `error` 和失败终态,不会再显示成功。
- [ ] 客户端在第 1 步断开后,后台模型/工具调用在限定时间内停止。
- [ ] 超时后 Future/Disposable 被取消,并发计数归还。
- [ ] 同一工具连续调用达到阈值后停止。
- [ ] 两个并发用户的消息和状态不会串线。
- [ ] EventSource 不会因服务端正常关闭而自动重放任务。
- [ ] 慢步骤不会被过短的前端空闲计时误杀。

## 本项目现状

已采用:
- Agent 每次请求新建实例。
- SseEmitter 注册超时、完成和错误回调。
- Agent SSE 发送 `step/loop/error/done`,前端可据 `done` 关闭。

待处理:
- 后台 `CompletableFuture` 未保存,连接断开后不能可靠取消任务。
- SseEmitter 备用聊天接口未保存 Reactor 订阅句柄。
- 外层循环与 ToolCallAgent 当前使用不同 LoopGuard,外层检测看不到真实记录。
- 工具失败和 Agent 失败仍需要稳定的结构化错误语义。
- 前端停止操作需要真正传递到后端任务。

## 项目证据

- `src/main/java/com/yupi/aitravelplanner/agent/BaseAgent.java`
- `src/main/java/com/yupi/aitravelplanner/agent/ToolCallAgent.java`
- `src/main/java/com/yupi/aitravelplanner/agent/LoopGuard.java`
- `src/main/java/com/yupi/aitravelplanner/controller/AiController.java`

## Related

- [[security/api-security-baseline]]
- [[security/agent-tool-security]]
- [[engineering/ai-testing-and-delivery]]
