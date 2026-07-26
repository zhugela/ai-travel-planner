# Cursor Prompt 模板 · Vue3 + Vite AI 旅行规划前端(浅色商务风)

> 配套笔记:[学习笔记-09-AI服务化部署.md §4](../学习笔记-09-AI服务化部署.md)
> 用法:Cursor 打开空文件夹 → 粘贴本文 Prompt → Agent 模式自动生成完整项目
> 适用后端:本项目 `ai-travel-planner`(端口 8123,SSE 已实现)
> UI 风格:**企业后台简约浅色商务风**(对齐你已实现的前端)

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
一、整体 UI 视觉风格(强制)
═══════════════════════════════════════════

整体企业后台简约浅色商务风,主色 #1677ff,扁平化设计。
禁用:渐变、粒子、霓虹、发光、深色科技背景。
统一 4px 小圆角、浅灰细边框、极轻单层阴影,页面留白充足。
仅保留基础淡入过渡动画,无复杂动效。
聊天气泡扁平简洁,无多余装饰,功能性优先。
适配 PC 后台为主,简易兼容平板移动端。

═══════════════════════════════════════════
二、页面需求
═══════════════════════════════════════════

1. 首页(/)
   - 白底简约布局,PC 端双卡片左右并排,平板/手机垂直堆叠自适应
   - 两张功能入口卡片:
     a) 「AI 旅行问答」:走普通对话接口(浅蓝气泡主题)
     b) 「AI 旅行规划智能体」:走 Agent 多步工具调用接口(深蓝主题)
   - 卡片包含:简约文字标题、简短功能描述、跳转按钮(不使用复杂装饰图标)
   - 页面底部极简版权文字,删减多余友情链接、用户协议文案

2. 旅行问答页(/chat)
   - 顶部窄导航栏:左侧标题「旅行问答」,右侧「返回首页」按钮
   - 滚动聊天容器:
     - 用户消息:右对齐、浅蓝纯色气泡
     - AI 消息:左对齐、浅灰气泡,每条 AI 消息左侧统一简易灰色头像(写"AI"或"我")
   - 底部输入区:文本输入框 + 发送按钮,增加空输入拦截,空白禁止发送请求
   - 会话持久化:自动生成 UUID chatId,存入 localStorage 区分不同对话会话
   - 调用 GET /api/travel/chat/sse?message=xxx&chatId=xxx
   - SSE 流式渲染:EventSource 分片监听,逐字追加实现打字机效果
   - 连接管控:发送新消息、切换路由时自动销毁上一条 EventSource,防止后端 SseEmitter 句柄泄漏

3. 旅行规划智能体页(/agent)
   - 顶部导航栏:左侧标题「旅行规划智能体」,右侧返回首页按钮
   - 消息区分:用户消息右侧气泡,Agent 执行日志左侧展示
   - Agent 日志分层固定渲染规则:
     a) "Step 1: ..."  → 深色加粗标题
     b) "【工具执行】xxx => result"  → 工具名浅蓝色高亮,结果自动换行展示
     c) "[LoopGuard] ..."  → 低饱和红色纯文字警告,无发光特效
     d) 所有步骤内容必须使用 <pre> + white-space: pre-wrap 保留原始换行格式
   - 底部增加「停止生成」按钮,点击立即关闭当前 SSE 连接

═══════════════════════════════════════════
三、技术栈与依赖
═══════════════════════════════════════════

- Vue 3(Composition API + <script setup>)
- Vite 5(要求 Node.js ≥ 18;Node 16 会报 crypto.getRandomValues 错)
- Vue Router 4
- Axios 1.x(用于非 SSE 普通请求)
- 浏览器原生 EventSource API(SSE 流式输出)

═══════════════════════════════════════════
四、后端接口文档
═══════════════════════════════════════════

后端基础地址:http://localhost:8123
统一前缀:/api(context-path)

接口 1:旅行问答 SSE(GET)
  路径:/api/travel/chat/sse
  参数:message(必填)、chatId(可选,前端自动生成)
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
五、环境、适配、跨域规则
═══════════════════════════════════════════

- 支持 Windows / macOS 双系统开发
- Node 版本:≥ 18(Vite 5 要求;Node 16 请降级 Vite 到 4 或升级 Node)
- 浏览器:Chrome / Edge / Safari 最新版(EventSource 兼容)
- 开发端口:3000(Vite 默认)
- 跨域解决方案:必须配置 Vite 开发代理转发 /api,规避浏览器 CORS 限制;
  后端已配置 CorsConfig 作为兜底
- 无登录、无权限校验,纯演示页面
- 不要引入 TypeScript(保持简单,JS 即可)
- 中文界面,UTF-8

═══════════════════════════════════════════
六、固定项目目录结构(严格按此生成)
═══════════════════════════════════════════

yu-ai-agent-frontend/
├── src/
│   ├── api/
│   │   └── index.js        # axios + EventSource 封装
│   ├── views/
│   │   ├── Home.vue        # 首页门户
│   │   ├── ChatView.vue    # 旅行问答
│   │   └── AgentView.vue   # 智能体
│   ├── components/         # 复用组件(顶栏/气泡/输入框等)
│   ├── router/
│   │   └── index.js        # 路由配置
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js          # 配置 server.proxy 把 /api 转发到 8123
├── package.json
└── README.md

═══════════════════════════════════════════
七、生成后必须做的两件事
═══════════════════════════════════════════

1. 在 vite.config.js 配置 dev server proxy:
   server: { proxy: { '/api': 'http://localhost:8123' } }
   这样开发时前端 3000 调 /api 不会触发 CORS

2. 在 README.md 写清:
   - 启动方式(npm install + npm run dev)
   - 后端要先跑(端口 8123,需要 DASHSCOPE_API_KEY)
   - Node 版本要求(≥ 18)

═══════════════════════════════════════════
八、必须修复的 2 个常见 Bug
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
九、必须满足的样式规范(浅色商务风)
═══════════════════════════════════════════

- 全局 background:#ffffff,color:#1f2937
- 主色:#1677ff,hover 用 #4096ff
- 按钮:padding 6-12px / border-radius 4px / 禁用态用 #d1d5db
- 卡片:border 1px solid #e5e7eb,box-shadow 0 1px 2px rgba(0,0,0,0.04)
- 聊天气泡:用户 #e6f4ff,AI #f3f4f6,字号 14px
- 输入框:focus 边框 #1677ff
- 头部导航:padding 12px 24px,底部 1px 浅灰边
- 响应式:≤640px 单列布局
- 字体栈:-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif

═══════════════════════════════════════════
执行步骤:
═══════════════════════════════════════════

1. cd 到本项目同级目录的 yu-ai-agent-frontend 空文件夹
2. npm create vite@latest . -- --template vue(如果 . 是空文件夹)
3. npm install vue-router@4 axios
4. 按上面的目录结构生成所有文件
5. 写入所有文件代码,严格遵守浅色商务风样式规范
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
3. **后端 SSE 端点真要通**,必须先有后端 6 端点都跑通(本项目已实现)

---

## Bug 修复套路

第一次生成完基本能跑,但通常有 1-3 处 bug。修复流程:

| 现象 | 修复方法 |
|---|---|
| EventSource 时序错位 | 检查 `<button @click>` 才 `new EventSource()`,不能 `onMounted` |
| CORS | 检查 `vite.config.js` 有没有 proxy |
| `crypto.getRandomValues is not a function` | Node < 18 → 升级 Node 或改 Vite 版本 |
| SSE 不打字机 | 检查 `currentAIMessage.value += chunk` 是不是响应式 ref,是不是 `.value` 没加 |
| 出现深色背景 / 渐变 | 违反"浅色商务风"约束,改 background 为 #ffffff,删渐变 |

**修 Bug 时**:把 F12 网络面板日志 + 报错截图 + 完整需求一起丢回 Cursor,让它修复。

---

*最后更新:2026-07-18 · 配套 Ch 09 §4 用 · 浅色商务风版本*