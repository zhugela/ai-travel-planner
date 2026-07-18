# Cursor Prompt 模板 · Vue3 + Vite AI 旅行规划前端(配套 Ch 09 §4)

> 配套笔记:[学习笔记-09-AI服务化部署.md §4](../学习笔记-09-AI服务化部署.md)
> 用法:Cursor 打开空文件夹 → 粘贴本文 Prompt → Agent 模式自动生成完整项目
> 适用后端:本项目 `ai-travel-planner`(端口 8123,SSE 已实现)

---

## 一句话流程

```
Cursor 打开空文件夹 yu-ai-agent-frontend
  → 粘贴下面 Prompt
  → Agent 模式自动:Vite 创建 + 装依赖 + 写页面/路由/接口 + README
  → npm run dev → http://127.0.0.1:3000
```

---

## Prompt(直接复制整段)

```markdown
请帮我用 Vue3 + Vite + VueRouter + Axios 技术栈,
创建一个「AI 旅行规划助手」前端聊天项目。

═══════════════════════════════════════════
一、页面需求
═══════════════════════════════════════════

1. 首页(/)
   - 深色科技风背景
   - 两个应用入口卡片:
     a) 「AI 旅行问答」:走普通对话接口(蓝色气泡主题)
     b) 「AI 旅行规划智能体」:走 Agent 多步工具调用接口(深蓝主题)
   - 每个卡片包含:图标、标题、描述、按钮
   - 底部统一:版权信息 + 友情链接 + 用户协议
   - 支持响应式(PC / 平板 / 手机自适应)

2. 旅行问答页(/chat)
   - 顶部导航栏:左侧 logo「旅行问答」,右侧"返回首页"
   - 主区:上下滚动聊天记录
   - 用户消息:右对齐 + 蓝色气泡
   - AI 消息:左对齐 + 浅灰气泡
   - 每条 AI 消息左侧带统一 AI 头像
   - 底部输入区:文本框 + 发送按钮
   - 自动生成 chatId(UUID)区分会话,存入 localStorage
   - 调用 GET /api/travel/chat/sse?message=xxx&chatId=xxx
   - SSE 流式渲染:EventSource 监听,逐字追加到气泡(打字机效果)

3. 旅行规划智能体页(/agent)
   - 顶部导航栏:左侧 logo「旅行规划智能体」,右侧"返回首页"
   - 主区:用户消息右气泡,Agent 执行日志左气泡
   - Agent 日志分步骤渲染:
     a) "Step 1: ..."  → 深色加粗标题
     b) "【工具执行】xxx => result"  → 工具名高亮、结果换行展示
     c) "[LoopGuard] ..."  → 红色警告样式
     d) 所有 step 内容必须用 <pre> + white-space: pre-wrap 换行
   - 底部「停止生成」按钮(可选,可先不做)

═══════════════════════════════════════════
二、技术栈与依赖
═══════════════════════════════════════════

- Vue 3(Composition API + <script setup>)
- Vite 5(要求 Node.js ≥ 18,Node 16 会报 crypto.getRandomValues 错)
- Vue Router 4
- Axios 1.x(用于非 SSE 普通请求)
- EventSource API(浏览器原生,SSE 用它)

═══════════════════════════════════════════
三、后端接口文档
═══════════════════════════════════════════

后端基础地址:http://localhost:8123
统一前缀:/api(context-path)

接口 1:旅行问答 SSE(GET)
  路径:/api/travel/chat/sse
  参数:message(必填)、chatId(可选,自动生成)
  返回:text/event-stream,data 帧为字符串片段
  用途:逐字打字机效果

接口 2:旅行规划智能体 SSE(GET)
  路径:/api/agent/sse
  参数:message(必填)
  返回:text/event-stream,data 帧为 "Step x: ..." 日志
  用途:Agent 多步骤实时日志

接口 3:旅行规划同步报告(POST,可选)
  路径:/api/travel/report
  入参:{ message, conversationId }
  返回:{ code, data: { title, suggestions[] } }

═══════════════════════════════════════════
四、环境与适配
═══════════════════════════════════════════

- 跨平台:Windows / macOS
- Node 版本:≥ 18(Vite 5 要求;Node 16 请降级 Vite 到 4 或升级 Node)
- 浏览器:Chrome / Edge / Safari 最新版(EventSource 兼容)
- 开发端口:3000(Vite 默认)
- 跨域:后端已配 CorsConfig 允许 localhost:3000;但建议用 Vite proxy 转发避免跨域
- 不需要实现登录鉴权(演示项目)
- 不要引入 TypeScript(保持简单,JS 即可)
- 中文界面,UTF-8

═══════════════════════════════════════════
五、目录结构建议
═══════════════════════════════════════════

yu-ai-agent-frontend/
├── src/
│   ├── api/
│   │   └── index.js        # axios + EventSource 封装
│   ├── views/
│   │   ├── Home.vue        # 首页门户
│   │   ├── ChatView.vue    # 旅行问答
│   │   └── AgentView.vue   # 智能体
│   ├── router/
│   │   └── index.js        # 路由配置
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js          # 配置 server.proxy 把 /api 转发到 8123
├── package.json
└── README.md

═══════════════════════════════════════════
六、生成后必须做的两件事
═══════════════════════════════════════════

1. 在 vite.config.js 配置 dev server proxy:
   server: { proxy: { '/api': 'http://localhost:8123' } }
   这样开发时前端 3000 调 /api 不会触发 CORS

2. 在 README.md 写清:
   - 启动方式(npm install + npm run dev)
   - 后端要先跑(端口 8123,需要 DASHSCOPE_API_KEY)
   - Node 版本要求(≥ 18)

═══════════════════════════════════════════
七、必须修复的 2 个常见 Bug
═══════════════════════════════════════════

1. EventSource 不要在父组件 created() 时 new,要用户点发送按钮才 new,
   否则组件销毁时旧连接残留 → 后端 SseEmitter 句柄不释放

2. 流式追加必须用响应式 ref,例如:
   ```js
   const messages = ref([])
   const currentAIMessage = ref('')
   // EventSource onmessage:
   currentAIMessage.value += chunk
   // 完成后 push 到 messages
   ```
   不要每次都整个数组重渲染,性能极差

═══════════════════════════════════════════
执行步骤:
═══════════════════════════════════════════

1. cd 到本项目同级目录的 yu-ai-agent-frontend 空文件夹
2. npm create vite@latest . -- --template vue(如果 . 是空文件夹)
3. npm install vue-router@4 axios
4. 按上面的目录结构生成所有文件
5. 写入所有文件代码
6. 写入完整 README.md
7. 跑 npm install 确保依赖装好
8. 最后告诉我「项目已生成,可以 npm run dev」
```

---

## 三步执行

| # | 操作 |
|---|---|
| 1 | 在 `D:\code\ai-travel-planner\` 同级新建 `yu-ai-agent-frontend\` 空文件夹 |
| 2 | 用 Cursor 打开该文件夹 |
| 3 | 粘贴整段 Prompt → Agent 模式 → 它会自动跑完全套 |

---

## 联调时的 3 个关键点

1. **前端 3000,后端 8123**
2. **`vite.config.js` 必须配 `/api` proxy**(否则一定会 CORS)
3. **后端 SSE 端点真要通**,必须先有 Ch 09 §A1+A2+B1+B2 的后端代码(本项目已实现)

---

## Bug 修复套路

第一次生成完基本能跑,但通常有 1-3 处 bug。修复流程:

| 现象 | 修复方法 |
|---|---|
| EventSource 时序错位 | 检查 `<button @click>` 才 `new EventSource()`,不能 `onMounted` |
| CORS | 检查 `vite.config.js` 有没有 proxy |
| `crypto.getRandomValues is not a function` | Node < 18 → 升级 Node 或改 Vite 版本 |
| SSE 不打字机 | 检查 `currentAIMessage.value += chunk` 是不是响应式 ref,是不是 `.value` 没加 |

**修 Bug 时**:把 F12 网络面板日志 + 报错截图 + 完整需求一起丢回 Cursor,让它修复。

---

*最后更新:2026-07-17 · 配套 Ch 09 §4 用*