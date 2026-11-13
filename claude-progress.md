# AI Travel Planner — 会话进度

> 这是跨会话状态入口。只记录已经执行或明确为未验证的事实。
> 最后更新：2026-11-13

## 当前已验证状态

| 项 | 状态 |
|---|---|
| 仓库根目录 | `D:\zhugela-hub\ai-travel-planner` |
| Git 工作树 | 独立仓库，分支 `main`，`origin` 指向 `zhugela/ai-travel-planner` |
| 后端源码 | 存在 Spring Boot 3.4.4 / Java 21 目标版本 |
| 测试代码 | `src/test/java` 下已有 101 个 `@Test` 标记 |
| 当前测试基线 | **未通过**：2026-11-12 执行 `.\mvnw.cmd test`，100 tests, 0 failures, 45 errors, 0 skipped |
| 文档 Harness | 已按 repo-template + harness-creator 结构生成 |
| 前端端到端 | 未在当前会话验证；前端不在本仓库 |
| Docker 运行 | 未验证；只有 Dockerfile 和启动文档 |

## 标准路径

- 标准启动：IDEA Run `AiTravelPlannerApplication`，JDK 26，端口 `8123`。
- 无 IDEA 启动：按 `docs/project/CONVENTIONS.md` 的 WMI + bat 方式；密钥只放在被忽略的本地文件中。
- 标准编译：`.\mvnw.cmd -q -DskipTests compile`
- 标准验证：`.\mvnw.cmd test`
- 标准推送：`git push origin main`
- Harness 校验：`powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-harness.ps1`
- 快速初始化：`powershell -NoProfile -ExecutionPolicy Bypass -File .\init.ps1`

## 当前最高优先级

`F-001`：恢复可重复、可信的测试基线。最新完整测试已确认 45 个上下文启动错误；
先修测试隔离和占位配置，再重跑同一命令。

## 当前 blocker

1. 测试套件存在 45 个 errors，集中在 `@SpringBootTest` 上下文启动阶段。
   首个根因是测试环境没有 `spring.ai.openai.api-key` 占位值，导致
   `OpenAiChatModel` Bean 创建失败；其他错误是该上下文失败的连锁结果。
2. `FileOperationTool` 尚未使用统一安全路径解析器。
3. `TerminalTool` 仍将整段字符串交给 shell，首命令白名单不是完整安全边界。
4. 前端端到端和 Docker 启动仍缺少当前会话证据。

## 会话记录

### 2026-11-12 — 生成项目 Harness

**本轮目标**

- 把 repo-template 的长期文档结构和 harness-creator 的会话状态结构适配到本项目。
- 保持根入口短、深规则留在 `docs/`。
- 不把已有但未验证的能力写成 `passing`。

**已完成**

- 新增架构、设计、前端、计划、产品、质量、可靠性、安全文档入口。
- 新增功能清单、进度、交接、收尾清单、评审表和代码库质量快照。
- 新增 Windows 初始化脚本、POSIX 包装脚本和机械校验脚本。
- 新增四个本项目 SOP 以及外部参考资料说明。
- 将根 `AGENTS.md` / `CLAUDE.md` 收敛为短路由入口。

**运行过的验证**

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-harness.ps1`
  → passed，0 warnings。
- `.\mvnw.cmd test` → 100 tests run, 0 failures, 45 errors, 0 skipped。
- Java 22.0.2 / Maven 3.9.11；测试上下文首个错误为
  `OpenAI API key must be set`。

**已记录证据**

- 文件结构与内容位于仓库根目录和 `docs/`。
- `target/surefire-reports` 保留了 2026-11-12 的逐类测试统计。
- `feature_list.json` 的 F-001 已记录上述测试统计和首个根因。

**提交记录**

- 本次 Harness 生成作为单个 `docs(harness)` 提交；最终 commit hash 见交付汇报。

**已知风险或未解决问题**

- 上层 Git 仓库包含其他项目的未提交改动，禁止 `git add -A`。
- 本地运行态目录可能含真实 API Key；不得读取进文档、日志或提交。
- `requirements/tasks/test-cases` 有部分状态落后于源码，需要后续单独清理。

**下一步最佳动作**

1. 在 `src/test/resources` 为 OpenAI 兼容 ChatModel 提供无副作用的测试占位配置，
   并确认是否还需要注册 Mock `ChatModel`。
2. 重跑 `.\mvnw.cmd test`，区分剩余真实缺陷与外部依赖问题。
3. 基线转绿后，再推进 `F-006` 安全路径与终端工具治理。

### 2026-11-12 — harness-creator 五子系统审计

**本轮目标**

- 按 `harness-creator` 的指令、状态、验证、范围、生命周期模型复核现有 Harness。
- 只补机械缺口，不扩展到业务代码修复。

**五子系统评分**

| 子系统 | 分数 | 判断 |
|---|---:|---|
| 指令 | 2 | 根入口短，启动路径、硬约束和完成定义明确 |
| 状态 | 2 | 功能清单、进度和证据边界可恢复 |
| 验证 | 1 | 命令可执行，但 Maven 基线仍为红 |
| 范围 | 2 | 单一 `in_progress`，功能依赖已机械化校验 |
| 生命周期 | 2 | 进度、交接和收尾清单齐全 |

最低分是“验证”。候选瓶颈不是缺少命令，而是测试上下文隔离失败。该判断已由
`.\mvnw.cmd test` 的 45 个 errors 证实，因此优先修 F-001，不新增业务功能。

**已完成**

- 为 `feature_list.json` 增加 `dependencies`。
- 更新 JSON Schema，使依赖字段成为必填。
- 扩展 `scripts/check-harness.ps1`：检查未知依赖、自依赖，以及 `in_progress`
  功能是否存在未完成依赖。
- 更新 `docs/PLANS.md` 的范围规则。

**运行过的验证**

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-harness.ps1`
  → passed，0 warnings。
- `git diff --check` → 无空白错误。

**提交记录**

- 本次审计作为独立 `docs(harness)` 提交；最终 commit hash 见交付汇报。

### 2026-11-13 — 迁移到独立仓库

**本轮目标**

- 修正此前从父级 `code2` 单体仓库推送的问题。
- 让当前目录成为独立 Git 工作区，并固定推送目标。

**已完成**

- 在当前目录初始化独立仓库，分支为 `main`。
- 拉取 `https://github.com/zhugela/ai-travel-planner.git` 的既有历史。
- 将 `origin` 固定为该仓库。
- 把当前项目内容作为目标仓库 `main` 的新提交，而不是推送父级目录结构。

**标准推送**

```powershell
git push origin main
```

禁止从 `D:\zhugela-hub` 根目录推送本项目。
