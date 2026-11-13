# FRONTEND.md — 前端边界与验收

> 前端是独立仓库，当前后端仓库只保存接口契约、页面边界和验收标准。
> 实现细节以 [guides/FRONTEND_SPEC.md](guides/FRONTEND_SPEC.md) 为准。

## 1. 技术边界

- Vue 3 + Vite + Element Plus。
- 仅使用 JavaScript，不引入 TypeScript。
- 页面组件不直接拼后端 URL，所有请求走 `src/api/`。
- 开发环境通过 Vite 代理 `/api`，不写死 `http://localhost:8123`。

## 2. 页面职责

| 页面 | 主要能力 | 当前后端 |
|---|---|---|
| `ChatView.vue` | 多轮问答、SSE 打字机 | `/api/travel/chat`、`/api/travel/chat/sse` |
| `ReportView.vue` | 结构化逐日行程与预算 | `/api/travel/report` |
| `RagView.vue` | 本地知识库问答与未命中态 | `/api/travel/rag-chat` |
| `AgentView.vue` | 步骤流、工具结果、终止信号 | `/api/agent/sse` |

## 3. 状态设计

| 状态 | 所属位置 | 规则 |
|---|---|---|
| `conversationId` | Pinia / localStorage | 续聊时回传；多用户上线前不能视为授权凭证 |
| 输入框、loading | 组件 local state | 页面卸载即丢弃 |
| 流式文本 | 组件 local state | 不写入全局 store |
| Agent 步骤列表 | `AgentView` | 按 `Step N:` 聚合多行 data |
| RAG `matched` | 页面 local state | `false` 显示“未命中”，不渲染成普通回答 |

## 4. SSE 铁律

1. 聊天和 Agent 使用 GET + `EventSource`。
2. 参数使用 `URLSearchParams`，不要再叠加 `encodeURIComponent`。
3. 收到 Agent `done` 后立即 `close()` 并复位 loading。
4. 组件 `onUnmounted` 必须关闭所有 EventSource。
5. 断线、关闭、错误和重连要有明确 UI 状态。
6. Agent 的 `step` 多行 data 按 `Step N:` 聚合，不能逐行覆盖。

协议事实以 [guides/API_CONTRACT.md](guides/API_CONTRACT.md) 为准。

## 5. 必须覆盖的状态

- 空输入、超长输入、中文参数。
- 首次请求、流式中、正常结束、用户取消。
- RAG 命中与未命中。
- 报告 `days` 为空、预算字段缺失、长文本。
- Agent `step`、`loop`、`error`、`done`。
- 网络失败、后端 4xx/5xx、请求重复。

## 6. 验收闭环

接口返回 200 不代表前端完成。按
[guides/ACCEPTANCE_CHECKLIST.md](guides/ACCEPTANCE_CHECKLIST.md) 依次验证：

1. 接口连通。
2. 字段和事件契约。
3. 浏览器 Console / Network / 渲染状态。
4. 真实点击的端到端体验与移动端宽度。

任何“前端完成”声明都必须包含 Console 和真实页面操作证据。
