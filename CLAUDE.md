# CLAUDE.md — AI 旅行规划助手项目约束

> 这是 Claude Code 在本仓库工作时的全局行为规范。所有会话开始前先读这份文件。

---

## 一、项目身份

- **项目名**: `ai-travel-planner`(仓库根 `D:\code\ai-travel-planner`)
- **课程原型**: AI 恋爱大师 → 本项目适配为 **AI 旅行规划助手**
- **学习目标**: Spring AI 全栈 + AI Agent 实战项目,简历可写
- **当前阶段**: Ch 02~Ch 09 全部笔记已落,核心功能实现中

---

## 二、技术栈约束

| 层 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 语言 | Java | 21 (LTS) | 本机多版本并存:21/22/26,优先用 26 |
| 后端框架 | Spring Boot | 3.4.4 | spring-boot-starter-web(非 WebFlux) |
| AI | Spring AI + Alibaba DashScope | 1.0.0 / 1.0.0.2 | qwen-plus + text-embedding-v3 |
| 向量库 | Spring AI SimpleVectorStore | 内置 | 内存,无 PG 依赖 |
| 持久化 | 文件 (Kryo 序列化) | 5.6.2 | 会话记忆,非数据库 |
| 工具 | Hutool + jsoup + iText | 5.8.37 / 1.19.1 / 9.1.0 | |
| 前端 | Vue 3 + Vite + Element Plus | 3.x / 5.x | 独立仓库 `ai-travel-planner-frontend/` |
| 测试 | JUnit 5 + Spring Boot Test | 5.x | |

**严禁**:
- 引入数据库(MySQL/PG/Redis)
- 引入 TypeScript 到前端
- 替换 Spring AI 框架
- 引入 Lombok 到非 Java 21 环境

---

## 三、目录与文件约束

```
ai-travel-planner/
├── CLAUDE.md                   ← 本文件,先读
├── docs/                       ← 设计文档(REQUIREMENTS/SCHEMA/TASKS/TEST_CASES)
│   ├── 学习笔记-NN-XXX.md      ← Ch 02~09 学习笔记(权威需求源)
│   └── prompts/                ← Prompt 模板(可独立维护)
├── src/main/java/com/yupi/aitravelplanner/
│   ├── agent/                  ← Agent 4 层架构(必动)
│   ├── tools/                  ← @Tool 注解工具
│   ├── app/TravelApp.java      ← 业务组装类
│   ├── controller/             ← REST + SSE 端点
│   ├── service/                ← 业务服务(RAG 等)
│   ├── rag/                    ← RAG 组件(loader/enhancer)
│   ├── config/                 ← Spring 配置
│   ├── chatmemory/             ← 文件会话记忆
│   └── common/exception/...    ← 通用基类
├── src/main/resources/
│   ├── application.yml         ← 主配置
│   ├── application-prod.yml    ← 生产配置
│   ├── document/               ← RAG 知识库 md
│   └── prompts/                ← Spring AI 用的 .st 模板
├── src/test/                   ← 测试代码(用 JUnit 5 + MockMvc)
├── pom.xml                     ← Maven 依赖
├── Dockerfile                  ← 后端容器化
└── .dockerignore
```

---

## 四、协作流程约束(5 阶段)

### 阶段 1: 读 CLAUDE.md + docs/
Claude Code **每次会话开头**必须读:
1. 本文件 (`CLAUDE.md`)
2. `docs/REQUIREMENTS.md`(需求范围)
3. `docs/TASKS.md`(任务拆解,选当前 task)

### 阶段 2: 调研模式(只读)
调研现状时**禁止修改**任何代码 / 文档,只能:
- 读文件
- grep / glob
- 跑只读命令(`git status`、`ls`、`wc`)

输出**变更清单**(Change Manifest):
```
[MODIFY] src/main/java/com/yupi/aitravelplanner/X.java
  + 新增方法 Y
  - 删除方法 Z
[NEW]    src/test/java/.../XTest.java
[DELETE] src/main/java/.../Obsolete.java
```

### 阶段 3: TDD 编码(分阶段 commit)
- **测试先行**:先写 JUnit 5 + Spring Boot Test,跑测试 → 红
- **写实现**:让测试 → 绿
- **小粒度 commit**:每过一个测试 → 一次 commit
- **commit message 格式**:`<type>(<scope>): <subject>`(沿用现有 git log 风格)
  - `feat(agent): 新增 runStream SSE 推送`
  - `fix(rag): 修复检索空指针`
  - `test(tools): 补 WeatherTool 异常路径`

### 阶段 4: 同步更新文档
代码改完**必须**回头更新:
- 改功能 → `REQUIREMENTS.md` 状态
- 改实体字段 → `SCHEMA.md` 字段
- 完成 task → `TASKS.md` 勾选 [x]
- 加测试 → `TEST_CASES.md` 用例

### 阶段 5: 主动收尾
每个 task 结束必须给出:
1. ✅ commit hash(让用户能 revert)
2. 📊 测试结果(passed / failed / skipped)
3. ⚠️ 已知问题(留 TODO,不阻塞)

---

## 五、学习模式约束(严守 study-rules.md)

来自 `study-rules.md`,Claude 必须:

| # | 规则 | 违反后果 |
|---|---|---|
| 1 | **不直接甩完整代码** | 用户未自主思考前不写代码 |
| 2 | **先思路 → 用户写 → 校对** | 三段式:讲解 → 留 TODO → 用户填空 |
| 3 | **费曼检验** | 解释必须让零基础新生听懂 |
| 4 | **不堆砌术语** | 复杂概念配通俗类比 |
| 5 | **拒绝闲聊** | 不主动延伸无关话题 |
| 6 | **番茄节奏** | 不主动延长学习时间 |
| 7 | **分点结构化** | 长内容分段,不堆大段无分割文字 |
| 8 | **写代码前先抛问题** | 让用户先梳理思路 |

**例外**:用户明确说"直接给完整代码"(如本任务确认 5 阶段流程) → 可跳过引导。

---

## 六、代码风格约束

### Java 命名
- 类名 `UpperCamelCase`(Spring 注解类例外)
- 方法/字段 `lowerCamelCase`
- 常量 `UPPER_SNAKE_CASE`
- 包名全小写

### 注释
- 类注释:说明用途 + 作者 + 创建时间(可选)
- 方法注释:只解释**为什么**(不是做什么)
- 复杂逻辑:加 `// XXX: 此处 XXX 风险` 注释
- **禁绝不写废话注释**(`// 设置 name 字段` 这种)

### 异常处理
- 业务异常:`BusinessException` + `ErrorCode`
- 系统异常:全局 `GlobalExceptionHandler` 统一捕获
- 工具类:返回 `Result<T>` 或抛业务异常,不返回 null

### 日志
- 用 `@Slf4j`(Lombok)
- 关键路径必须 INFO,异常 ERROR
- 调试用 DEBUG,**生产禁用** DEBUG

---

## 七、SSE 专项约束(Ch 09)

所有流式接口必须遵守:

| # | 规则 |
|---|---|
| 1 | 必须 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` |
| 2 | 必须用 `SseEmitter` 或 `Flux<String>` |
| 3 | 必须注册 `onTimeout()` / `onCompletion()` / `onError()` |
| 4 | Agent 接口每步必须 emit `step` / `loop` / `error` 三种 event name |
| 5 | 客户端连接断开时**必须**释放后端 `SseEmitter`(P0-2 必修) |

---

## 八、环境与运行约束

### JDK 切换
- **首选**:`C:\Users\Administrator\.jdks\openjdk-26.0.1`(JDK 26,JDK 21 的 HttpClient Windows bug 已修复)
- 备选:`C:\Program Files\Java\jdk-22` / `C:\Users\Administrator\.jdks\temurin-24`
- 避免用:`C:\Users\Administrator\.jdks\oracle_open_jdk-21`(maven 锁住,改不了)

### 启动方式
- **首选**:IDEA Run(自动 JDK 26 + 全 classpath)
- **备选**:完整 java -cp 命令(超长,需 IDEA 控制台复制)
- **慎用**:`java -jar`(会触发 UnixDomainSocket bug)
- **禁用**:`mvn spring-boot:run`(JDK 21 锁死)

### 端口
- 后端:`8123`
- 前端 dev:`3000`
- MCP server(可选):`8127`

### API Key
- `DASHSCOPE_API_KEY`:环境变量,**禁入代码**
- `SEARCH_API_KEY`:可选,联网搜索工具用

---

## 九、Git 约束

### 分支策略
- 主分支:`main`(当前所有改动直接进 main,沿用你之前习惯)
- 不开 feature 分支(单人学习项目,简化)

### Commit 风格(沿用现有)
```
<type>(<scope>): <subject>

[可选 body,说明 why]
[可选 footer,关联需求]
```

**type**:`feat` / `fix` / `docs` / `test` / `refactor` / `chore`
**scope**:`agent` / `tools` / `rag` / `config` / `docs` / `frontend` 等

### 不要做
- ❌ `git push --force`
- ❌ 一次性大 commit(每个功能点独立)
- ❌ 提交 `.env` / `application-prod.yml` 里的真 Key
- ❌ `--no-verify` 跳过 hook

---

## 十、当前已知问题与限制

| # | 问题 | 状态 | 处理方式 |
|---|---|---|---|
| 1 | 后端启动可能触发 JDK HttpClient Windows bug | 部分缓解(JDK 26 修复) | IDEA Run 优先 |
| 2 | Agent SSE 端点 `/api/agent/sse` 可能因 classpath 缓存不生效 | 待观察 | IDEA Rebuild Project |
| 3 | MCP 远程工具未启用(application.yml 已 disable) | 已禁 | 暂不动,演示项目不需要 |
| 4 | 无真实数据库,会话用文件存储 | 设计决策 | 学习项目可接受 |
| 5 | 前端未完全联调 | Ch 09 后处理 | 端到端 SSE 是测试重点 |

---

## 十一、文档同步约束(每次 PR 必检)

代码改动后**必须**检查:
- [ ] `docs/REQUIREMENTS.md` 功能清单状态
- [ ] `docs/SCHEMA.md` 实体字段(如有新增)
- [ ] `docs/TASKS.md` 任务勾选 [x]
- [ ] `docs/TEST_CASES.md` 新增/更新用例
- [ ] `docs/学习笔记-NN-XXX.md`(如有重要设计决策)

---

## 十二、用户协作风格(基于过往对话沉淀)

| # | 偏好 | 说明 |
|---|---|---|
| 1 | 中文为主 | 注释 + 文档 + commit 都用中文 |
| 2 | 简洁优先 | 不堆废话,信息密度高 |
| 3 | 关键决策先讲 | 写代码前先讲思路 |
| 4 | 失败诚实承认 | 不糊弄,错了直说 |
| 5 | 选项让用户选 | 多个方案并列,标注推荐项 |

---

*最后更新:2026-07-18*
*对应 5 阶段流程:CLAUDE.md → docs 四层 → 新会话只读调研 → TDD 编码 → 同步文档*