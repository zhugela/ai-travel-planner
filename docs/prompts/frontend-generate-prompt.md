# AI旅行规划助手前端一键构建Prompt（Claude Code专用）
适配后端ai-travel-planner服务，端口8123，内置完整业务逻辑、SSE规范、目录约束、Bug规避规则，整体采用企业后台简约浅色商务UI，摒弃深色科技炫酷风格。
> 使用方式：空文件夹打开，将全文交给Claude Code自动初始化项目、生成全部代码、配置、README

## 一、整体执行流程
1. 本地新建空文件夹 yu-ai-agent-frontend
2. Claude Code打开该目录
3. 读取本文档全部规则，全自动执行项目初始化、依赖安装、页面/接口/路由编写
4. 执行完成后运行 npm run dev，访问 http://127.0.0.1:3000

## 二、全局强制规范
1. 框架语法：Vue3 全部使用 <script setup> 组合式API，禁止Options API
2. 语言：原生JS，不引入TypeScript，降低项目复杂度
3. Node环境：Node.js ≥ 18；Node16运行会报crypto.getRandomValues错误，README标注降级Vite4兼容方案
4. 浏览器兼容：Chrome/Edge/Safari最新版，保障EventSource流式接口正常运行
5. 权限：演示项目，无需登录、无token鉴权，全站中文UTF-8
6. 请求区分规则
   - 普通同步接口：Axios 1.x
   - 流式对话接口：浏览器原生 EventSource API，统一封装，严格管控连接生命周期
7. UI视觉风格约束（替换原深色科技风，适配后端管理系统）
整体企业后台简约浅色商务风，主色#1677ff，扁平化设计；禁用渐变、粒子、霓虹、发光、深色科技背景。
统一4px小圆角、浅灰细边框、极轻单层阴影，页面留白充足。
仅保留基础淡入过渡动画，无复杂动效；聊天气泡扁平简洁，无多余装饰，功能性优先，适配PC后台为主，简易兼容平板移动端。

## 三、页面完整需求
### 1. 首页 /
- 白底简约布局，PC端双卡片左右并排，平板/手机垂直堆叠自适应
- 两张功能入口卡片：
  a) 「AI 旅行问答」：跳转 /chat，普通SSE对话接口，浅蓝气泡主题
  b) 「AI 旅行规划智能体」：跳转 /agent，Agent多步骤工具调用日志页面
- 卡片包含简约文字标题、简短功能描述、跳转按钮，不使用复杂装饰图标
- 页面底部极简版权文字，删减多余友情链接、用户协议文案

### 2. 旅行问答页 /chat
- 顶部窄导航栏：左侧标题「旅行问答」，右侧「返回首页」按钮
- 滚动聊天容器：
  - 用户消息：右对齐、浅蓝纯色气泡
  - AI消息：左对齐、浅灰气泡，每条AI消息左侧统一简易灰色头像
- 底部输入区：文本输入框 + 发送按钮，增加空输入拦截，空白禁止发送请求
- 会话持久化：自动生成UUID chatId，存入localStorage区分不同对话会话
- 接口调用 GET /api/travel/chat/sse?message=xxx&chatId=xxx
- SSE流式渲染：EventSource分片监听，逐字追加实现打字机效果
- 连接管控：发送新消息、切换路由时自动销毁上一条EventSource，防止后端SseEmitter句柄泄漏

### 3. 旅行规划智能体页 /agent
- 顶部导航栏：左侧标题「旅行规划智能体」，右侧返回首页按钮
- 消息区分：用户消息右侧气泡，Agent执行日志左侧展示
- Agent日志分层固定渲染规则：
  a) "Step 1: ..." → 深色加粗标题
  b) "【工具执行】xxx => result" → 工具名浅蓝色高亮，结果自动换行展示
  c) "[LoopGuard] ..." → 低饱和红色纯文字警告，无发光特效
  d) 所有步骤内容必须使用 <pre> + white-space: pre-wrap 保留原始换行格式
- 底部增加「停止生成」按钮，点击立即关闭当前SSE连接

## 四、技术栈与依赖清单
- Vue 3（Composition API + <script setup>）
- Vite 5（Node≥18）
- Vue Router 4
- Axios 1.x（非流式普通请求）
- 浏览器原生 EventSource API（SSE流式输出）

## 五、后端完整接口文档
后端基础地址：http://localhost:8123
全局接口统一前缀：/api
1. 旅行问答 SSE（GET）
路径：/api/travel/chat/sse
参数：message(必填)、chatId(可选，前端自动生成UUID)
响应类型：text/event-stream，分段字符串分片，用于打字机流式输出

2. 旅行规划智能体 SSE（GET）
路径：/api/agent/sse
参数：message(必填)
响应类型：text/event-stream，分段返回Step执行日志文本

3. 旅行规划同步报告（POST，可选）
路径：/api/travel/report
请求体：{ message, conversationId }
返回格式：{ code, data: { title, suggestions[] } }

## 六、环境、适配、跨域规则
1. 支持 Windows / macOS 双系统开发
2. 开发端口固定 3000
3. 跨域解决方案：必须配置Vite开发代理转发/api，规避浏览器CORS限制；后端已配置CorsConfig作为兜底
4. 无登录、无权限校验，纯演示页面

## 七、固定项目目录结构（严格按此生成）