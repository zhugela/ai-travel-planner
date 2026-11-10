# 本地 RAG 架构与检索质量

> 状态:核心架构已采用,质量工程待补
> 来源:百炼不可用后的 RAG 本地化改造
> 最后验证:2026-11-10

## 架构结论

将 RAG 拆成两个可以独立替换的职责:

```text
Markdown 文档
  → 本地切片
  → 本地 Embedding
  → 本地向量检索
  → 只把命中片段发给 DeepSeek
  → 生成最终回答
```

本项目采用:
- Embedding:`bge-small-zh-v1.5` ONNX,本机执行。
- 向量库:`SimpleVectorStore`,内存存储。
- 生成模型:DeepSeek OpenAI 兼容接口。
- 无命中策略:直接返回固定兜底,不让模型自由发挥。

这叫“本地检索 + 云端生成”,不是“全链路离线”。原始知识库与向量留在本机,但命中的文本片段会发送给云端模型。

## 可复用设计规则

### 1. 模型职责解耦
- ChatModel 与 EmbeddingModel 分开配置、分开健康检查。
- 向量维度、切片方式和向量库索引是一组版本,更换 Embedding 后必须重建索引。
- 云端模型不可用时,检索层仍应能单独测试和观测。

### 2. 文档边界明确
- 开发文档 `docs/` 与业务知识库不是同一类数据。
- 只有经过审核、切片和元数据标注的内容进入运行时知识库。
- 每个文档至少记录来源、主题、更新时间和适用范围。

本项目运行时知识库目录是 `src/main/resources/document/*.md`,把文件放进 `docs/` 不会自动被 RAG 检索。

### 3. 检索只做一次或显式复用结果

“先检索判断空,再让 Advisor 重检索”会增加延迟,并可能因两次结果不一致造成难以解释的行为。推荐二选一:
- 手动检索一次,把同一批片段注入 Prompt。
- 扩展 Advisor,让它同时暴露命中结果与生成链路。

如果暂时保留双检索,必须记录两次耗时和命中差异。

### 4. 无命中与低置信度分开
- 无命中:没有片段达到阈值,不调用生成模型。
- 低置信度:有片段但证据不足,回答中明确不确定性。
- 命中:回答应可追溯到来源片段,不能只返回一段无来源文本。

### 5. 参数靠评测集调,不靠单次感觉

至少准备三组问题:
- 应命中:文档中有直接答案。
- 近义命中:换一种说法仍应找到。
- 不应命中:知识库外问题必须拒答。

对每组记录 Recall@K、误命中率、首条相似度、延迟和回答忠实度,再调整切片、topK、阈值和重排。

## 上线前清单

- [ ] Embedding 模型文件可离线加载,启动失败信息明确。
- [ ] 更换 Embedding 后会重建向量索引。
- [ ] 文档有来源、更新时间和主题元数据。
- [ ] 检索结果包含分数与来源,日志不记录敏感全文。
- [ ] 无命中不调用生成模型。
- [ ] 评测集同时覆盖命中、近义和拒答。
- [ ] 阈值和 topK 有评测依据。
- [ ] 知识片段发送云端前经过数据分级和脱敏。
- [ ] 知识库增长后评估持久化向量库,避免每次启动全量重算。

## 本项目现状

已采用:
- 本地 ONNX Embedding、本地 SimpleVectorStore、DeepSeek 生成。
- `topK=4`、相似度阈值 `0.35`,无命中固定兜底。
- 运行时只加载 `resources/document/*.md`。

待改进:
- 前置判空与 Advisor 存在重复检索。
- 缺少固定评测集、召回指标和来源展示。
- 内存向量库重启后重新构建,只适合当前小规模知识库。

## 项目证据

- `src/main/java/com/yupi/aitravelplanner/config/LocalEmbeddingConfig.java`
- `src/main/java/com/yupi/aitravelplanner/config/TravelVectorStoreConfig.java`
- `src/main/java/com/yupi/aitravelplanner/service/TravelRagChatService.java`
- `src/main/resources/document/`

## Related

- [[adr/001-local-embedding-deepseek]]
- [[engineering/ai-testing-and-delivery]]
- [[security/secret-management]]
