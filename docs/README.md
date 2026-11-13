# docs/ 索引

> 先读哪份,看你要做什么。

```
docs/
├── README.md          ← 本索引
├── design-docs/       ← 设计信念与设计文档索引
├── exec-plans/        ← 活跃/已完成计划与技术债
├── generated/         ← 从源码/设计生成的快照
├── product-specs/     ← 用户行为和首次使用规格
├── references/        ← 外部 LLM 参考材料
├── sops/              ← 可逐步执行的工程流程
├── project/           ← 项目定义:需求 / 任务 / 数据模型 / 测试
├── guides/            ← 写代码时的规范:prompt / 验收 / 接口 / Agent
├── knowledge/         ← 可跨项目复用的规范 / 清单 / 模板
├── adr/               ← 重要架构决策及其取舍
├── notes/             ← 课程学习笔记(原文,需求来源)
└── prompts/           ← Prompt 模板 + 前端生成 prompt
```

---

## 一、开工前必读

| 文档 | 什么时候读 |
|---|---|
| [../CLAUDE.md](../CLAUDE.md) / [../AGENTS.md](../AGENTS.md) | 工具读取入口(内容以 CONVENTIONS.md 为准) |
| [project/CONVENTIONS.md](./project/CONVENTIONS.md) | **项目约定总纲**。技术栈、协作流程、编码规范、环境启动 |
| [project/REQUIREMENTS.md](./project/REQUIREMENTS.md) | 确认需求范围 |
| [project/TASKS.md](./project/TASKS.md) | 选当前任务 |
| [../feature_list.json](../feature_list.json) | 当前功能状态、验证步骤与证据 |
| [../claude-progress.md](../claude-progress.md) | 当前已验证状态、blocker 和下一步 |
| [../ARCHITECTURE.md](../ARCHITECTURE.md) | 系统分层、数据流和信任边界 |

## 一·五、工程骨架

| 文档 | 用途 |
|---|---|
| [DESIGN.md](./DESIGN.md) | 产品与架构建模原则 |
| [PLANS.md](./PLANS.md) | 计划、状态和执行计划生命周期 |
| [PRODUCT_SENSE.md](./PRODUCT_SENSE.md) | 产品价值和优先级判断 |
| [QUALITY_SCORE.md](./QUALITY_SCORE.md) | 质量评分标准 |
| [RELIABILITY.md](./RELIABILITY.md) | 超时、SSE、故障恢复边界 |
| [SECURITY.md](./SECURITY.md) | 信任边界、密钥和高风险工具 |
| [FRONTEND.md](./FRONTEND.md) | Vue 前端边界与 SSE 验收 |
| [design-docs/index.md](./design-docs/index.md) | 设计文档与 ADR 入口 |
| [product-specs/index.md](./product-specs/index.md) | 产品规格入口 |
| [exec-plans/tech-debt-tracker.md](./exec-plans/tech-debt-tracker.md) | 技术债与验收条件 |
| [sops/index.md](./sops/index.md) | 分层架构、知识沉淀、可观测性和 UI 验证 SOP |

---

## 二、改代码前查(guides/)

| 文档 | 适用场景 |
|---|---|
| [guides/HARNESS_OVERVIEW.md](./guides/HARNESS_OVERVIEW.md) | **先看这份**。harness 五个部件全景 + 换模型检查清单 |
| [guides/PROMPT_GUIDELINES.md](./guides/PROMPT_GUIDELINES.md) | **改任何 prompt / 新增 AI 功能**。7 问自检 + 冲突检测 + 能力门控 |
| [guides/AGENT_DESIGN.md](./guides/AGENT_DESIGN.md) | **改 agent/ 包**。四层链路 + 5 个必修坑 |
| [guides/API_CONTRACT.md](./guides/API_CONTRACT.md) | **改接口 / 前后端对接**。端点契约 + SSE 陷阱 |
| [guides/CODE_WIKI.md](./guides/CODE_WIKI.md) | 全项目代码导读(437 行) |
| [guides/FRONTEND_SPEC.md](./guides/FRONTEND_SPEC.md) | 前端开发规范(Vue3 + Element Plus) |
| [guides/TESTING_SPEC.md](./guides/TESTING_SPEC.md) | 后端测试规范与流程 |
| [project/SCHEMA.md](./project/SCHEMA.md) | 改数据模型(当前无 DB,仅设计参考) |

---

## 三、交付前必做

| 文档 | 用途 |
|---|---|
| [guides/ACCEPTANCE_CHECKLIST.md](./guides/ACCEPTANCE_CHECKLIST.md) | **四层验收**。接口 → 契约 → 渲染 → 体验,不许跳级 |
| [project/TEST_CASES.md](./project/TEST_CASES.md) | 测试用例清单 |

---

## 四、跨项目复用(knowledge/ + adr/)

| 入口 | 用途 |
|---|---|
| [knowledge/README.md](./knowledge/README.md) | 按安全、RAG、Agent、工程化、模板查找可复用成果 |
| [knowledge/log.md](./knowledge/log.md) | 记录每次沉淀了什么、来源是什么 |
| [adr/README.md](./adr/README.md) | 查看重要架构决策、适用条件和回退方案 |

维护边界:
- `guides/`:本项目当前怎么实现,允许包含具体类名和端点。
- `knowledge/`:换一个项目仍然成立的原则、清单和模板。
- `adr/`:为什么选择某个方案,以及何时应该换方案。
- `notes/`:课程原始学习材料,不作为当前实现的唯一事实来源。

> `docs/` 是开发知识库,不会自动进入运行时 RAG。旅行问答真正加载的是
> `src/main/resources/document/*.md`。

---

## 五、课程笔记(notes/)

`学习笔记-02` ~ `学习笔记-09`,按章节记录课程内容。**这是需求来源,不是实现规范。**

| 文件 | 内容 |
|---|---|
| `学习笔记-大纲篇.md` | 全书大纲 |
| `学习笔记-02-AI大模型接入.md` | 5 种模型接入方式 |
| `学习笔记-03-Prompt与多轮对话.md` | ChatClient + ChatMemory |
| `学习笔记-04-RAG知识库基础.md` | 文档加载 + 向量库 |
| `学习笔记-05-RAG进阶与调优.md` | Re-ranking + 查询扩展 |
| `学习笔记-06-工具调用与MCP.md` | @Tool + MCP 基础 |
| `学习笔记-07-MCP协议深入.md` | MCP 协议 + 服务端 |
| `学习笔记-08-AI智能体构建.md` | 4 层 Agent 架构 |
| `学习笔记-09-AI服务化部署.md` | SSE + Docker |
| `旅行规划-SystemPrompt.md` | 原始 prompt 设计稿 |
| `深度思考练习手册.md` | 练习题库 |

---

## 六、Prompt 模板(prompts/)

| 文件 | 用途 |
|---|---|
| `cursor-vue3-frontend-prompt.md` | 用 Cursor 生成前端的完整 prompt |
| `frontend-generate-prompt.md` | 另一版前端生成 prompt |
| `fronted.md` | UX/UI 协作 prompt |

> 运行时真正生效的 prompt 在 `src/main/resources/prompts/*.st`,不在这个目录。

---

## 七、本项目已修的 13 个缺陷(经验库)

| 类别 | 数量 | 典型 |
|---|---|---|
| 代码缺陷 | 7 | 丢 toolCalls、不推 done、双重编码 |
| 设计缺陷 | 4 | prompt 冲突、数据结构缺字段、缺能力门控 |
| 数据不足 | 1 | 知识库仅 4KB |
| 流程失误 | 1 | 只看 curl 就报完成 |

**结论:多数"AI 答非所问"不是知识库问题,是约束缺失。**
详见 [guides/PROMPT_GUIDELINES.md](./guides/PROMPT_GUIDELINES.md) 第一节的三次事故复盘。
