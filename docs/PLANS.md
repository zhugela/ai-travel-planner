# PLANS.md — 计划与执行边界

## 1. 计划层级

| 层级 | 文件 | 用途 |
|---|---|---|
| 产品范围 | `docs/project/REQUIREMENTS.md` | 用户故事和功能范围 |
| 长期任务 | `docs/project/TASKS.md` | 章节级任务与依赖 |
| 当前功能 | `feature_list.json` | 机器可读状态、验证与证据 |
| 活跃执行计划 | `docs/exec-plans/active/` | 多步骤任务的详细计划 |
| 已完成计划 | `docs/exec-plans/completed/` | 保留决策和验收证据 |
| 技术债 | `docs/exec-plans/tech-debt-tracker.md` | 已知缺口与触发条件 |

## 2. 状态规则

- 同一时间只允许一个功能为 `in_progress`。
- `not_started`：尚未开始。
- `in_progress`：当前唯一正在推进的功能。
- `blocked`：有具体、可记录的阻塞条件。
- `passing`：行为已验证，且 `evidence` 写明日期、验证方式和结果。
- 每个功能必须列出 `dependencies`；进入 `in_progress` 前，依赖必须是 `passing`。
- 依赖字段用于阻止范围漂移，不是完成后的装饰信息。

状态不是进度百分比。一个功能可以代码已写但测试未过，此时仍不能是 `passing`。

## 3. 何时创建执行计划

满足任一条件就创建：

- 预计跨多个文件或多个提交。
- 需要先诊断、再实现、再端到端验证。
- 涉及迁移、安全边界或接口契约变化。
- 任务可能跨会话继续。

单文件、低风险且一次提交能完成的小改动可以只记录在功能清单和进度日志。

## 4. 执行计划模板

每个活跃计划至少包含：

1. 目标与用户可见行为。
2. 不做什么（范围边界）。
3. 前置依赖与已知风险。
4. 测试先行步骤。
5. 实现步骤。
6. 验证命令与证据位置。
7. 文档同步清单。
8. 完成条件。

## 5. 生命周期

```text
not_started
  -> 创建 active 计划
  -> in_progress
  -> 执行与记录证据
  -> passing
  -> 计划移动到 completed
  -> 更新 progress / handoff / quality
```

如果受阻：

```text
in_progress -> blocked
  -> 记录原因、已尝试方案和解除条件
  -> 不必继续挤占 in_progress 名额
```

## 6. 当前计划

当前唯一活跃功能是 `F-001` 测试基线。执行计划见
[exec-plans/active/F-001-test-baseline.md](exec-plans/active/F-001-test-baseline.md)。
