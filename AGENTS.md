# AGENTS.md — 短入口

> 本文件只负责路由和硬约束。项目事实以仓库文档为准，不依赖聊天上下文。
> 技术栈冲突时，以 [docs/project/CONVENTIONS.md](docs/project/CONVENTIONS.md) 为准。

## 开工必读

按顺序读取：

1. 本文件。
2. [claude-progress.md](claude-progress.md)：当前已验证状态、阻塞和下一步。
3. [feature_list.json](feature_list.json)：选择一个功能，且只允许一个 `in_progress`。
4. [ARCHITECTURE.md](ARCHITECTURE.md) 和相关实现文档。
5. [docs/project/CONVENTIONS.md](docs/project/CONVENTIONS.md)、
   [docs/project/REQUIREMENTS.md](docs/project/REQUIREMENTS.md)、
   [docs/project/TASKS.md](docs/project/TASKS.md)。

学习型任务还要遵循 [study-rules.md](study-rules.md) 和
[docs/knowledge/engineering/guided-learning-rules.md](docs/knowledge/engineering/guided-learning-rules.md)。
用户明确要求“直接生成/直接修复”时，可直接执行。

## 硬约束

- 只推进当前选中的一个功能；额外发现写成 TODO 或技术债，不夹带实现。
- 调研阶段只读。修改前先输出 `[MODIFY] / [NEW] / [DELETE]` 变更清单。
- 本目录是独立 Git 工作区，`origin` 必须是
  `https://github.com/zhugela/ai-travel-planner.git`。不要从父级
  `D:\zhugela-hub` 执行本项目的提交或推送。
- 不引入 MySQL、PostgreSQL、Redis；当前会话记忆为文件持久化。
- 不替换 Spring AI，不给前端引入 TypeScript。
- Java 代码遵守 `docs/project/CONVENTIONS.md`；SSE 必须显式发送
  `step` / `loop` / `error` / `done`，并注册超时、完成、错误回调。
- API Key 只从环境变量读取；不得写入代码、文档、命令输出或提交记录。
- 修改接口先更新 [docs/guides/API_CONTRACT.md](docs/guides/API_CONTRACT.md)。
- 修改高风险工具或文件路径逻辑前，先读
  [docs/SECURITY.md](docs/SECURITY.md) 和对应知识卡片。

## 正确性规则

- 测试先行：先写或更新 JUnit 5 测试，确认失败原因与目标功能一致，再实现。
- 修复后先跑最小相关测试，再跑标准验证；不能用“代码看起来正确”代替执行结果。
- 外部模型、网络和浏览器相关能力必须区分 mock 验证与真实端到端验证。
- 失败就记录失败。不得把未运行的检查、旧日期的报告或人工推测写成当前证据。

## 完成定义

一个功能只有同时满足以下条件，才能标为 `passing`：

1. 用户可见行为符合该功能的 `user_visible_behavior`。
2. 功能清单中的验证步骤已实际执行，并记录命令、日期和结果。
3. 失败、跳过和未覆盖边界被明确写出，没有“假 passing”。
4. 相关需求、接口、测试用例、架构或技术债文档已同步。
5. 工作区没有与本功能相关的未记录半成品。
6. 下一轮只靠仓库文件即可继续，不需要口头背景。

## 收尾流程

1. 运行 `scripts/check-harness.ps1` 和适用的测试命令。
2. 更新 `feature_list.json`、`claude-progress.md`、`session-handoff.md`。
3. 按 [clean-state-checklist.md](clean-state-checklist.md) 检查。
4. 只暂存本任务涉及的文件，使用
   `<type>(<scope>): <subject>` 格式提交。
5. 汇报 commit hash、passed/failed/skipped、已知问题和下一步。

## 标准命令

```powershell
# 结构校验
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-harness.ps1

# 编译
.\mvnw.cmd -q -DskipTests compile

# 全量测试
.\mvnw.cmd test

# 单测示例
.\mvnw.cmd test -Dtest=LoopGuardTest

# Harness 初始化（安装、校验、测试）
powershell -NoProfile -ExecutionPolicy Bypass -File .\init.ps1
```

启动优先使用 IDEA Run（JDK 26，端口 `8123`）。完整环境说明、Windows 启动绕行方式和
API Key 配置见 [docs/project/CONVENTIONS.md](docs/project/CONVENTIONS.md)。
