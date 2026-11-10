# API 契约表

> **用途**:前后端对接的唯一事实来源。改接口必须同步改这份文档。
> 前端仓库:`D:\code2\ai-travel-planner-frontend`
> 后端端口:8123,统一前缀 `/api`(context-path)
>
> 最后更新:2026-11-10

---

## 一、端点总览

| # | 方法 | 路径 | 类型 | 前端调用方 |
|---|---|---|---|---|
| 1 | POST | `/api/travel/chat` | JSON | (未使用) |
| 2 | POST | `/api/travel/report` | JSON | `ReportView.vue` |
| 3 | GET | `/api/travel/chat/sse` | SSE | `ChatView.vue` |
| 4 | GET | `/api/agent/sse` | SSE | `AgentView.vue` |
| 5 | POST | `/api/travel/rag-chat` | JSON | `RagView.vue` |

统一响应结构(非 SSE):

```json
{ "code": 0, "data": {...}, "message": "ok" }
```

`code !== 0` 表示业务错误,前端 `api/request.js` 拦截器会抛 `ElMessage.error`。

---

## 二、POST /api/travel/report

结构化行程报告。**注意:不使用对话式追问,信息缺失时用默认值补齐。**

**请求**

```json
{ "message": "成都 5 天，带爸妈，节奏慢", "conversationId": "可选" }
```

**响应 data**

```json
{
  "conversationId": "uuid",
  "title": "成都 5 天慢节奏带爸妈游",
  "summary": "以市区低强度景点为主线，每天 1-2 个核心点",
  "days": [
    {
      "day": 1,
      "theme": "抵达成都·市区适应",
      "morning": "抵达，地铁/网约车入住春熙路附近（网约车约 60 元）",
      "afternoon": "天府广场、太古里缓步逛",
      "evening": "春熙路一带川菜馆（人均 80 元）"
    }
  ],
  "budget": {
    "total": 5000.0,
    "transport": 1400.0,
    "hotel": 1600.0,
    "food": 1150.0,
    "tickets": 850.0,
    "note": "按 2 人、4 晚舒适型酒店计算，不含往返大交通"
  },
  "assumptions": ["默认 2 人出行", "默认 11 月出发"],
  "tips": ["熊猫基地早上 7:30 前入园人少", "都江堰动车票提前一天买"]
}
```

**契约约束**

| 字段 | 约束 |
|---|---|
| `days[].day` | 从 1 连续递增 |
| `days[].morning/afternoon/evening` | 必须是具体安排(含景点/交通/餐饮),不能是"自由活动" |
| `budget` | 四项之和 ≈ `total` |
| `assumptions` | 信息残缺时必须非空 |
| `tips` | 3-5 条真实注意事项 |

**变更记录**:2026-11-10 由 `suggestions: string[]` 改为 `days/budget/assumptions/tips`。
旧字段 `suggestions` 已移除,前端同步改造。

---

## 三、GET /api/travel/chat/sse

**请求参数**

| 参数 | 必填 | 说明 |
|---|---|---|
| `message` | 是 | 用户消息 |
| `chatId` | 否 | 会话 ID,用于多轮上下文 |

**响应**:`text/event-stream`

```
data:杭州

data:是

data:一座

...
```

**契约约束**

| 项 | 说明 |
|---|---|
| 事件名 | 默认 `message`(不带 `event:` 行) |
| 结束信号 | 连接关闭(无显式 done 帧) |
| 客户端处理 | 收到无 data 的 error 时**必须 `close()`**,否则 EventSource 自动重连导致重复请求 |

**编码坑**:`URLSearchParams` 自带编码,不要再套 `encodeURIComponent`(会双重编码成 `%2520`)。

---

## 四、GET /api/agent/sse

**请求参数**

| 参数 | 必填 |
|---|---|
| `message` | 是 |

**响应事件**

| 事件名 | 内容 | 说明 |
|---|---|---|
| `step` | `Step N:\n<内容>` | 思考或工具执行 |
| `loop` | `[LoopGuard] ...` | 死循环警告 |
| `error` | 错误描述 | 异常 |
| `done` | `finished` | **必须收到,收到后前端 close** |

**关键坑:多行 data 会被拆成多次回调**

后端推送的 `step` 内含换行:

```
event:step
data:Step 1:
data:【工具执行】getWeather => "..."
data:
data:
```

按 SSE 规范,**每行 `data:` 触发一次回调**,且行间无分隔符。前端若逐条处理会丢失内容。

**前端必须按 `Step N:` 重新分块聚合**:

```js
function handleStep(chunk) {
  if (buffer.length && /^\s*Step\s*\d+\s*:/.test(chunk)) flush()
  buffer.push(chunk)
}
```

**工具调用没有独立的 `tool` 事件**,信息嵌在 `step` 文本里,格式:
`【工具执行】<工具名> => <结果>`

### 能力门控(2026-11-10 新增)

**不可用的工具不会出现在模型的工具列表里。**

| 工具 | 依赖 | 未配置时的行为 |
|---|---|---|
| 联网搜索 | `SEARCH_API_KEY` | 不注册给模型 |
| 其余 7 个 | 无外部密钥 | 恒可用 |

未配置的能力会写进 system prompt 的「当前不可用的能力」段落,
模型被要求**直接说明做不到**,而不是用别的工具拼凑。

**影响前端**:如果快捷提示里写了"对比机票均价"这类依赖联网搜索的任务,
在没配 key 时模型会直接拒绝。前端应据此调整提示文案或标记为不可用。

---

## 五、POST /api/travel/rag-chat

**请求**

```json
{ "question": "杭州西湖需要门票吗？", "conversationId": "可选" }
```

**响应 data**

```json
{
  "answer": "根据知识库…",
  "matched": true,
  "conversationId": null
}
```

**契约约束**

| 项 | 说明 |
|---|---|
| `matched=false` | 知识库无命中,`answer` 为固定兜底文案 `暂无相关旅游攻略信息` |
| 知识库范围 | 仅 `resources/document/*.md`(当前 3 份,约 4KB) |
| 检索参数 | topK=4,相似度阈值=0.35 |

**注意**:知识库很小,很多问题会 `matched=false`。这是**正确行为**,不是 bug。

---

## 六、变更流程

改接口时:

1. 先改本文件
2. 再改后端
3. 再改前端
4. 走 `docs/ACCEPTANCE_CHECKLIST.md` 四层验收
