# TEST_CASES.md — 测试用例清单

> **测试策略**:端到端 SSE 流式输出为主,辅以单元 / Controller 测试。
> **测试框架**:JUnit 5 + Spring Boot Test + MockMvc + Awaitility
> **运行**:`mvn test`
> **当前状态**:所有用例**待写**;T-07 任务推进

---

## 一、测试金字塔

```
            ┌─────────────┐
            │  E2E 浏览器  │   ← T-08.3 手动(慢)
            └─────────────┘
       ┌─────────────────────┐
       │  集成(SSE 端到端)   │   ← T-07.1 (P0,重点)
       └─────────────────────┘
   ┌─────────────────────────────┐
   │  单元(Controller/Tool/Agent)│   ← T-07.2~07.8
   └─────────────────────────────┘
```

**当前 T-07 任务重点**:**集成层(SSE 端到端)** + **单元层(Controller + 关键 Tool)**。

---

## 二、SSE 端到端测试(P0)

### TC-SSE-01: 普通聊天 Flux SSE 流式

**目标**:验证 `GET /api/travel/chat/sse?message=...&chatId=...` 返回 `text/event-stream`

**测试方法**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ChatSseIntegrationTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    @Test
    void chatSse_returnsTextEventStream() {
        ResponseEntity<EventSource> resp = rest.execute(
            "http://localhost:" + port + "/api/travel/chat/sse?message=你好&chatId=t1",
            HttpMethod.GET, null, EventSource::new);

        // 1. 验证 Content-Type
        List<MediaType> contentTypes = resp.getHeaders().getContentType().;
        assertTrue(contentTypes.toString().contains("text/event-stream"));

        // 2. 验证收到至少 1 个 data: 帧
        // (用 CountDownLatch 等流式输出)
    }
}
```

**验收**:
- [ ] Content-Type = `text/event-stream`
- [ ] 至少收到 1 个 `data:` 帧
- [ ] 帧内容包含"你好"等 AI 回复片段
- [ ] 3~5 秒内流结束(非无限挂起)

---

### TC-SSE-02: 普通聊天 SseEmitter 备用

**目标**:验证 `GET /api/travel/chat/sse/emitter?message=...&chatId=...` 工作

**验收**:
- [ ] 返回 SseEmitter
- [ ] 收到 `data:` 帧
- [ ] 完成后 connection 关闭

---

### TC-SSE-03: Agent 多步 SSE 流式

**目标**:验证 `GET /api/agent/sse?message=...` 返回步骤日志

**验收**:
- [ ] Content-Type = `text/event-stream`
- [ ] 收到 `event: step` 帧(多步)
- [ ] 收到 `event: tool` 帧(工具调用)
- [ ] 收到 `event: done`(任务完成)
- [ ] 如果死循环触发,收到 `event: loop` + `[LoopGuard]` 提示

---

### TC-SSE-04: 异常路径

**目标**:验证后端异常时 SSE 也能优雅关闭

**场景**:
- [ ] 缺 `message` 参数 → 400 Bad Request
- [ ] `message` 为空字符串 → 后端兜底
- [ ] `message` 含特殊字符(`%` / 中文) → URL encode 后正常

---

## 三、Controller 测试(P1)

### TC-CTRL-01: POST /api/travel/chat 同步聊天

**测试**:
```java
@Test
void chat_returnsReply() {
    ChatRequest req = new ChatRequest();
    req.setMessage("北京3月天气");
    req.setConversationId("c1");

    BaseResponse<ChatResponse> resp = rest.postForObject(
        "/api/travel/chat", req, BaseResponse.class);

    assertEquals(0, resp.getCode());
    assertNotNull(resp.getData().getReply());
}
```

**验收**:
- [ ] code = 0
- [ ] data.reply 非空
- [ ] data.conversationId 正确(等于入参)

---

### TC-CTRL-02: POST /api/travel/report 结构化报告

**验收**:
- [ ] 返回 title 非空
- [ ] 返回 suggestions 数组非空
- [ ] JSON schema 正确(报告类)

---

### TC-CTRL-03: POST /api/travel/rag-chat

**验收**:
- [ ] 收到基于知识库的回答
- [ ] 异常时返回 NO_MATCH_REPLY 兜底

---

### TC-CTRL-04: GET /api/agent/sse **重点**

**验收**:
- [ ] HTTP 200,不是 404(classpath 缓存修了后)
- [ ] 返回 `text/event-stream`
- [ ] 至少 1 个 `step` 事件

**这个测试是 T-07 任务的核心** —— 通过它验证 AgentController 是否真的生效。

---

## 四、RAG 测试(P1)

### TC-RAG-01: TravelRagChatService 单元测试

**测试**:
```java
@SpringBootTest
class TravelRagChatServiceTest {
    @Autowired TravelRagChatService ragService;

    @Test
    void chatWithRag_returnsAnswer() {
        String answer = ragService.chatWithRag("杭州西湖几月去最好");
        assertNotNull(answer);
        assertFalse(answer.isBlank());
    }
}
```

**验收**:
- [ ] 非空回复
- [ ] 异常不抛出(兜底 NO_MATCH_REPLY)

---

### TC-RAG-02: Re-ranking 单元测试(P2)

```java
@Test
void rerank_putsRelevantDocFirst() {
    RagRetrieverEnhancer enhancer = new RagRetrieverEnhancer();
    Document relevant = new Document("杭州西湖以春日景色闻名");
    Document irrelevant = new Document("北京烤鸭很好吃");

    List<Document> reranked = enhancer.rerank(
        "杭州西湖几月去最好",
        List.of(irrelevant, relevant), 5);

    assertEquals(relevant, reranked.get(0));
}
```

**验收**:
- [ ] 相关文档排在前面
- [ ] topK 参数生效
- [ ] 空 query / 空 docs 不抛异常

---

### TC-RAG-03: 缓存命中

```java
@Test
void retrieveWithCache_cachesResult() {
    RagRetrieverEnhancer enhancer = new RagRetrieverEnhancer();
    AtomicInteger callCount = new AtomicInteger(0);
    Function<String, List<Document>> retriever = q -> {
        callCount.incrementAndGet();
        return List.of(new Document("doc"));
    };

    enhancer.retrieveWithCache("q1", retriever, 5);
    enhancer.retrieveWithCache("q1", retriever, 5);
    enhancer.retrieveWithCache("q1", retriever, 5);

    assertEquals(1, callCount.get());  // 只调 1 次
}
```

**验收**:
- [ ] 同一 query 只触发 1 次 retriever
- [ ] `clearCache()` 后再次触发

---

## 五、Tool 单元测试(P2)

### TC-TOOL-01~08: 8 个 Tool

每个工具 1 个测试文件,覆盖:
- [ ] happy path(正常参数调用)
- [ ] 异常路径(空参数 / 非法参数)
- [ ] 输入校验(URL 格式 / 文件名 / 命令白名单)

**重点**:
- [ ] **TC-TOOL-04 TerminalTool**:白名单外的命令应该被拒
- [ ] **TC-TOOL-08 AskHumanTool**:写入 askhuman-pending.md,内容含问题
- [ ] **TC-TOOL-09 WeatherTool**:wttr.in 调用 mock(避免真实网络)

---

## 六、Agent 4 层架构测试(P2)

### TC-AGENT-01: BaseAgent.run 循环

```java
@Test
void run_executesStepsUntilMaxOrFinished() {
    // Mock 1 个 step 1 次后改 state = FINISHED
    // 验证 run() 返回,只调 1 次 step
}
```

**验收**:
- [ ] maxSteps 生效
- [ ] state 变更退出循环

---

### TC-AGENT-02: LoopGuard 死循环检测

```java
@Test
void loopGuard_detectsSameToolThreeTimes() {
    LoopGuard guard = new LoopGuard();
    guard.recordThink(true);
    guard.recordTool("getWeather");
    guard.recordTool("getWeather");
    guard.recordTool("getWeather");
    assertTrue(guard.isLooping());
}
```

**验收**:
- [ ] 连续 3 次同工具触发
- [ ] 连续 5 次无进展触发
- [ ] 混合调用不触发

---

### TC-AGENT-03: StateContext 状态切换

**验收**:
- [ ] `try (var ctx = StateContext.of(agent, RUNNING))` → 进入 RUNNING
- [ ] close 后:未到 FINISHED/ERROR → 回到 IDLE
- [ ] 异常 → ERROR

---

## 七、运行测试

```bash
# 跑全部
mvn test

# 跑单个类
mvn test -Dtest=ChatSseIntegrationTest

# 跑单个方法
mvn test -Dtest=ChatSseIntegrationTest#chatSse_returnsTextEventStream

# 跑集成测试(慢)
mvn verify -Pintegration
```

---

## 八、覆盖率目标

| 模块 | 行覆盖率 | 分支覆盖率 |
|---|---|---|
| Controller | ≥ 80% | ≥ 70% |
| Service | ≥ 70% | ≥ 60% |
| Agent 4 层 | ≥ 80% | ≥ 70% |
| Tool | ≥ 70% | ≥ 60% |
| RAG | ≥ 60% | ≥ 50% |
| **整体** | **≥ 70%** | **≥ 60%** |

---

## 九、当前进度

| 用例 ID | 描述 | 状态 |
|---|---|---|
| TC-SSE-01~04 | SSE 端到端 | ❌ |
| TC-CTRL-01~04 | Controller | ❌ |
| TC-RAG-01~03 | RAG | ❌ |
| TC-TOOL-01~09 | 8 个 Tool | ❌ |
| TC-AGENT-01~03 | Agent 4 层 | ❌ |
| **合计 21 个** | | **0/21 完成** |

---

## 十、变更记录

| 日期 | 变更 |
|---|---|
| 2026-07-18 | 初版:21 个测试用例,聚焦 SSE 端到端 |

---

*最后更新:2026-07-18*