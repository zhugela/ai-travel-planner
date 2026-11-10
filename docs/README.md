# docs/ 索引

> 先读哪份,看你要做什么。

```
docs/
├── README.md          ← 本索引
├── project/           ← 项目定义:需求 / 任务 / 数据模型 / 测试
├── guides/            ← 写代码时的规范:prompt / 验收 / 接口 / Agent
├── notes/             ← 课程学习笔记(原文,需求来源)
└── prompts/           ← Prompt 模板 + 前端生成 prompt
```

---

## 一、开工前必读

| 文档 | 什么时候读 |
|---|---|
| [../CLAUDE.md](../CLAUDE.md) | 每次会话开始(行为规范) |
| [project/REQUIREMENTS.md](./project/REQUIREMENTS.md) | 确认需求范围 |
| [project/TASKS.md](./project/TASKS.md) | 选当前任务 |

---

## 二、改代码前查(guides/)

| 文档 | 适用场景 |
|---|---|
| [guides/PROMPT_GUIDELINES.md](./guides/PROMPT_GUIDELINES.md) | **改任何 prompt / 新增 AI 功能**。7 问自检 + 冲突检测 + 能力门控 |
| [guides/AGENT_DESIGN.md](./guides/AGENT_DESIGN.md) | **改 agent/ 包**。四层链路 + 5 个必修坑 |
| [guides/API_CONTRACT.md](./guides/API_CONTRACT.md) | **改接口 / 前后端对接**。端点契约 + SSE 陷阱 |
| [project/SCHEMA.md](./project/SCHEMA.md) | 改数据模型(当前无 DB,仅设计参考) |

---

## 三、交付前必做

| 文档 | 用途 |
|---|---|
| [guides/ACCEPTANCE_CHECKLIST.md](./guides/ACCEPTANCE_CHECKLIST.md) | **四层验收**。接口 → 契约 → 渲染 → 体验,不许跳级 |
| [project/TEST_CASES.md](./project/TEST_CASES.md) | 测试用例清单 |

---

## 四、课程笔记(notes/)

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

## 五、Prompt 模板(prompts/)

| 文件 | 用途 |
|---|---|
| `cursor-vue3-frontend-prompt.md` | 用 Cursor 生成前端的完整 prompt |
| `frontend-generate-prompt.md` | 另一版前端生成 prompt |
| `fronted.md` | UX/UI 协作 prompt |

> 运行时真正生效的 prompt 在 `src/main/resources/prompts/*.st`,不在这个目录。

---

## 六、本项目已修的 13 个缺陷(经验库)

| 类别 | 数量 | 典型 |
|---|---|---|
| 代码缺陷 | 7 | 丢 toolCalls、不推 done、双重编码 |
| 设计缺陷 | 4 | prompt 冲突、数据结构缺字段、缺能力门控 |
| 数据不足 | 1 | 知识库仅 4KB |
| 流程失误 | 1 | 只看 curl 就报完成 |

**结论:多数"AI 答非所问"不是知识库问题,是约束缺失。**
详见 [guides/PROMPT_GUIDELINES.md](./guides/PROMPT_GUIDELINES.md) 第一节的三次事故复盘。
