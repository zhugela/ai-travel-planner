# AI Travel Planner Code Wiki

## 1. 项目概述

### 1.1 项目简介

AI Travel Planner 是一个基于 **Spring Boot 3.4** 和 **Spring AI** 构建的智能旅行规划应用。该应用集成了阿里云 DashScope 大模型服务，能够为用户提供个性化的旅行规划建议，支持多轮对话和结构化旅行报告输出。

### 1.2 核心功能

| 功能模块 | 说明 |
| :--- | :--- |
| 多轮对话 | 支持上下文保持的智能对话，自动提取用户姓名 |
| 旅行规划 | 根据用户需求生成详细的分天行程计划 |
| 结构化报告 | 输出 JSON 格式的旅行报告 |
| 会话记忆 | 基于文件持久化的对话历史存储，重启后可恢复 |

### 1.3 技术栈

| 分类 | 技术 | 版本 |
| :--- | :--- | :--- |
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.4.4 |
| AI SDK | Spring AI | 1.0.0 |
| 大模型服务 | DashScope（阿里云） | 2.19.1 |
| 本地模型 | Ollama | - |
| 序列化 | Kryo | 5.6.2 |
| 文档 | SpringDoc OpenAPI | 2.8.9 |
| 工具库 | Hutool | 5.8.37 |

---

## 2. 项目架构

### 2.1 模块结构

```plaintext
src/main/java/com/yupi/aitravelplanner/
├── advisor/          # ChatClient 顾问组件
├── app/              # 核心业务逻辑（TravelApp）
├── chatmemory/       # 对话记忆持久化
├── common/           # 通用响应封装
├── config/           # 配置类
├── constant/         # 常量定义
├── controller/       # REST API 控制器
├── demo/             # 示例代码（invoke/RAG）
├── exception/        # 异常处理
├── model/dto/        # 请求/响应 DTO
├── utils/            # 工具类
├── AiTravelPlannerApplication.java  # 启动类
└── HealthController.java            # 健康检查
```

### 2.2 核心流程图

```
用户请求 → AiController → TravelApp → ChatClient → DashScope API
                                                  ↓
                                            FileBasedChatMemory
                                            (会话持久化)
```

### 2.3 模块职责说明

| 模块 | 职责 | 关键类 |
| :--- | :--- | :--- |
| `advisor` | ChatClient 的 AOP 增强，如日志记录 | `MyLoggerAdvisor` |
| `app` | 封装 AI 对话逻辑，提供业务方法 | `TravelApp` |
| `chatmemory` | 实现 ChatMemory 接口，持久化对话历史 | `FileBasedChatMemory` |
| `common` | 统一响应格式和工具方法 | `BaseResponse`, `ResultUtils` |
| `config` | Spring 配置类，如跨域配置 | `CorsConfig` |
| `constant` | 常量定义，如文件路径 | `FileConstant` |
| `controller` | REST API 入口，参数校验 | `AiController` |
| `exception` | 自定义异常和全局异常处理 | `BusinessException`, `GlobalExceptionHandler` |
| `model/dto` | 请求和响应的数据传输对象 | `ChatRequest`, `ChatResponse`, `TravelReportResponse` |

---

## 3. 关键类与函数说明

### 3.1 TravelApp（核心业务类）

**位置**: `src/main/java/com/yupi/aitravelplanner/app/TravelApp.java`

**职责**: 封装 ChatClient + System Prompt + 多轮对话记忆，提供旅行规划的核心业务方法。

**核心方法**:

| 方法名 | 功能说明 | 参数 | 返回值 |
| :--- | :--- | :--- | :--- |
| `doChat` | 多轮对话，返回回复文本 | `message`: 用户消息<br>`conversationId`: 会话ID | `String` - AI回复 |
| `doChatWithId` | 多轮对话，返回会话ID和回复 | `message`: 用户消息<br>`conversationId`: 会话ID | `ChatResult` |
| `doChatWithTravelReport` | 生成结构化旅行报告 | `message`: 用户消息<br>`conversationId`: 会话ID | `TravelReport` |
| `doChatWithTravelReportAndId` | 生成报告并返回会话ID | `message`: 用户消息<br>`conversationId`: 会话ID | `TravelReportResult` |

**内部方法**:

| 方法名 | 功能说明 |
| :--- | :--- |
| `renderSystemPrompt` | 渲染 System Prompt 模板，替换 `{today}` 和 `{userName}` |
| `extractUserName` | 从用户消息中提取姓名（正则匹配："我叫XX"或"我是XX"） |
| `resolveConversationId` | 解析会话ID，为空则生成UUID |

**内部记录类**:

```java
// 聊天结果
public record ChatResult(String conversationId, String reply) {}

// 旅行报告（结构化输出）
public record TravelReport(String title, List<String> suggestions) {}

// 报告结果
public record TravelReportResult(String conversationId, TravelReport report) {}
```

### 3.2 AiController（REST API 控制器）

**位置**: `src/main/java/com/yupi/aitravelplanner/controller/AiController.java`

**职责**: 对外暴露 REST API，处理 HTTP 请求和响应。

**API 接口**:

| 接口路径 | HTTP 方法 | 功能 |
| :--- | :--- | :--- |
| `/api/travel/chat` | POST | 旅行规划多轮对话 |
| `/api/travel/report` | POST | 旅行规划结构化报告 |

**接口详情**:

**POST /api/travel/chat**

请求体:
```json
{
  "message": "我想去北京玩3天",
  "conversationId": "可选，不传则自动生成"
}
```

响应体:
```json
{
  "code": 0,
  "data": {
    "conversationId": "uuid-string",
    "reply": "好的，请问您的预算大概是多少呢？"
  },
  "message": "ok"
}
```

**POST /api/travel/report**

请求体:
```json
{
  "message": "我叫张三，想从上海去杭州玩2天，预算2000元",
  "conversationId": "可选"
}
```

响应体:
```json
{
  "code": 0,
  "data": {
    "conversationId": "uuid-string",
    "title": "张三的旅行报告",
    "suggestions": ["Day1: 西湖景区...", "Day2: 灵隐寺..."]
  },
  "message": "ok"
}
```

### 3.3 FileBasedChatMemory（对话记忆持久化）

**位置**: `src/main/java/com/yupi/aitravelplanner/chatmemory/FileBasedChatMemory.java`

**职责**: 实现 `ChatMemory` 接口，基于文件系统持久化对话历史（使用 Kryo 序列化）。

**核心特性**:
- 支持重启后恢复对话历史
- 每个会话独立存储为 `.kryo` 文件
- 自动创建存储目录

**方法说明**:

| 方法名 | 功能说明 | 参数 |
| :--- | :--- | :--- |
| `add` | 添加消息到会话 | `conversationId`: 会话ID<br>`messages`: 消息列表 |
| `get` | 获取会话消息 | `conversationId`: 会话ID |
| `clear` | 清除会话消息 | `conversationId`: 会话ID |

### 3.4 MyLoggerAdvisor（日志顾问）

**位置**: `src/main/java/com/yupi/aitravelplanner/advisor/MyLoggerAdvisor.java`

**职责**: 实现 `CallAdvisor` 和 `StreamAdvisor` 接口，在 AI 请求前后打印日志。

**功能**:
- 请求前：打印完整 Prompt
- 响应后：打印 AI 回复内容

### 3.5 异常处理体系

**ErrorCode（错误码枚举）**:

| 错误码 | 含义 |
| :--- | :--- |
| `SUCCESS(0)` | 成功 |
| `PARAMS_ERROR(40000)` | 请求参数错误 |
| `NOT_LOGIN_ERROR(40100)` | 未登录 |
| `NO_AUTH_ERROR(40101)` | 无权限 |
| `NOT_FOUND_ERROR(40400)` | 请求数据不存在 |
| `FORBIDDEN_ERROR(40300)` | 禁止访问 |
| `SYSTEM_ERROR(50000)` | 系统内部异常 |
| `OPERATION_ERROR(50001)` | 操作失败 |

**GlobalExceptionHandler**: 全局异常处理器，统一捕获 `BusinessException` 和 `RuntimeException`，返回标准化错误响应。

---

## 4. 依赖关系

### 4.1 核心依赖树

```
yu-ai-agent (0.0.1-SNAPSHOT)
├── spring-boot-starter-web          # Web 框架
├── dashscope-sdk-java               # 阿里云大模型 SDK
├── spring-ai-alibaba-starter-dashscope  # Spring AI 阿里云集成
├── spring-ai-starter-model-ollama   # Ollama 本地模型支持
├── spring-ai-markdown-document-reader  # Markdown 文档解析
├── langchain4j-community-dashscope   # LangChain4J 支持
├── jsonschema-generator             # JSON Schema 生成
├── kryo                             # 对象序列化
├── jsoup                            # HTML 解析
├── itext-core                       # PDF 生成
├── hutool-all                       # 工具库
├── knife4j-openapi3-jakarta         # API 文档
└── springdoc-openapi-starter-webmvc-ui  # Swagger UI
```

### 4.2 依赖说明

| 依赖 | GroupId | ArtifactId | 用途 |
| :--- | :--- | :--- | :--- |
| Spring AI Alibaba | `com.alibaba.cloud.ai` | `spring-ai-alibaba-starter-dashscope` | 接入阿里云大模型 |
| DashScope SDK | `com.alibaba` | `dashscope-sdk-java` | 阿里云 API 调用 |
| Ollama | `org.springframework.ai` | `spring-ai-starter-model-ollama` | 本地模型支持 |
| Kryo | `com.esotericsoftware` | `kryo` | 对象序列化（会话持久化） |
| Hutool | `cn.hutool` | `hutool-all` | 工具库 |

---

## 5. 配置与运行

### 5.1 application.yml 关键配置

**服务配置**:
```yaml
server:
  port: 8123                    # 服务端口
  servlet:
    context-path: /api          # 上下文路径
```

**AI 模型配置**:
```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}  # 阿里云 API Key（环境变量）
    ollama:
      base-url: http://localhost:11434  # Ollama 本地地址
      chat:
        options:
          model: gemma3:1b      # 默认模型
```

**API 文档配置**:
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

knife4j:
  enable: false                 # 是否启用 Knife4j
```

### 5.2 环境变量

| 环境变量 | 说明 |
| :--- | :--- |
| `DASHSCOPE_API_KEY` | 阿里云 DashScope API Key |

### 5.3 运行方式

**开发态运行**:
```bash
# 设置环境变量
export DASHSCOPE_API_KEY=your-api-key

# Maven 运行
mvn spring-boot:run
```

**打包构建**:
```bash
mvn clean package
```

**运行打包后的 Jar**:
```bash
java -jar target/yu-ai-agent-0.0.1-SNAPSHOT.jar
```

**验证服务**:
```bash
# 健康检查
curl http://localhost:8123/api/health
# 响应: ok
```

### 5.4 API 文档访问

- Swagger UI: `http://localhost:8123/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8123/api/v3/api-docs`

---

## 6. System Prompt 模板

**位置**: `src/main/resources/prompts/travel-system.st`

**核心逻辑**:
1. **角色定位**: 经验丰富的自由行旅行规划师
2. **个性化称呼**: 根据用户消息自动提取姓名
3. **场景引导**: 区分短途周边游、国内多日游、出境游三种场景
4. **信息收集**: 在关键信息（目的地、天数、预算、人数、偏好）未明确前，仅进行追问
5. **行程输出**: 信息足够后输出分天行程，包含交通、景点、餐饮、住宿、花费估算

---

## 7. 数据流与调用链

### 7.1 多轮对话调用链

```
POST /api/travel/chat
       ↓
AiController.chat()
       ↓
TravelApp.doChatWithId()
       ↓
ChatClient.prompt()
       ↓
MessageChatMemoryAdvisor (加载会话历史)
       ↓
MyLoggerAdvisor (日志记录)
       ↓
DashScope ChatModel (调用大模型)
       ↓
FileBasedChatMemory (保存对话历史)
       ↓
返回 ChatResponse
```

### 7.2 会话记忆机制

```
用户首次请求 → conversationId=null → 生成 UUID
       ↓
后续请求 → conversationId=xxx → 从文件加载历史消息
       ↓
每次对话 → 追加消息 → Kryo 序列化 → 保存到 tmp/chat-memory/{conversationId}.kryo
```

---

## 8. 目录结构总结

```
ai-travel-planner/
├── .mvn/                      # Maven 包装器
├── docs/                      # 学习笔记文档
├── src/
│   ├── main/
│   │   ├── java/com/yupi/aitravelplanner/
│   │   │   ├── advisor/       # ChatClient 顾问
│   │   │   ├── app/           # 核心业务
│   │   │   ├── chatmemory/    # 对话记忆
│   │   │   ├── common/        # 通用组件
│   │   │   ├── config/        # 配置类
│   │   │   ├── constant/      # 常量
│   │   │   ├── controller/    # REST API
│   │   │   ├── demo/          # 示例代码
│   │   │   ├── exception/     # 异常处理
│   │   │   ├── model/dto/     # 数据传输对象
│   │   │   ├── utils/         # 工具类
│   │   │   └── *.java         # 启动类和健康检查
│   │   └── resources/
│   │       ├── prompts/       # Prompt 模板
│   │       └── application.yml
│   └── test/                  # 测试代码
├── README.md
├── pom.xml
└── mvnw*                      # Maven 脚本
```

---

## 9. 扩展点

### 9.1 添加新的 AI 模型

项目已支持 DashScope 和 Ollama，可通过配置切换。如需添加其他模型，只需引入对应的 Spring AI Starter 依赖并配置。

### 9.2 自定义 Advisor

实现 `CallAdvisor` 或 `StreamAdvisor` 接口，在 `TravelApp` 中注册即可。

### 9.3 扩展对话记忆存储

实现 `ChatMemory` 接口，可替换为 Redis、数据库等存储方式。

---

## 10. 注意事项

1. **API Key 安全**: `DASHSCOPE_API_KEY` 通过环境变量注入，避免硬编码
2. **会话文件清理**: 对话记忆存储在 `tmp/chat-memory/` 目录，需定期清理
3. **Ollama 配置**: 使用 Ollama 时需确保本地已启动 Ollama 服务并拉取对应模型
4. **Prompt 模板**: 修改 `travel-system.st` 可调整 AI 的行为和回复风格