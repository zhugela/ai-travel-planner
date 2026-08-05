# AI 旅行规划助手 — 后端测试规范与流程

> **适用项目**: `ai-travel-planner` (Spring Boot 3.4.4, Java 21/25, Spring AI 1.0.0, JUnit 5)
> **最后更新**: 2026-08-03
> **关联文档**: REQUIREMENTS.md / SCHEMA.md / TASKS.md / TEST_CASES.md

---

## 一、测试分层策略

```
                 ┌──────────────────────┐
                 │  E2E · 手工浏览器验证  │   ← 优先级 P2，提测前跑
                 └──────────────────────┘
            ┌────────────────────────────────┐
            │  集成测试 · SSE 端到端 + MockMvc  │   ← 优先级 P0，每次改代码必跑
            └────────────────────────────────┘
       ┌──────────────────────────────────────────┐
       │  单元测试 · Controller / Service / Tool     │   ← 优先级 P1，写新功能就写
       └──────────────────────────────────────────┘
   ┌──────────────────────────────────────────────────┐
   │  单元测试 · Agent 4层 / LoopGuard / RAG / 工具类    │   ← 优先级 P2，逐步补
   └──────────────────────────────────────────────────┘
```

**铁律**: 集成测试（SSE 端到端）是 P0 核心——这个项目是 AI Agent + 流式响应，传统单元测试覆盖不到 SSE 帧序列和 Agent 多步推理。

---

## 二、测试环境速查

### 2.1 测试前提条件

```bash
# 1. 确认 JDK 版本（建议 JDK 26，避免 HttpClient bug）
java -version

# 2. 确认环境变量
echo $DASHSCOPE_API_KEY    # 必须设置，否则 AI 调用全部失败
echo $SEARCH_API_KEY        # 可选，搜索工具用

# 3. 确认端口未被占用
netstat -ano | findstr 8123
```

### 2.2 测试专用配置

项目已有 `src/test/resources/application-test.properties`。如需额外覆盖，建议新建 `src/test/resources/application-test.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
    mcp:
      client:
        enabled: false       # 测试环境禁 MCP
    ollama:
      chat:
        enabled: false

logging:
  level:
    com.yupi.aitravelplanner: debug
```

### 2.3 Maven 运行命令速查

```bash
mvn test                                         # 跑全部测试
mvn test -Dtest=AiControllerTest                 # 跑单个类
mvn test -Dtest=AiControllerTest#chat_returnsReply  # 跑单个方法
mvn compile -DskipTests                          # 跳过测试
```

---

## 三、测试用例编写规范

### 3.1 命名规范

```
测试类: {被测类名}Test.java
测试方法: {方法名}_{场景}_{预期结果}

示例:
  class AiControllerTest {
      @Test void chat_emptyMessage_throwsParamError() { }
      @Test void chat_validRequest_returnsReply() { }
      @Test void chatSse_streaming_returnsMultipleFrames() { }
  }
```

### 3.2 测试类模板（SSE 集成测试）

```java
package com.yupi.aitravelplanner.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("AiController 集成测试")
class AiControllerTest {

    @LocalServerPort private int port;
    @Autowired private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @Test
    @DisplayName("POST /travel/chat — 正常请求返回 AI 回复")
    void chat_validRequest_returnsReply() {
        ChatRequest req = new ChatRequest();
        req.setMessage("北京3月天气怎么样？");
        req.setConversationId("test-" + System.currentTimeMillis());

        var resp = restTemplate.postForEntity(
            baseUrl() + "/travel/chat", req, BaseResponse.class);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody().getCode()).isEqualTo(0);
        assertThat(resp.getBody().getData()).isNotNull();
    }
}
```

### 3.3 SSE 流式测试关键代码

```java
@Test
@DisplayName("SSE 流式 — 返回 text/event-stream 并收到至少3帧")
void chatSse_returnsStream() throws Exception {
    String url = baseUrl() + "/travel/chat/sse?message=你好&chatId=t1";

    var frames = restTemplate.execute(url, HttpMethod.GET, null,
        clientHttpResponse -> {
            // 校验 Content-Type
            String ct = clientHttpResponse.getHeaders().getFirst("Content-Type");
            assertThat(ct).contains("text/event-stream");

            // 读流
            var reader = new BufferedReader(
                new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
            List<String> dataFrames = new ArrayList<>();
            String line;
            long deadline = System.currentTimeMillis() + 30_000;
            while ((line = reader.readLine()) != null
                   && System.currentTimeMillis() < deadline) {
                if (line.startsWith("data:")) dataFrames.add(line);
            }
            assertThat(dataFrames.size()).isGreaterThanOrEqualTo(3);
            return dataFrames;
        });
}
```

---

## 四、分模块测试清单

### 4.1 AiController（P0 · 最高优先级）

| 用例ID | 接口 | 验证要点 |
|---|---|---|
| TC-AI-01 | POST /travel/chat | code=0, reply 非空, conversationId 回显 |
| TC-AI-02 | POST /travel/chat (空消息) | code≠0（参数校验） |
| TC-AI-03 | POST /travel/report | title 非空, suggestions[] 非空 |
| TC-AI-04 | GET /travel/chat/sse | Content-Type=text/event-stream, 收到多帧 |
| TC-AI-05 | GET /travel/chat/sse/emitter | SseEmitter 正常推送 |
| TC-AI-06 | GET /travel/chat/sse (缺参) | 400 Bad Request |
| TC-AI-07 | GET /travel/chat/sse (中文参数) | URL encode 后正常返回 |

### 4.2 AgentController（P0 · Agent SSE 事件协议）

| 用例ID | 验证要点 |
|---|---|
| TC-AGT-01 | HTTP 200, text/event-stream |
| TC-AGT-02 | 收到 `event:step` 帧 |
| TC-AGT-03 | 收到 `event:tool` 帧 |
| TC-AGT-04 | 收到 `event:done`（任务完成） |
| TC-AGT-05 | 空消息参数校验 |

**SSE 事件协议**：

| event name | 含义 | data 内容 |
|---|---|---|
| `step` | 思考步骤 | "思考: {thought}" |
| `tool` | 工具调用 | "{toolName}({args}) → {result}" |
| `loop` | 死循环警告 | "[LoopGuard] {message}" |
| `error` | 错误 | "{errorMessage}" |
| `done` | 完成 | 最终回复文本 |

### 4.3 TravelRagController（P1）

| 用例ID | 验证要点 |
|---|---|
| TC-RAG-01 | matched=true, answer 非空 |
| TC-RAG-02 | matched=false, answer=兜底回复 |
| TC-RAG-03 | 空 question → code≠0 |

### 4.4 Agent 4 层架构（P2 · 纯单元测试，不依赖 AI）

| 被测类 | 验证要点 |
|---|---|
| BaseAgent | state 状态机: IDLE→RUNNING→FINISHED |
| BaseAgent | maxSteps 上限生效 |
| LoopGuard | 连续 3 次同工具 → isLooping=true |
| LoopGuard | 混合调用不误触发 |
| LoopGuard | 连续 5 次无进展 → 触发 |
| StateContext | try-with-resources 自动 restore |

**LoopGuard 测试示例**：

```java
@Test
@DisplayName("连续3次相同工具 → 检测到死循环")
void recordTool_3sameTool_detectsLoop() {
    LoopGuard guard = new LoopGuard();
    guard.recordTool("getWeather");
    guard.recordTool("getWeather");
    guard.recordTool("getWeather");
    assertThat(guard.isLooping()).isTrue();
}

@Test
@DisplayName("混合工具调用 → 不误触发")
void recordTool_mixedTools_noFalsePositive() {
    LoopGuard guard = new LoopGuard();
    guard.recordTool("getWeather");
    guard.recordTool("searchWeb");
    guard.recordTool("getWeather");
    guard.recordTool("searchWeb");
    guard.recordTool("getWeather");
    assertThat(guard.isLooping()).isFalse();
}
```

### 4.5 Tools 工具测试（P2）

| 被测Tool | Happy Path | 异常 Path |
|---|---|---|
| FileOperationTool | 读/写文件正常 | 非法路径 |
| WebSearchTool | 搜索返回结果 | 空关键词 |
| WebScrapingTool | 抓取网页 | 无效 URL |
| TerminalTool | 白名单命令执行 | **白名单外拒绝**（安全重点） |
| DownloadTool | 下载文件 | 无效 URL |
| PdfGenerationTool | 生成PDF | 空内容 |
| WeatherTool | 查天气（mock wttr.in） | 网络超时 |
| TerminateTool | 标记任务结束 | — |
| AskHumanTool | 写入 askhuman-pending.md | 空问题 |

### 4.6 RAG 检索测试（P2）

| 验证要点 |
|---|
| 命中知识库 → 返回引用内容 |
| 无匹配 → 返回兜底回复 |
| Re-rank: 相关文档排在前面 |
| 缓存: 同一 query 只触发 1 次检索 |
| clearCache() 后重新检索 |

### 4.7 MCP Image-Search 子模块（独立测试）

```bash
cd mcp-servers/image-search
mvn test
# 不依赖 DashScope API Key，可独立运行
```

---

## 五、完整测试执行流程（提测 SOP）

### 第一步：环境检查

```bash
java -version                           # 预期 21+
echo $DASHSCOPE_API_KEY                 # 必须设置
netstat -ano | findstr 8123             # 确认端口空闲
```

### 第二步：编译

```bash
cd D:\code2\ai-travel-planner
mvn compile                             # 预期 BUILD SUCCESS
```

### 第三步：跑测试（按优先级分批）

```bash
# 第一轮：P0 集成测试（SSE）—— 耗时 30~120s
mvn test -Dtest=AiControllerTest

# 第二轮：P1 Controller 测试
mvn test -Dtest=TravelRagControllerTest

# 第三轮：P2 纯单元测试（不依赖 AI API）
mvn test -Dtest=LoopGuardTest,StateContextTest

# 第四轮：P2 工具测试
mvn test -Dtest=TerminalToolTest,WeatherToolTest
```

### 第四步：结果判断

| 结果 | 行动 |
|---|---|
| 全部 PASS | ✅ 可以部署/提测 |
| P0 有 FAIL | ❌ 优先排查 AI API / 网络 / 配置 |
| P2 有 FAIL | ⚠️ 记录 TODO，不阻塞提测 |
| 超时 | ⚠️ 检查 DashScope 是否欠费/限流 |

---

## 六、AI 调用的 Mock 策略

P0 集成测试用真实 API，P1/P2 单元测试用 Mock：

```java
@SpringBootTest
class MyServiceTest {
    @MockitoBean                         // Spring Boot 3.4+ 支持
    private ChatModel chatModel;

    @Test
    void testWithoutRealAi() {
        when(chatModel.call(anyString())).thenReturn("Mock AI 回复");
        // 正常走业务逻辑，AI 调用被拦截
    }
}
```

---

## 七、常见问题排查表

| 症状 | 可能原因 | 排查命令 |
|---|---|---|
| 测试都不跑 | 类名不以 Test 结尾 | 检查命名 |
| `UnsatisfiedDependencyException` | Spring 自动装配失败 | 检查 @SpringBootTest 和缺失 Bean |
| AI 调用返回空 | API Key 未设/过期 | `echo $DASHSCOPE_API_KEY` |
| AI 调用超时 | 网络/模型限流 | 设更长超时 |
| `Connection refused` | 后端没起来 | 检查端口 8123 |
| Agent SSE 404 | classpath 缓存 | IDEA → Build → Rebuild Project |
| Maven 锁 Java 21 | pom.xml 声明 | 设 `JAVA_HOME` 指向 JDK 26 |

---

## 八、现有测试盘点 & 覆盖率目标

### 8.1 现有测试（14 个文件，主模块）

| 文件 | 质量 |
|---|---|
| `AiTravelPlannerApplicationTests.java` | ✅ 启动上下文 |
| `TravelAppTest.java` | ⚠️ 断言弱（只有 assertNotNull） |
| `TravelRagChatServiceTest.java` | ⚠️ 同上 |
| `BaseAgentTest.java` (test/agent/) | ⚠️ 仅 1 个测试，断言弱 |
| `FileBasedChatMemoryTest.java` | ✅ |
| `PromptTemplateDemoTest.java` | ✅ |
| `TerminalToolTest.java` | ✅ 安全测试好 |
| `WeatherToolTest.java` 等 6 个 Tool | ✅ |
| MCP: 2 个测试文件 | ✅ 独立可跑 |

### 8.2 缺失测试（必须补，按优先级）

| 优先级 | 缺失 | 重要性 |
|---|---|---|
| **P0** | AiController SSE 集成测试 | 核心打字机流式输出 |
| **P0** | AgentController SSE 集成测试 | Agent 多步推理 + 事件协议 |
| **P0** | TravelRagController 接口测试 | RAG 问答参数校验 |
| **P1** | LoopGuard 单元测试 | 死循环检测（不依赖 AI） |
| **P1** | StateContext 单元测试 | 状态机正确性 |
| **P2** | GlobalExceptionHandler 测试 | 异常兜底 |

### 8.3 覆盖率目标

| 模块 | 行覆盖率 | 分支覆盖率 | 当前估算 |
|---|---|---|---|
| Controller 层 | ≥ 80% | ≥ 70% | ~0% |
| Service 层 | ≥ 70% | ≥ 60% | ~20% |
| Agent 4 层 | ≥ 80% | ≥ 70% | ~5% |
| Tool 7 个 | ≥ 70% | ≥ 60% | ~40% |
| RAG 组件 | ≥ 60% | ≥ 50% | ~20% |
| **整体** | **≥ 70%** | **≥ 60%** | **~15%** |

---

## 九、测试文件目标目录结构

> ✅ = 已有  ❌ = 待创建

```
src/test/java/com/yupi/aitravelplanner/
├── AiTravelPlannerApplicationTests.java    ✅
├── TravelAppTest.java                      ✅ (可加强)
├── controller/
│   ├── AiControllerTest.java               ❌ P0
│   ├── AgentControllerTest.java             ❌ P0
│   └── TravelRagControllerTest.java         ❌ P1
├── agent/
│   ├── LoopGuardTest.java                   ❌ P1
│   ├── StateContextTest.java                ❌ P1
│   ├── BaseAgentTest.java                   ❌ (移到这)
│   └── ToolCallAgentTest.java               ❌ P2
├── tools/
│   ├── DownloadToolTest.java                ✅
│   ├── FileOperationToolTest.java           ✅
│   ├── PdfGenerationToolTest.java           ✅
│   ├── TerminalToolTest.java                ✅
│   ├── WeatherToolTest.java                 ✅
│   ├── WebScrapingToolTest.java             ✅
│   ├── WebSearchToolTest.java               ✅
│   ├── TerminateToolTest.java               ❌
│   └── AskHumanToolTest.java                ❌
├── rag/
│   └── RagRetrieverEnhancerTest.java        ❌ P2
├── service/
│   └── TravelRagChatServiceTest.java        ✅
└── common/
    ├── GlobalExceptionHandlerTest.java      ❌ P2
    └── ResultUtilsTest.java                 ❌ P2
```

---

## 十、变更记录

| 日期 | 变更 |
|---|---|
| 2026-08-03 | 初版：完整测试规范，覆盖分层策略、用例清单、执行 SOP、排查表 |
