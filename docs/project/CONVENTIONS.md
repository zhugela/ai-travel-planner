# 项目约定

> 从仓库根目录的 `CLAUDE.md` / `AGENTS.md` 提炼而来。
> **这两份文件内容 99% 重复**(仅工具名不同),这里合并为单一事实来源。
> 根目录那两份保留各工具读取入口,内容变更以本文件为准。
>
> 最后更新:2026-11-10

---

## 一、项目身份

| 项 | 内容 |
|---|---|
| 项目名 | `ai-travel-planner` |
| 课程原型 | AI 恋爱大师 → 适配为 AI 旅行规划助手 |
| 学习目标 | Spring AI 全栈 + AI Agent 实战,简历可写 |
| 当前阶段 | Ch 02~09 完成,进入工程化加固 |

---

## 二、技术栈(2026-11-10 更新)

> ⚠️ 根目录 CLAUDE.md/AGENTS.md 里写的「DashScope qwen-plus」已过时,以本表为准。

| 层 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 语言 | Java | 21 (LTS) | 本机多版本:21/22/26,优先 26 |
| 框架 | Spring Boot | 3.4.4 | starter-web,非 WebFlux |
| Spring AI | spring-ai-bom | 1.0.0 | |
| **Chat** | **DeepSeek** | `deepseek-chat` | OpenAI 兼容协议接入 |
| **Embedding** | **本地 ONNX bge-small-zh-v1.5** | 512 维 | DeepSeek 无 embedding 接口 |
| 向量库 | SimpleVectorStore | 内置 | 内存,重启重建 |
| 会话记忆 | 文件 + Kryo | 5.6.2 | 无数据库 |
| 搜索 | 博查 / SerpAPI | 双 provider | 默认 bocha |
| 工具 | Hutool / jsoup / iText | 5.8.37 / 1.19.1 / 9.1.0 | |
| 前端 | Vue 3 + Vite + Element Plus | 3.x / 5.x | 独立目录 |

### 严禁
- 引入数据库(MySQL / PG / Redis)
- 前端引入 TypeScript
- 替换 Spring AI 框架
- 提交任何真实 API Key

---

## 三、协作流程(5 阶段)

### 阶段 1:读约定
每次会话开头必读:
1. 根目录 `CLAUDE.md` 或 `AGENTS.md`(工具入口)
2. 本文件
3. `docs/project/REQUIREMENTS.md` + `docs/project/TASKS.md`

### 阶段 2:只读调研
**禁止修改**任何代码/文档,只能读文件、grep、跑只读命令。

必须输出变更清单:
```
[MODIFY] src/main/java/com/yupi/aitravelplanner/X.java
  + 新增方法 Y
  - 删除方法 Z
[NEW]    src/test/java/.../XTest.java
[DELETE] src/main/java/.../Obsolete.java
```

### 阶段 3:TDD 编码
- 测试先行:先写测试 → 红 → 实现 → 绿
- 小粒度 commit,每过一个功能点一次
- commit 格式:`<type>(<scope>): <subject>`

### 阶段 4:同步文档
| 改了什么 | 更新哪里 |
|---|---|
| 功能 | `docs/project/REQUIREMENTS.md` |
| 实体字段 | `docs/project/SCHEMA.md` |
| 完成任务 | `docs/project/TASKS.md` |
| 测试用例 | `docs/project/TEST_CASES.md` |
| 接口 | `docs/guides/API_CONTRACT.md` |
| prompt | `docs/guides/PROMPT_GUIDELINES.md` |

### 阶段 5:收尾三件套
1. ✅ commit hash(可 revert)
2. 📊 测试结果(passed / failed / skipped)
3. ⚠️ 已知问题(留 TODO,不阻塞)

---

## 四、学习模式约束

来自根目录 `study-rules.md`,本项目的 AI 必须遵守:

| # | 规则 | 说明 |
|---|---|---|
| 1 | 不直接甩完整代码 | 用户未自主思考前不写实现 |
| 2 | 三段式 | 讲解思路 → 留 TODO → 用户填空 → 校对 |
| 3 | 费曼检验 | 解释要让零基础新生听懂 |
| 4 | 通俗类比 | 讲计网/OS/算法/框架必须配类比 |
| 5 | 拒绝闲聊 | 不延伸无关话题 |
| 6 | 番茄节奏 | 不主动拉长学习时间 |
| 7 | 分点结构化 | 长内容分段,不堆大段文字 |
| 8 | 先抛问题 | 写代码前让用户梳理思路 |
| 9 | 不看答案 | 排查 bug 引导用户自主定位,不直接给修复代码 |

**例外**:用户明确说"直接给完整代码"时,可跳过引导(如本文件这类工程化任务)。

---

## 五、代码风格

### 命名
- 类 `UpperCamelCase` / 方法字段 `lowerCamelCase`
- 常量 `UPPER_SNAKE_CASE` / 包名全小写

### 注释
- **方法注释只解释"为什么"**,不写"做什么"
- 复杂逻辑标风险:`// XXX: 此处有 XXX 风险`
- **禁止废话注释**(如 `// 设置 name 字段`)

### 异常
- 业务异常 → `BusinessException` + `ErrorCode`
- 系统异常 → `GlobalExceptionHandler` 统一捕获
- 工具类返回 `Result<T>` 或抛业务异常,**不返回 null**

### 日志
- `@Slf4j`
- 关键路径 INFO,异常 ERROR,调试 DEBUG
- **生产禁用 DEBUG**

---

## 六、SSE 专项约束

| # | 规则 |
|---|---|
| 1 | 必须 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` |
| 2 | 必须 `SseEmitter` 或 `Flux<String>` |
| 3 | 必须注册 `onTimeout()` / `onCompletion()` / `onError()` |
| 4 | Agent 必须 emit `step` / `loop` / `error` / **`done`** |
| 5 | 客户端断开必须释放后端 `SseEmitter` |

**补充(2026-11-10)**:`done` 帧是必须的。
只 `complete()` 不推 `done`,浏览器 EventSource 会按规范自动重连,导致重复执行。
详见 `docs/guides/API_CONTRACT.md`。

---

## 七、环境与运行

### JDK
- 首选:`C:\Users\Administrator\.jdks\openjdk-26.0.1`
- 备选:`jdk-22` / `temurin-24`
- 避免:`oracle_open_jdk-21`(maven 锁住)

### 启动
| 方式 | 评价 |
|---|---|
| IDEA Run | ✅ 首选 |
| WMI + bat 脚本 | ✅ 无 IDEA 时可用(见下) |
| 普通 shell `java -jar` | ❌ Windows loopback 报错 |
| `mvn spring-boot:run` | ❌ JDK 21 锁死 |

#### 无 IDEA 时的启动方式(2026-11-10 验证)

普通 shell 启动会报 `Unable to establish loopback connection`。
用 WMI 创建进程可绕过,且**环境变量必须在 bat 内部设置**(WMI 不继承 powershell 环境):

```bat
@echo off
set "DEEPSEEK_API_KEY=你的key"
set "SEARCH_API_KEY=你的key"
set "SEARCH_API_PROVIDER=bocha"
"C:\Users\Administrator\.jdks\openjdk-26.0.1\bin\java.exe" ^
  -jar "D:\code\ai-travel-planner\target\ai-travel-planner-0.0.1-SNAPSHOT.jar" ^
  --spring.main.lazy-initialization=true
```

```powershell
Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
  CommandLine = 'cmd.exe /c "D:\code\ai-travel-planner\tmp\launch.bat"'
  CurrentDirectory = 'D:\code\ai-travel-planner'
}
```

### 端口
- 后端 8123 / 前端 3000 / MCP(可选)8127

### 环境变量
| 变量 | 必需 | 用途 |
|---|---|---|
| `DEEPSEEK_API_KEY` | ✅ | Chat 模型 |
| `SEARCH_API_KEY` | 可选 | 联网搜索 |
| `SEARCH_API_PROVIDER` | 可选 | `bocha`(默认) / `serpapi` |
| `DASHSCOPE_API_KEY` | 可选 | 切回百炼时用 |

**全部禁入代码**,放 `.env.local.ps1` 等已 gitignore 的文件。

---

## 八、Git 约束

- 主分支 `main`,不开 feature 分支(单人项目)
- Commit 格式:`<type>(<scope>): <subject>`
  - type:`feat` / `fix` / `docs` / `test` / `refactor` / `chore`
  - scope:`agent` / `tools` / `rag` / `config` / `docs` / `frontend`

### 禁止
- ❌ `git push --force`
- ❌ 一次性大 commit
- ❌ 提交 `.env` / 真实 Key
- ❌ `--no-verify`
- ❌ `git add -A`(本机 `D:\code2` 是大仓库,含多个无关项目)

---

## 九、当前已知限制

| # | 问题 | 状态 |
|---|---|---|
| 1 | Windows loopback 导致 `@SpringBootTest` 跑不了 | 未解,纯单元测试可跑 |
| 2 | RAG 知识库仅 4KB,覆盖不足 | 待扩充 |
| 3 | `FileOperationTool` 无路径校验 | 待加 |
| 4 | 源码历史中有硬编码 key | 需去百炼后台作废 |
| 5 | MCP 远程工具未启用 | 设计决策,暂不需要 |

---

## 十、用户协作偏好

| # | 偏好 |
|---|---|
| 1 | 中文为主(注释/文档/commit) |
| 2 | 简洁优先,信息密度高 |
| 3 | 关键决策先讲思路再动手 |
| 4 | 失败诚实承认,不糊弄 |
| 5 | 多方案并列,标注推荐项 |
