# 会话交接

> 新会话先读 `AGENTS.md`、本文件、`claude-progress.md` 和 `feature_list.json`。
> 更新时间：2026-11-13

## 当前已验证

- 仓库已有 Spring AI、RAG、Agent、工具、SSE 后端源码。
- 2026-11-12 已执行全量测试：100 tests, 0 failures, 45 errors, 0 skipped。
- 45 个 errors 集中在 Spring 测试上下文启动；首个根因是 OpenAI 兼容 ChatModel
  缺少测试占位 API key。
- 新的 Harness 文档和机械校验脚本已写入仓库。
- 当前目录已迁移为独立 Git 仓库，`origin` 指向 `zhugela/ai-travel-planner`。

## 本轮改动

- 根入口收敛为短 `AGENTS.md` / `CLAUDE.md`。
- 新增 repo-template 对应文档结构。
- 新增 harness 状态、交接、评分和质量快照。
- 新增 `init.ps1`、`init.sh`、`scripts/check-harness.ps1`。
- 按 `harness-creator` 审计后，为功能清单补充机器可读依赖并增加依赖校验。
- 修正 Git 推送目标，停止使用父级 `code2` 仓库。

## 仍损坏或未验证

- Maven 全量测试已确认不是绿色，需先补测试环境隔离。
- 文件工具路径边界、终端工具 shell 绕过面仍是安全问题。
- 前端联调、Docker 冒烟、MCP 图片搜索没有当前证据。

## 下一步最佳动作

1. 只处理 `F-001`：在测试资源中提供安全的 ChatModel 占位/Mock 配置。
2. 重跑全量测试，记录剩余失败类与根因。
3. 不要同时改安全工具、前端和部署。

## 命令

```powershell
.\mvnw.cmd -q -DskipTests compile
.\mvnw.cmd test
.\mvnw.cmd test -Dtest=LoopGuardTest
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-harness.ps1
```

## 不要动

- 不要把真实 Key 写入任何受版本控制的文件。
- 本目录已是独立仓库，但仍不要提交密钥、运行日志或未确认产物。
- 不要从 `D:\zhugela-hub` 父级仓库推送本项目。
- 不要在基线红灯时把新功能标为 `passing`。
