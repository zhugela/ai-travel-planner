# ADR 模板

```markdown
# ADR-NNN:决策标题

- 状态:提议 / 已接受 / 已废弃 / 已替代
- 日期:YYYY-MM-DD
- 决策人:角色或团队

## 背景

描述促成决策的约束、故障或业务目标。

## 决策

用一段话明确写“我们决定……”。

## 备选方案

| 方案 | 优点 | 缺点 | 未选择原因 |
|---|---|---|---|

## 影响

### 正向
- ...

### 代价与风险
- ...

## 落地与验收

- [ ] 实现项
- [ ] 测试项
- [ ] 文档项
- [ ] 回退条件与步骤

## Related

- [[category/related-page]]
```

什么时候需要 ADR:
- 更换模型、存储、框架或部署方式。
- 安全策略、数据边界或团队约束发生变化。
- 两个以上可行方案之间存在长期取舍。

普通 bug 修复不需要 ADR,写知识卡片或测试用例即可。

## Related

- [[engineering/knowledge-capture-workflow]]
- [[templates/knowledge-card-template]]
