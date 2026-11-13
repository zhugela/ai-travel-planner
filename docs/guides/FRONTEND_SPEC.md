# AI 旅行规划助手 — 前端开发规范

> **适用项目**: `ai-travel-planner-frontend` (Vue 3 + Vite + Element Plus, 无 TypeScript)
> **最后更新**: 2026-08-04
> **关联文档**: REQUIREMENTS.md / SCHEMA.md / TASKS.md / TESTING_SPEC.md / 学习笔记-Ch 09
> **配套文档**: 后端接口文档见各 Controller (`AiController` / `AgentController` / `TravelRagController`)

---

## 一、技术栈与硬性约束

| 项 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 框架 | Vue 3 | 3.x | Composition API (`<script setup>`) |
| 构建 | Vite | 5.x | dev 端口 **3000** |
| UI 库 | Element Plus | 2.x | 全站组件来源 |
| HTTP | Axios | 1.x | 统一封装，见第三章 |
| 状态 | Pinia | 2.x | 仅跨页面共享状态用 |
| 流式 | EventSource / fetch stream | — | SSE 对接，见第五章 |

**严禁**:
- ❌ 引入 TypeScript（课程约束，项目保持纯 JS）
- ❌ 引入 Vue 2 / 其他 UI 库混用
- ❌ 在组件里直接 `fetch` 拼 URL（必须走 axios 封装）
- ❌ 后端 API Key / 敏感配置出现在前端代码

---

## 二、前端项目目录结构

```
ai-travel-planner-frontend/
├── index.html
├── vite.config.js               ← dev 代理(见第八章)
├── package.json
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── api/                     ← 所有后端请求都在这里
│   │   ├── request.js           ← axios 实例 + 拦截器(唯一出口)
│   │   ├── travel.js            ← /travel/* 接口封装
│   │   └── agent.js             ← /agent/* 接口封装
│   ├── utils/
│   │   └── sse.js               ← SSE 连接工具(打字机/事件协议)
│   ├── views/                   ← 页面
│   │   ├── ChatView.vue         ← 多轮对话 + SSE 打字机
│   │   ├── ReportView.vue       ← 结构化旅行报告
│   │   ├── AgentView.vue        ← Agent 工作台(步骤/工具可视化)
│   │   └── RagView.vue          ← 知识库问答
│   └── components/              ← 可复用组件(聊天气泡/步骤面板等)
└── .env.development             ← 环境变量
```

**铁律**: 页面组件不写业务请求逻辑，全部收敛到 `src/api/`；`request.js` 是全项目唯一的 axios 出口。

---

## 三、API 对接规范

### 3.1 axios 统一封装（request.js）

```js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',               // 走 Vite 代理(见第八章),不写死 localhost
  timeout: 30000                 // 普通接口 30s；SSE 不走 axios(见第五章)
})

// 响应拦截器:统一解包 BaseResponse
request.interceptors.response.use(
  (resp) => {
    const { code, data, message } = resp.data
    if (code === 0) return data        // 直接返回 data,业务层不用管 code
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request
```

### 3.2 接口封装示例（travel.js）

```js
import request from './request'

// 多轮对话
export const chat = (message, conversationId) =>
  request.post('/travel/chat', { message, conversationId })

// 结构化报告
export const travelReport = (message, conversationId) =>
  request.post('/travel/report', { message, conversationId })

// RAG 问答
export const ragChat = (question, conversationId) =>
  request.post('/travel/rag-chat', { question, conversationId })
```

### 3.3 统一响应结构（后端 BaseResponse）

```json
{ "code": 0, "data": {...}, "message": "" }
```

| code | 含义 | 前端处理 |
|---|---|---|
| 0 | 成功 | 正常渲染 data |
| 40000 | 请求参数错误 | 提示用户检查输入 |
| 40100 / 40101 | 未登录 / 无权限 | 跳登录页（本项目暂无登录） |
| 40400 | 数据不存在 | 提示 |
| 50000 / 50001 | 系统异常 / 操作失败 | 统一错误提示 |

---

## 四、后端接口清单（全部 6 个）

> baseURL = `http://localhost:8123/api`（context-path=`/api`，端口 8123）
> 浏览器开发环境统一用 Vite 代理 `/api`，**不要**直接写 8123。

### 4.1 多轮对话（AiController）

| 项 | 内容 |
|---|---|
| 方法/路径 | `POST /api/travel/chat` |
| 请求体 | `{ "message": "北京3月天气怎么样？", "conversationId": "可选" }` |
| 成功响应 | `{ code:0, data:{ conversationId, reply }, message:"" }` |
| 错误 | message 为空 → code=40000 |
| 备注 | conversationId 回显，前端保存，下轮请求带上即可续聊 |

### 4.2 结构化旅行报告（AiController）

| 项 | 内容 |
|---|---|
| 方法/路径 | `POST /api/travel/report` |
| 请求体 | `{ "message": "规划3天北京游", "conversationId": "可选" }` |
| 成功响应 | `{ code:0, data:{ conversationId, title, suggestions: ["..."] }, message:"" }` |
| 前端用法 | suggestions 数组渲染成推荐卡片列表 |

### 4.3 RAG 知识库问答（TravelRagController）

| 项 | 内容 |
|---|---|
| 方法/路径 | `POST /api/travel/rag-chat` |
| 请求体 | `{ "question": "必填", "conversationId": "可选" }` |
| 成功响应 | `{ code:0, data:{ answer, matched: true/false, conversationId }, message:"" }` |
| 关键字段 | **matched=false 时 answer 是固定兜底话术**（"暂无相关旅游攻略信息"），前端要展示"未命中"样式（如提示条），别当成正常回答 |
| 错误 | question 为空 → code=40000 |

### 4.4 旅行问答 SSE 流式（AiController · Flux 版）

| 项 | 内容 |
|---|---|
| 方法/路径 | `GET /api/travel/chat/sse?message=你好&chatId=t1` |
| 响应头 | `Content-Type: text/event-stream` |
| 返回 | `Flux<String>`，**逐字/逐块**推送，前端拼成打字机效果 |
| 参数 | message 必填；chatId 可选（会话续聊） |
| 超时 | 无 SSE 专用超时（Flux 流），连接断开即停 |

### 4.5 旅行问答 SSE 备用接口（AiController · SseEmitter 版）

| 项 | 内容 |
|---|---|
| 方法/路径 | `GET /api/travel/chat/sse/emitter?message=你好&chatId=t1` |
| 响应头 | `Content-Type: text/event-stream` |
| 备注 | 与 4.4 二选一，效果一致；**推荐用 4.4**，本接口备用 |

### 4.6 Agent 智能体 SSE（AgentController · 事件协议核心）

| 项 | 内容 |
|---|---|
| 方法/路径 | `GET /api/agent/sse?message=帮我查北京天气` |
| 响应头 | `Content-Type: text/event-stream` |
| 备注 | 每次请求后端新建 Agent，**不支持多轮**（messageList 隔离） |

**SSE 事件协议（Agent 前端必读）**：

| event name | 含义 | data 内容 | 前端渲染 |
|---|---|---|---|
| `step` | 思考步骤 | `"思考: {thought}"` | 步骤面板"思考中…" |
| `tool` | 工具调用 | `"{toolName}({args}) → {result}"` | 工具卡片：名称/入参/结果 |
| `loop` | 死循环警告 | `"[LoopGuard] {message}"` | 红色警告条 |
| `error` | 错误 | `"{errorMessage}"` | 错误提示 |
| `done` | 完成 | 最终回复文本 | 正常输出最终答案 |

---

## 五、SSE 流式对接规范（核心章节）

### 5.1 打字机效果（/travel/chat/sse 用 EventSource）

```js
// utils/sse.js —— EventSource 封装
export function connectChatSSE({ message, chatId, onData, onDone, onError }) {
  const params = new URLSearchParams({ message })
  if (chatId) params.set('chatId', chatId)

  const es = new EventSource(`/api/travel/chat/sse?${params}`)
  // 注意:后端 Flux<String> 默认走 message 事件;data 就是文本块
  es.onmessage = (e) => onData(e.data)      // 逐块追加到打字机
  es.onerror = () => {                       // 流结束或断线
    es.close()
    onDone?.()
  }
  return es   // 返回实例,组件卸载时调用 es.close()
}
```

**打字机渲染**（ChatView.vue）：

```vue
<template>
  <div ref="box" class="chat-msg">{{ streamText }}</div>
</template>
<script setup>
import { ref, nextTick } from 'vue'
const streamText = ref('')
const box = ref(null)

function append(chunk) {
  streamText.value += chunk
  nextTick(() => box.value?.scrollIntoView({ behavior: 'smooth' }))
}
</script>
```

### 5.2 Agent 事件流（/agent/sse 用 EventSource + addEventListener）

```js
export function connectAgentSSE({ message, handlers, onDone }) {
  const es = new EventSource(`/api/agent/sse?message=${encodeURIComponent(message)}`)
  const events = ['step', 'tool', 'loop', 'error', 'done']

  events.forEach(name => {
    es.addEventListener(name, (e) => handlers[name]?.(e.data))
  })
  es.onerror = () => { es.close(); onDone?.() }
  return es
}
```

### 5.3 SSE 铁律（对照后端 TESTING_SPEC Ch09 约束）

| # | 规则 |
|---|---|
| 1 | **必须**用 `EventSource`（GET + text/event-stream），不能用 axios/fetch 拉 SSE |
| 2 | 组件 `onUnmounted` **必须** `es.close()`，否则后端 SseEmitter 不释放（P0-2） |
| 3 | 中文参数**必须** `encodeURIComponent`（TC-AI-07 已列为测试用例） |
| 4 | 断线策略：EventSource 自动重连，重连后前端**清空并重新渲染**，不要继续拼接旧文本 |
| 5 | Agent 的 `done` 事件到达后必须 `es.close()` 并置 loading=false |

### 5.4 loading 状态规范

```js
const loading = ref(false)
async function send() {
  loading.value = true
  streamText.value = ''
  try {
    const es = connectChatSSE({ message, onData: append })
    pendingES.value = es
  } finally { /* 不要在 finally 关 loading,等 onDone/onerror 里关 */ }
}
```

---

## 六、状态管理规范（Pinia）

| 状态类型 | 存放位置 | 示例 |
|---|---|---|
| 跨页面共享 | Pinia store | conversationId（聊天页→报告页传参） |
| 单页临时 | 组件 ref | 输入框、loading、流式文本 |
| 后端返回 | 不缓存 | 每次请求重新拉 |

**禁止**:把流式回答缓存进 store（打字机文本是组件私有状态）。

---

## 七、组件与页面规范

- 页面统一 `views/`，可复用组件统一 `components/`，命名 `PascalCase.vue`
- 所有 UI 用 Element Plus：按钮 `el-button`、输入 `el-input`、加载 `v-loading`
- 聊天消息气泡组件 `ChatBubble.vue`：区分用户消息/AI 消息/流式中的消息
- Agent 步骤面板 `AgentSteps.vue`：接收 step/tool 事件，渲染时间线（`el-timeline`）
- 样式：scoped CSS，尺寸单位 px（项目简单，不引入 rem/px2rem）
- 错误态：接口失败 → `ElMessage.error` + 页面局部空态，不白屏

---

## 八、环境与启动

### 8.1 Vite 代理（vite.config.js，dev 必配）

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8123',   // 后端端口(见 CLAUDE.md)
        changeOrigin: true
      }
    }
  }
})
```

### 8.2 启动流程

```bash
# 1. 先起后端(IDEA Run,JDK 26,端口 8123)
# 2. 前端
cd ai-travel-planner-frontend
npm install
npm run dev        # http://localhost:3000
```

### 8.3 环境变量（.env.development）

```ini
VITE_APP_TITLE=AI 旅行规划助手
# baseURL 统一走 /api 代理,不要在这里写 http://localhost:8123
```

---

## 九、常见问题排查表

| 症状 | 可能原因 | 排查方向 |
|---|---|---|
| 接口 404 | 后端没起 / context-path 不对 | 确认 8123 端口 + `/api` 前缀 |
| 请求报 CORS | 没用 Vite 代理,直连了 8123 | 检查 request.js baseURL 是否为 `/api` |
| SSE 不输出 | EventSource 不能用 POST / 用了 axios | 确认 GET + text/event-stream |
| 中文乱码/报错 | 参数没 encode | `encodeURIComponent` |
| 打字机卡住 | 流结束没 close / onerror 没处理 | 检查 onerror 里 es.close() |
| Agent 无 step/tool 帧 | 后端 Agent 异常 | 看后端日志 + TESTING_SPEC TC-AGT 系列 |
| matched=false 显示成正常回答 | 前端没判断 matched | RAG 页必须区分两种样式 |

---

## 十、前端自测清单（提测前过一遍）

- [ ] 6 个接口全部用真实后端跑通（对照第四章表格）
- [ ] SSE 打字机：逐字输出、自动滚动、断网重连不花屏
- [ ] Agent 工作台：step/tool/loop/error/done 五种事件都有对应 UI
- [ ] 中文输入、空输入、超长输入 三个边界
- [ ] 页面刷新后 conversationId 保留（localStorage），能续聊
- [ ] 组件卸载（切路由）时所有 EventSource 已 close

---

## 十一、变更记录

| 日期 | 变更 |
|---|---|
| 2026-08-04 | 初版：基于后端 3 个 Controller 生成，覆盖 API 对接、SSE 事件协议、Agent 工作台、自测清单 |
