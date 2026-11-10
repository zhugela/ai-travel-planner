# SCHEMA.md — 数据模型设计

> **当前状态**:**不建表**。会话记忆走文件(Kryo),RAG 走 Spring AI SimpleVectorStore(内存)。
> **本文档用途**:描述未来切换到数据库时的实体设计,**仅作设计参考**,目前不强制实现。
> **切换路径**:从 `FileBasedChatMemory` / `SimpleVectorStore` 平滑替换为 MyBatis-Plus / Spring Data JPA。

---

## 一、当前数据存储(无 DB)

| 数据 | 存储位置 | 序列化 | 生命周期 |
|---|---|---|---|
| 会话消息 | `chat-memory/{conversationId}.kryo` | Kryo | 永久 |
| RAG 文档 | Spring AI SimpleVectorStore(内存) | - | 应用重启丢失 |
| 配置 | application.yml / 环境变量 | - | - |
| Prompt 模板 | resources/prompts/*.st | UTF-8 | 永久 |

---

## 二、未来实体设计(参考)

### 实体 1: User(用户)
> **当前不需要**(无登录鉴权)。未来扩展时引入。

```sql
CREATE TABLE user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(64) NOT NULL UNIQUE,
    email           VARCHAR(128),
    password_hash   VARCHAR(255),  -- BCrypt
    api_key         VARCHAR(64),   -- 用于 API Key 模式对外开放
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,  -- 逻辑删除
    INDEX idx_username (username),
    INDEX idx_api_key (api_key)
);
```

**Java 实体**:
```java
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String apiKey;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
```

---

### 实体 2: Conversation(会话)

```sql
CREATE TABLE conversation (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL UNIQUE,  -- 业务 UUID,前端用
    user_id         BIGINT,                        -- 预留,当前不强制外键
    title           VARCHAR(255),                  -- 会话标题(可由首条消息生成)
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id)
);
```

**Java 实体**:
```java
@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;  // UUID,前端用它
    private Long userId;             // 预留
    private String title;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

---

### 实体 3: Message(消息)

```sql
CREATE TABLE message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL,
    role            VARCHAR(16) NOT NULL,  -- 'user' / 'assistant' / 'system' / 'tool'
    content         TEXT NOT NULL,
    metadata        JSON,                    -- 工具调用 / token 数 / model 等
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
);
```

**Java 实体**:
```java
@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private String role;      // user / assistant / system / tool
    private String content;
    private String metadata;  // JSON
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

---

### 实体 4: RAGDocument(RAG 文档)

```sql
CREATE TABLE rag_document (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id          VARCHAR(64) NOT NULL UNIQUE,
    filename        VARCHAR(255) NOT NULL,
    content         LONGTEXT NOT NULL,
    metadata        JSON,                  -- budget / region / tags
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_id (doc_id)
);
```

---

### 实体 5: RAGChunk(文档切片)

```sql
CREATE TABLE rag_chunk (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id          VARCHAR(64) NOT NULL,
    chunk_index     INT NOT NULL,
    content         TEXT NOT NULL,
    embedding       BLOB NOT NULL,          -- text-embedding-v3 输出的 1024 维向量
    metadata        JSON,
    INDEX idx_doc_id (doc_id)
);
```

**说明**:
- `embedding` 字段存的是向量(1024 维 float[])
- 大规模场景建议用 **PGVector / Milvus / Qdrant** 替代,自带向量索引
- 当前用 Spring AI SimpleVectorStore(内存)够用学习场景

---

## 三、实体关系图(未来)

```
┌──────────┐  1   N  ┌─────────────┐  1   N  ┌──────────┐
│   User   │────────│ Conversation │────────│ Message  │
└──────────┘        └─────────────┘        └──────────┘
                            │ 
                            │ 预留关联(可不强制)
                            ↓
                    ┌─────────────┐  1   N  ┌──────────┐
                    │ RAGDocument │────────│ RAGChunk │
                    └─────────────┘        └──────────┘
```

---

## 四、当前文件存储格式

### 会话记忆文件
**路径**:`{user.dir}/chat-memory/{conversationId}.kryo`

**内容**:`List<Message>` 序列化结果(Message 是 Spring AI 的 org.springframework.ai.chat.messages.Message)

**Kryo 序列化**(FileBasedChatMemory 实现)

---

### RAG 文档
**路径**:`resources/document/*.md`

**内容**:3 份 markdown 攻略

**加载时机**:应用启动时(TravelDocumentLoader)

**存储**:Spring AI SimpleVectorStore(应用进程内存,重启丢)

---

## 五、迁移路径(从文件 → 数据库)

### 阶段 1: 抽象层
- 创建 `ChatMemoryRepository` 接口
- `FileBasedChatMemory` 实现 → `JdbcChatMemory` 实现
- 用 Spring Profile 控制用哪个实现

### 阶段 2: 双写过渡
- 文件 + DB 同时写
- 验证数据一致
- 切换读取到 DB

### 阶段 3: 切量
- 读 DB,写 DB
- 文件保留备份 N 天后清

### 阶段 4: RAG 向量库升级
- 替换 SimpleVectorStore 为 PGVector / Milvus
- 文档切片 schema 对齐 RAGChunk

---

## 六、API Key / 密钥存储

**当前**:环境变量 + application.yml 占位符 `${DASHSCOPE_API_KEY}`

**未来**(生产):
- 引入 Vault / Nacos / KMS
- 不进代码 / 不进 yml / 不进 git
- 启动时从密钥服务拉

---

## 七、为什么不现在建表

| # | 原因 |
|---|---|
| 1 | 学习项目,数据库会让部署变重(Docker 多一个容器) |
| 2 | 会话量小,文件够用 |
| 3 | RAG 文档固定 3 份,内存向量库够 |
| 4 | 没有多用户并发需求 |
| 5 | MyBatis-Plus / Spring Data JPA 是独立学习专题,容易跑偏 |

**什么时候建表**:
- 多用户功能(US-6 鉴权)
- 持久化 RAG(超过 1000 文档)
- 数据分析(用户行为 / Token 用量统计)
- 部署到生产环境

---

## 八、变更记录

| 日期 | 变更 |
|---|---|
| 2026-07-18 | 初版:5 个实体设计 + 迁移路径(仅参考) |

---

*最后更新:2026-07-18*