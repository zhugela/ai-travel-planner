# REQUIREMENTS.md — AI 旅行规划助手需求梳理

> **需求来源**:仅整理 `docs/学习笔记-NN-XXX.md` (Ch 02~Ch 09) 里已实现 / 已设计的功能。
> **更新规则**:任何新增需求必须改这份文件 + 对应章节笔记。

---

## 一、业务目标

构建一个**面向旅行场景的 AI 助手**,用户用自然语言描述旅行需求,系统:

1. **理解意图**——通过大模型理解用户旅行偏好
2. **检索知识**——基于本地攻略文档做 RAG 增强(减少编造)
3. **调用工具**——必要时调用外部工具(天气、搜索、文件操作等)
4. **执行规划**——多步骤任务由 AI Agent 自动拆解执行
5. **输出可交付方案**——支持结构化报告 + PDF 导出

**学习定位**:从「会调 API」到「完整 AI 应用工程化」的实战项目,简历可写。

---

## 二、用户故事

### US-1 普通旅行问答
- **作为** 旅客
- **我希望** 用自然语言问旅行相关问题(景点 / 签证 / 行程)
- **以便于** 快速获取答案,不用自己搜索多个网站

**验收**:
- 提问"北京 3 月穿什么" → 收到自然语言回复
- 支持多轮上下文(记住上一轮答案)

### US-2 旅行攻略深度问答
- **作为** 旅客
- **我希望** 系统能基于本地攻略文档回答
- **以便于** 答案有据可查,不是编造

**验收**:
- 提问"杭州西湖几月去最好" → 答案引用本地知识库
- 引用文档标识可见(可扩展)

### US-3 旅行报告自动生成
- **作为** 旅客
- **我希望** 输入需求自动生成结构化行程报告
- **以便于** 不用自己整理大纲

**验收**:
- 提问"杭州 3 天亲子游,预算 5000" → 收到 `title + suggestions[]`
- 报告可下载为 PDF

### US-4 多步骤智能体规划
- **作为** 旅客
- **我希望** 系统能自动调用工具完成复杂任务
- **以便于** 一次输入拿到完整方案

**验收**:
- 提问"帮我规划 3 天杭州旅行" → 智能体依次调用景点查询、天气查询、PDF 生成等
- 实时显示每一步日志

### US-5 长任务异步化
- **作为** 旅客
- **我希望** 长任务不卡死界面
- **以便于** 系统能处理耗时操作(查 PDF 下载、生成报告)

**验收**:
- SSE 流式响应,前段打字机渲染
- 支持中途停止

### US-6 多模态输入(扩展)
- **作为** 旅客
- **我希望** 上传图片让 AI 识别景点
- **以便于** "我在这,这是什么景点?"

**验收**:图片上传 → 识别 + 知识库检索
- **当前状态**:**未实现**(MCP image-search 服务端已实现,前端集成未做)

---

## 三、功能清单

### Ch 02 · AI 大模型接入

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| SDK 调用 | 用 DashScope SDK 直接调用 | ✅ | 6 个 Demo 类齐全 |
| HTTP 调用 | 用 HTTP 协议直接调通义 | ✅ | HttpAiInvoke.java |
| Spring AI 调用 | 用 Spring AI 抽象层 | ✅ | SpringAiAiInvoke.java |
| Ollama 调用 | 用本地 Ollama 备用 | ✅ | OllamaAiInvoke.java |
| LangChain4j 调用 | 用 LangChain4j 对比 | ✅ | LangChainAiInvoke.java |

### Ch 03 · Prompt 与多轮对话

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| ChatClient 多轮对话 | 用 ChatClient fluent API | ✅ | TravelApp.doChat |
| System Prompt 模板 | 用 .st 模板渲染动态内容 | ✅ | travel-system-prompt-template.st |
| 多轮上下文 | MessageChatMemoryAdvisor | ✅ | FileBasedChatMemory |
| 结构化报告 | Prompt + entity 解析 | ✅ | TravelReport(title, suggestions) |
| 文件会话记忆 | Kryo 序列化到本地 | ✅ | chatmemory/ |
| 日志 Advisor | MyLoggerAdvisor | ✅ | advisor/MyLoggerAdvisor.java |

### Ch 04 · RAG 知识库基础

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| 内存向量库 | SimpleVectorStore | ✅ | TravelVectorStoreConfig |
| Markdown 文档加载 | spring-ai-markdown-document-reader | ✅ | TravelDocumentLoader |
| 查询扩展 | MultiQueryExpanderDemo | ✅ | demo/rag/ |
| RAG Chat Service | QuestionAnswerAdvisor | ✅ | TravelRagChatService |
| RAG Controller | 云知识库接入 | ✅ | TravelRagController |
| 云知识库 Advisor | 百炼云知识库 | ✅ | TravelRagCloudAdvisorConfig |
| 知识库文档 | 3 份 markdown | ✅ | travel-domestic-low/mid/overseas.md |

### Ch 05 · RAG 进阶与调优

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| Re-ranking | 检索后重排序 | ✅ | RagRetrieverEnhancer |
| Query 缓存 | Caffeine 缓存相同查询 | ✅ | RagRetrieverEnhancer |

### Ch 06 · 工具调用与 MCP

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| FileOperationTool | 文件读写 | ✅ | tools/FileOperationTool |
| WebSearchTool | 联网搜索 | ✅ | tools/WebSearchTool |
| WebScrapingTool | 网页抓取 | ✅ | tools/WebScrapingTool |
| TerminalTool | 终端命令(白名单) | ✅ | tools/TerminalTool |
| DownloadTool | 资源下载 | ✅ | tools/DownloadTool |
| PdfGenerationTool | PDF 生成 | ✅ | tools/PdfGenerationTool |
| WeatherTool | 天气查询(免 key) | ✅ | tools/WeatherTool |
| TerminateTool | 任务结束 | ✅ | tools/TerminateTool |
| AskHumanTool | 人机询问 | ✅ | tools/AskHumanTool |
| ToolRegistration | 8 个 Tool 统一注册 | ✅ | config/ToolRegistration |

### Ch 07 · MCP 协议深入

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| MCP Client 配置 | application.yml | ✅ | spring.ai.mcp.client.* |
| MCP 远程工具注入 | AgentController 自动合并 | ✅ | buildToolCallbacks() |
| 图片搜索 MCP 服务端 | Maven 项目 | ✅ | 上次 commit `2e30886` |
| MCP 测试配置 | application-test.properties | ✅ | |
| **当前禁用** MCP server | 防止端口冲突 | ⚠️ | application.yml mcp.client.enabled=false |

### Ch 08 · AI 智能体构建

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| BaseAgent 顶层抽象 | state + run loop | ✅ | agent/BaseAgent |
| ReActAgent think/act | 思考-行动分离 | ✅ | agent/ReActAgent |
| ToolCallAgent 工具调用 | 完整实现 | ✅ | agent/ToolCallAgent |
| YuTravelAgent 成品 | maxSteps=20 + systemPrompt | ✅ | agent/YuTravelAgent |
| AgentState 枚举 | IDLE/RUNNING/FINISHED/ERROR | ✅ | enums/AgentState |
| StateContext | try-with-resources 自动管理 | ✅ | context/StateContext |
| LoopGuard | 死循环检测 | ✅ | LoopGuard |
| ChainAgent | 提示链工作流 | ✅ | workflow/ChainAgent |
| AskHumanTool | 人机交互 | ✅ | tools/AskHumanTool |
| **BaseAgent.runStream()** | Agent SSE 流式入口 | ✅ | BaseAgent.runStream |
| 多模态图片支持 | ToolResult.base64_image | ❌ | 未来扩展 |

### Ch 09 · AI 服务化部署

| 功能 | 描述 | 状态 | 验收 |
|---|---|---|---|
| 普通聊天 SSE | Flux<String> | ✅ | AiController.doChatWithSSE |
| SseEmitter 备用 | 传统 Servlet 异步 | ✅ | AiController.doChatWithSseEmitter |
| 普通同步聊天 | POST /api/travel/chat | ✅ | AiController.chat |
| 结构化报告 | POST /api/travel/report | ✅ | AiController.report |
| RAG 对话 | POST /api/travel/rag-chat | ✅ | TravelRagController |
| Agent SSE | GET /api/agent/sse | ⚠️ | 代码就绪,运行时 404(classpath 缓存) |
| CORS 配置 | allowedOriginPatterns | ✅ | CorsConfig |
| application-prod.yml | 生产配置 | ✅ | 资源文件 |
| Dockerfile | 后端容器化 | ✅ | Dockerfile |
| .dockerignore | 排除文件 | ✅ | .dockerignore |
| Nginx 配置 | 前端反代 + SSE | ❌ | 在前端项目里 |
| Serverless 部署 | 实际部署 | ❌ | 仅文档 |

---

## 四、非功能需求

### 性能
- 普通聊天 SSE 首字延迟 ≤ 2s
- Agent 步骤间隔 ≤ 5s
- 单 Agent 请求占用 Tomcat 线程 ≤ 1(用 SseEmitter)

### 安全
- API Key 走环境变量,不进代码
- 工具白名单(TerminalTool 仅允许特定命令)
- 工具输入校验(URL / 文件名)

### 可用性
- 接口支持 `chatId` 多轮上下文
- Agent 步骤可中断(停止按钮)
- 异常有兜底回复,不返回空

### 可观测(未来)
- 日志:结构化输出
- 监控:Tool 调用次数 / AI token 用量
- 链路:每个请求 traceId

---

## 五、需求优先级

| P | 描述 | 必须完成 |
|---|---|---|
| **P0** | Ch 02~08 主功能跑通 | ✅ 已完成 |
| **P0** | SSE 流式打字机工作 | ✅ 已验证 |
| **P1** | Agent SSE 步骤日志 | ⚠️ classpath 缓存 |
| **P1** | Dockerfile 可构建 | ✅ 已写 |
| **P2** | 真实部署 | ❌ 跳过 |
| **P2** | 多模态图片 | ❌ 跳过 |
| **P3** | 监控 / 链路追踪 | ❌ 跳过 |

---

## 六、变更记录

| 日期 | 变更 |
|---|---|
| 2026-07-18 | 初版:从 Ch 02~09 笔记抽取,标注所有功能状态 |
| TBD | 5 项补全后更新(Re-ranking / MCP 注入 / AskHuman / Dockerfile / AskHuman 已写入) |

---

*最后更新:2026-07-18*