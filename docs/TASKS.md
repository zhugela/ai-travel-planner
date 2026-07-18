# TASKS.md — 开发任务拆解

> **粒度**:大粒度,8 个 task,每个对应 Ch 02~09 一章(部分章节合并)。
> **依赖关系**:编号顺序执行,前序 task 不通过不进入下一章。
> **验收**:每个 task 有明确的"完成定义",独立可验证。

---

## 任务总览

| ID | 标题 | 对应笔记 | 状态 | 估时 |
|---|---|---|---|---|
| T-01 | AI 大模型多方式接入 | Ch 02 | ✅ 已完成 | — |
| T-02 | ChatClient 多轮对话 + 文件记忆 | Ch 03 | ✅ 已完成 | — |
| T-03 | RAG 知识库问答 | Ch 04 | ✅ 已完成 | — |
| T-04 | 工具调用(7 个 @Tool + 安全) | Ch 06 | ✅ 已完成 | — |
| T-05 | AI 智能体 4 层架构 | Ch 08 | ✅ 已完成 | — |
| T-06 | SSE 流式接口 | Ch 09 | ⚠️ 部分(AgentController 404) | — |
| T-07 | **TDD 测试覆盖 + 自动化** | Ch 02~09 | ❌ **当前任务** | 8h |
| T-08 | **联调 + Docker 部署验证** | Ch 09 | ❌ **当前任务** | 4h |

---

## T-01 · AI 大模型多方式接入 ✅

**目标**:跑通 5 种 invoke Demo,理解各调用方式

**对应笔记**:Ch 02 + 作业答案

**实现**:
- [x] `demo/invoke/SdkAiInvoke.java`
- [x] `demo/invoke/HttpAiInvoke.java`
- [x] `demo/invoke/SpringAiAiInvoke.java`
- [x] `demo/invoke/OllamaAiInvoke.java`
- [x] `demo/invoke/LangChainAiInvoke.java`
- [x] `demo/invoke/TestApiKey.java`

**验收**:
- 5 个 Demo 都能 `main()` 跑通
- application.yml 配 DASHSCOPE_API_KEY 后能调通百炼

---

## T-02 · ChatClient 多轮对话 + 文件记忆 ✅

**目标**:实现 ChatClient 抽象层的多轮对话,文件持久化

**对应笔记**:Ch 03

**实现**:
- [x] `app/TravelApp.java`(构造 ChatClient + 注册 advisor)
- [x] `chatmemory/FileBasedChatMemory.java`(Kryo 序列化)
- [x] `advisor/MyLoggerAdvisor.java`
- [x] 结构化报告 `TravelReport` record

**验收**:
- `travelApp.doChatWithId("hi", "chat-1")` 返回结果
- 第二次调 `doChatWithId("上次说什么", "chat-1")` 能引用上下文

---

## T-03 · RAG 知识库问答 ✅

**目标**:基于本地 md 攻略做检索增强问答

**对应笔记**:Ch 04

**实现**:
- [x] `config/TravelVectorStoreConfig.java`
- [x] `rag/TravelDocumentLoader.java`
- [x] `service/TravelRagChatService.java`
- [x] `config/TravelRagCloudAdvisorConfig.java`
- [x] `controller/TravelRagController.java`
- [x] 3 份知识库 md(国内低 / 中 / 海外预算)

**验收**:
- `POST /api/travel/rag-chat` 能调用
- 答案引用本地文档(可在响应 metadata 中看到)

---

## T-04 · 工具调用(7 个 @Tool + 安全) ✅

**目标**:8 个 @Tool 实现 + ToolRegistration 统一注册 + 输入校验

**对应笔记**:Ch 06

**实现**:
- [x] FileOperationTool
- [x] WebSearchTool(SerpAPI)
- [x] WebScrapingTool(jsoup)
- [x] TerminalTool(白名单)
- [x] DownloadTool
- [x] PdfGenerationTool(iText)
- [x] WeatherTool(wttr.in,免 key)
- [x] TerminateTool(Ch 08 用)
- [x] AskHumanTool(Ch 08 补)

**验收**:
- 工具通过 `@Tool` + `@ToolParam` 自动注册
- TerminalTool 命令白名单生效(rm -rf / 应该被拒)
- 每个工具输入做基本校验(URL 格式 / 文件名)

---

## T-05 · AI 智能体 4 层架构 ✅

**目标**:实现 OpenManus 风格 4 层继承架构

**对应笔记**:Ch 08

**实现**:
- [x] `agent/BaseAgent`(state + run + runStream + StateContext + LoopGuard)
- [x] `agent/ReActAgent`(think/act 抽象方法)
- [x] `agent/ToolCallAgent`(完整 think→act 实现)
- [x] `agent/YuTravelAgent`(`@Component` 成品)
- [x] `agent/enums/AgentState`(IDLE/RUNNING/FINISHED/ERROR)
- [x] `agent/context/StateContext`(AutoCloseable)
- [x] `agent/LoopGuard`(死循环检测)
- [x] `agent/workflow/ChainAgent`(提示链)

**验收**:
- 4 层继承链路清晰
- `YuTravelAgent.run("规划 3 天杭州")` 能跑
- 死循环检测生效(连续 3 次同工具 / 5 次无进展触发 ERROR)

---

## T-06 · SSE 流式接口 ⚠️

**目标**:把 AI 能力封装成可流式调用的 HTTP 接口

**对应笔记**:Ch 09 §3-§4

**实现**:
- [x] 普通聊天 SSE(`AiController.doChatWithSSE`)
- [x] SseEmitter 备用(`AiController.doChatWithSseEmitter`)
- [x] Agent 流式入口(`BaseAgent.runStream()`)
- [x] AgentController(代码就绪,**运行时 404 需修**)
- [x] CORS 配置(CorsConfig)
- [x] TravelApp 流式方法(`doChatByStream`)

**剩余问题**:
- [ ] AgentController 在 IDEA Run 下需要 Rebuild 后生效
- [ ] AgentController 注册后需 curl 验证 `/api/agent/sse`

---

## T-07 · TDD 测试覆盖 ❌ **当前任务**

**目标**:为每个核心功能写 JUnit 5 + Spring Boot Test

**对应章节**:Ch 02~09 全部

**子任务**:

| 子 ID | 标题 | 测试方法 | 优先级 |
|---|---|---|---|
| T-07.1 | **SSE 端到端流式测试** | MockMvc + EventSource 客户端 | **P0** |
| T-07.2 | 普通聊天 Controller 测试 | MockMvc | P1 |
| T-07.3 | 结构化报告测试 | MockMvc + JSON 断言 | P1 |
| T-07.4 | RAG Chat Service 测试 | @SpringBootTest + Mock ChatModel | P1 |
| T-07.5 | Re-ranking 单元测试 | 直接 new RagRetrieverEnhancer | P2 |
| T-07.6 | 7 个 @Tool 单元测试 | 直接 new + 反射调 | P2 |
| T-07.7 | Agent 4 层架构测试 | Step 模拟 + LoopGuard | P2 |
| T-07.8 | AskHumanTool 测试 | 临时目录 + 验证文件写入 | P3 |

**T-07.1 详细验收**(本任务最重要的):
```java
@Test
void test_chatSse_returnsTextEventStream() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/travel/chat/sse")
        .param("message", "你好")
        .param("chatId", "test-1"))
        .andExpect(request().asyncStarted())
        .andReturn();

    // 等 SSE 输出
    AsyncContext ctx = result.getRequest().getAsyncContext();
    SseEmitter emitter = (SseEmitter) ctx.getResponse();
    // ... 验证至少收到一个 data: 帧
}
```

**关键约束**(来自 CLAUDE.md):
- 测试必须**真实**(不能全 mock)
- SSE 必须验证**流式特性**(不是一次性返回)
- 测试用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 拿真实端口
- 测试间共享 `ChatModel` Bean,避免重复 embedding

**依赖**:
- 已实现代码(T-01~T-06)
- 测试用户:确认真 AI / mock AI 都可

**完成定义**:
- [ ] T-07.1 通过(端到端 SSE)
- [ ] T-07.2~07.4 通过(Controller / RAG)
- [ ] T-07.5~07.8 通过(工具 / Agent 单元)
- [ ] `mvn test` 全绿
- [ ] 至少 1 个测试截图 / 输出留档

---

## T-08 · 联调 + Docker 部署验证 ❌ **当前任务**

**目标**:全栈跑通 + 容器化构建可成功

**子任务**:

| 子 ID | 标题 | 验证方法 | 优先级 |
|---|---|---|---|
| T-08.1 | **后端 IDEA Run 启动验证** | 看启动日志 + curl | **P0** |
| T-08.2 | 前端 `npm run dev` + vite proxy | 浏览器访问 + curl | P0 |
| T-08.3 | 端到端 SSE 联调(浏览器打字机) | 手动浏览器验证 | P1 |
| T-08.4 | `mvn package` + `docker build` | 本地镜像构建成功 | P1 |
| T-08.5 | Dockerfile 启动 + curl | 容器内端口 8123 | P2 |
| T-08.6 | 前端 Dockerfile | 前端镜像构建 | P2 |
| T-08.7 | 部署文档更新 | README + CLAUDE.md | P3 |

**T-08.1 详细验收**(本任务最重要的):
- 后端用 IDEA Run 启动
- 看到 `Started AiTravelPlannerApplication in X.X seconds`
- curl `GET /api/travel/chat/sse?message=test&chatId=t` 返回 `data:` 帧
- curl `GET /api/agent/sse?message=test` 返回步骤日志

**完成定义**:
- [ ] T-08.1~T-08.3 通过
- [ ] T-08.4 后端 docker build 成功
- [ ] Dockerfile / nginx.conf 都在仓库
- [ ] STATUS.md 列出当前能用 / 不能用的端点

---

## 任务依赖图

```
T-01 → T-02 → T-03 → T-04 → T-05 → T-06 ─┬─→ T-07 ─┐
                                          │         ├─→ T-08
                                          └─────────┘
```

**当前状态**:
- T-01~T-06 已完成(笔记 + 代码)
- T-06 残留问题:AgentController 运行时 404
- T-07 / T-08 待开始

---

## 任务状态变更记录

| 日期 | 任务 | 变更 |
|---|---|---|
| 2026-07-17 | T-06 | 完成 BaseAgent.runStream + AgentController 代码 |
| 2026-07-18 | T-06 | 发现 AgentController 运行时 404(classpath 缓存) |
| 2026-07-18 | T-07 | **开始**:5 项补全(Re-ranking/MCP/AskHuman/Dockerfile/AskHuman) |
| TBD | T-08 | 等 T-07 完成 |

---

## 推荐执行顺序

1. **T-07.1 端到端 SSE 测试**(最重要)
2. **T-08.1 后端 Run 验证**(解 AgentController 404)
3. **T-07.2~07.8 单元测试**(在 SSE 通了后批量做)
4. **T-08.3 浏览器联调**(再回头看前端)
5. **T-08.4~08.6 Docker 构建**(最后)

---

*最后更新:2026-07-18*