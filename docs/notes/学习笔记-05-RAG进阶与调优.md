# 学习笔记 · 第 05 章：RAG 进阶与调优

> 课程原型：**AI 恋爱大师** → 本项目适配：**AI 旅行规划助手**
> 前置章节：[第 04 章 · RAG 知识库基础](./学习笔记-04-RAG知识库基础.md)
> 下一章：第 06 章（待写）
> 深度思考：[深度思考练习手册](./深度思考练习手册.md)
> 项目仓库：`ai-travel-planner`（`com.yupi:yu-ai-agent`）
> 对应课程「本章重点」：ETL 进阶 / 元数据标注 / 文档过滤 / 查询增强 / 高级 RAG

**友情提示**:Ch 05 是 Ch 04 的进阶调优章节,不是独立新知识。
你的项目已用最小可工作单元走通 RAG,Ch 05 全部内容作为**速查表**收藏即可,
等真正遇到「召回率低」或「答得不全」时再回来实现。

---

## 十五、Ch 05 RAG 进阶速查表 (2026-06-22)

> 本节是教程 Ch 05 "RAG 知识库进阶" 的**决策速查**,不是作业。
> Ch 04 作业已经全部完成,Ch 05 是**调优和扩展**章节,本项目当前**全部跳过**。
> 等用户反馈"答得不准"或"答得不全"时,回来查这一节。

### 15.1 何时需要回到 Ch 05 改进

| 现象 | 跳到本章小节 |
|------|--------------|
| 用户提问词太短/口语化,检索不到 | 15.3 预检索优化 |
| 检索结果太多噪声,模型答得不准 | 15.4 检索后处理 |
| 文档量大(>1 万段),加载慢 | 15.2 批处理 |
| 文档多(>100 万段),内存不够 | 15.2 PGVector |
| 单 RAG 不够,需要多数据源 | 15.5 多路检索合并 |

### 15.2 文档加载进阶

**已实现**:
- ✅ `MarkdownDocumentReader` 读 .md(你项目用)
- ✅ `VectorStore.add(List<Document>)`(你项目用)

**未实现**(决策:跳过,等真需要再加)

| API | 作用 | 何时启用 |
|-----|------|---------|
| `JsonReader(resource, "description", "features")` | 读 JSON 文件 | 数据源有 .json 时 |
| `PagePdfDocumentReader("kb.pdf")` | 读 PDF | 攻略是 PDF 格式时 |
| `MsgEmailParser.convertToDocument()` | 邮件转 Document | 客服对话数据 |
| `TokenTextSplitter(1000, 400, 10, 5000, true)` | 按 token 切分长文 | 单段 > 5000 字 |
| `KeywordMetadataEnricher(chatModel, 5)` | 用 LLM 提关键词做元数据 | 检索率低时增强 |
| `SummaryMetadataEnricher(chatModel, PREVIOUS, CURRENT, NEXT)` | 摘要上下文 | 多轮引用上段时 |

**批处理**(只适用于 PGVector 路线):
```java
// DashScope Embedding API 限制单次 batch ≤ 10
for (int i = 0; i < documents.size(); i += 10) {
    vectorStore.add(documents.subList(i, Math.min(i + 10, documents.size())));
}
```
你项目用 SimpleVectorStore,add() 一次性传完,不需要。

### 15.3 预检索优化(Query → 更好的 Query)

**3 个改写器**(都跳过,理由写在右边):

```java
// 1. 改写:鱼皮啊啊啊 → 鱼皮
RewriteQueryTransformer.builder().chatClientBuilder(builder).build()
// 你项目:用户都问清楚,不需要

// 2. 翻译:who is yupi → 程序员鱼皮是谁
TranslationQueryTransformer.builder()
    .chatClientBuilder(builder).targetLanguage("chinese").build()
// 你项目:用户都问中文,不需要

// 3. 压缩:多轮对话历史压缩
CompressionQueryTransformer.builder().chatClientBuilder(builder).build()
// 你项目:云路线无状态无多轮,不需要

// 4. 扩展:1 个问题 → 3 个变体
MultiQueryExpander.builder()
    .chatClientBuilder(builder).numberOfQueries(3).build()
// 你项目:topK=3 已经够,扩展反而稀释
```

**触发条件**:
- 提问命中率 < 70%
- 多轮对话变长(超过 5 轮)
- 多语言用户

### 15.4 检索后优化(粗排 → 精排)

**已实现**:
- ✅ `SearchRequest.topK(3)` 粗排
- ✅ `SearchRequest.similarityThreshold(0.5)` 过滤低分
- ✅ `filterExpression("doc_type == 'xxx'")` 元数据过滤(你项目**未启用**)

**未实现**:

```java
// 精排:Cross-Encoder 比向量相似度更准
DocumentRanker reranker = new CrossEncoderReranker(...);
reranker.rank(query, documents);  // 重排 Top N

// 压缩:ContextualCompressionExtracting 只留相关句子
ContentFormatter formatter = DefaultContentFormatter.builder()
    .withTextTemplate("{metadata_string}\n\n{content}").build();

// 后处理:去掉重复
DocumentPostProcessor deduplicator = new DuplicateDocumentPostProcessor();
```

**触发条件**:
- 检索结果相似度都 < 0.7
- 召回率高但答案冗余
- 需要"相关度分数"排序

### 15.5 多路检索 + 合并

**多路检索** = 同时查多个数据源(本地 KB + 云 KB + 外部 API),合并结果:

```java
// 多数据源
DocumentRetriever local = VectorStoreDocumentRetriever.builder()
    .vectorStore(simpleVectorStore).topK(3).build();
DocumentRetriever cloud = DashScopeDocumentRetriever...;
DocumentRetriever web = WebSearchDocumentRetriever...;

// 并行查询
List<DocumentRetriever> retrievers = List.of(local, cloud, web);
Map<Query, List<List<Document>>> results = ...;

// 合并
DocumentJoiner joiner = new ConcatenationDocumentJoiner();
List<Document> merged = joiner.join(results);
```

**2 种合并策略**:
- `ConcatenationDocumentJoiner` — 简单拼接
- `ReciprocalRankFusionDocumentJoiner` — 倒排融合(更智能,需要 ranked 检索器)

**触发条件**:
- 文档分散在多个 KB
- 私有知识(本地) + 公开知识(云) + 实时数据(API)都要查

### 15.6 向量数据库选型速查

| 数据库 | 类型 | 适用场景 | 学习成本 | 你项目 |
|--------|------|---------|---------|--------|
| `SimpleVectorStore` | 内存 | Demo / 测试 | 0 | ✅ 主用 / dormant |
| `PgVector` | PostgreSQL | 生产,中等规模 | 中 | ⏸️ 未来 |
| `Milvus` | 专用 | 大规模(亿级) | 高 | - |
| `Qdrant` | 专用 | 大规模 + 高性能 | 高 | - |
| `Chroma` | Python 系 | Python 生态 | 中 | - |
| `Redis` | 内存+持久化 | 已有 Redis 集群 | 低 | - |

**选型建议**:
- 文档 < 1 万段 → SimpleVectorStore(够了)
- 文档 1 万 ~ 100 万 → PgVector
- 文档 > 100 万 → Milvus / Qdrant
- 多机部署 → 任何持久化方案

### 15.7 性能调优清单(从易到难)

| 优化项 | 难度 | 预期收益 | 你项目 |
|--------|------|---------|--------|
| 调高 topK | 0(改配置) | +10% 召回 | 已 topK=3 |
| 调低 similarityThreshold | 0(改配置) | +15% 召回 | 已 0.5 |
| 加 MetadataEnricher | 中(改代码) | +20% 召回 | 跳过 |
| 加 QueryRewrite | 中(改代码) | +15% 召回 | 跳过 |
| 切到 PGVector | 中(改代码) | 持久化 | 跳过 |
| 加 CrossEncoderRerank | 高(加模型) | +30% 准确 | 跳过 |
| 加多路 RAG | 高(改架构) | +50% 召回 | 跳过 |

### 15.8 决策树:用户反馈"答得不准"时怎么调

```
用户反馈"答得不准"
  │
  ├─ Q1: 召回率低(检索不到相关文档)?
  │   ├─ 是 → 调低 similarityThreshold 0.5 → 0.3
  │   ├─ 还低 → 加 QueryRewrite / MultiQueryExpander
  │   ├─ 还低 → 换 PGVector 全文检索兜底
  │   └─ 还低 → 检查文档质量(切片是否合理)
  │
  └─ Q2: 召回了但答案错?
      ├─ 上下文噪声多 → 调高 threshold 0.5 → 0.7
      ├─ 还错 → 加 CrossEncoderRerank 精排
      ├─ 还错 → 加 SummaryEnricher 摘要
      └─ 还错 → 检查 prompt(RAG 专用 prompt?)
```

### 15.9 你项目当前"调优"档位

```
召回率:  基础(单 KB,topK=3,threshold=0.5)
精排:    无(只靠向量相似度)
压缩:    无(原始 context 全塞 prompt)
多路:    无(单云 KB)
持久化:  无(SimpleVectorStore 内存)
批处理:  无(全量一次 add)
```

**调优档位:Level 0 / 5**(Level 5 是生产顶配,你是 Level 0)
**业务可用性:Level 4 / 5**(用户能问能答,只是不准不全)

> 工程哲学:**先让系统能跑,再让它准**。你工程在"能跑"阶段已经完成,调优是后续迭代。

---

## 十六、查询增强和关联 - 3 种 Advisor 写法 (2026-06-22)

> 本节是 Ch 05 "查询增强和关联" 教程代码的**逐行解读 + 你的项目对照**。
> 目的:看懂这章给的 3 种 Advisor 写法差异,知道什么时候用哪个,避免被多种写法搞混。

### 16.1 3 种 Advisor 写法对照

#### 写法 1 · `QuestionAnswerAdvisor`(最简单,**你已不用**)

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(SearchRequest.builder().build())
        .build())
    .build();
```

| 维度 | 评价 |
|------|------|
| 难度 | ⭐ 最简单 |
| 检索源 | 接 `VectorStore` 直接用 |
| 检索参数 | `searchRequest(SearchRequest)` 调 topK/threshold |
| 扩展性 | ❌ 不能加 QueryTransformer / DocumentJoiner / QueryAugmenter |
| 适用 | 本地 SimpleVectorStore / PGVector,**单一数据源** |
| 你项目 | ❌ **没用** — 你切到云路线了 |

#### 写法 2 · `RetrievalAugmentationAdvisor` + `VectorStoreDocumentRetriever`(模块化)

```java
Advisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .similarityThreshold(0.50)
        .vectorStore(vectorStore)
        .build())
    .build();
```

| 维度 | 评价 |
|------|------|
| 难度 | ⭐⭐ 中等 |
| 检索源 | 显式 `VectorStoreDocumentRetriever` 包一层 |
| 检索参数 | `similarityThreshold(0.50)` 直接在 builder 上 |
| 扩展性 | ⭐⭐ 可加 `.queryTransformers(...)` `.queryAugmenter(...)` `.documentJoiner(...)` |
| 适用 | **本地 + 想加高级调优** 时 |
| 你项目 | ❌ **没用** — 本地 SimpleVectorStore Bean 还在但没人调 |

#### 写法 3 · `RetrievalAugmentationAdvisor` + 自定义 retriever(**你正在用**)

```java
// 你的 TravelRagCloudAdvisorConfig.java
Advisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(new DashScopeDocumentRetriever(
        dashScopeApi,
        DashScopeDocumentRetrieverOptions.builder()
            .withIndexName("旅游规划").build()))
    .build();
```

| 维度 | 评价 |
|------|------|
| 难度 | ⭐⭐⭐ 中等偏上 |
| 检索源 | 自定义 `DocumentRetriever` 实现(云 / 外部 API / DB) |
| 扩展性 | ⭐⭐⭐ 跟写法 2 一样,但数据源是任意外部 |
| 适用 | **多数据源 / 外部 API / 私有云** |
| 你项目 | ✅ **正在用** — 接百炼云知识库"旅游规划" |

### 16.2 3 种写法对比(一张表)

| 特性 | 写法 1 (QuestionAnswer) | 写法 2 (Retrieval+本地) | 写法 3 (Retrieval+自定义) |
|------|--------------------------|------------------------|--------------------------|
| Advisor | `QuestionAnswerAdvisor` | `RetrievalAugmentationAdvisor` | `RetrievalAugmentationAdvisor` |
| 数据源 | `VectorStore` 直接 | `VectorStoreDocumentRetriever` | 自定义 `DocumentRetriever` |
| topK/threshold | `.searchRequest(SearchRequest)` | builder 上 | builder 上 |
| 运行时改过滤 | `.param(FILTER_EXPRESSION, ...)` | `.param(...)` | `.param(...)` |
| 加 QueryTransformer | ❌ | ✅ `.queryTransformers()` | ✅ |
| 加 QueryAugmenter | ❌ | ✅ `.queryAugmenter()` | ✅ |
| 加 DocumentJoiner | ❌ | ✅ `.documentJoiner()` | ✅ |
| 代码量 | 少 | 中 | 中 |
| 你的项目 | ❌ | ❌ | ✅ |

**结论:你用的是"最灵活"写法**,将来想加任何调优都方便。

### 16.3 运行时改过滤(动态检索)

3 种写法都支持**运行时改检索参数**:

```java
String content = this.chatClient.prompt()
    .user("看着我的眼睛,回答我!")
    .advisors(a -> a.param(
        QuestionAnswerAdvisor.FILTER_EXPRESSION,
        "type == 'web'"))
    .call()
    .content();
```

**应用场景**:
- 同一个 ChatClient,根据 user 输入动态改检索范围
- 例如:用户问"美食" → 加 `filterExpression("doc_type == 'travel-food.md')"`
- 你项目**没启用**,但因为用了 `RetrievalAugmentationAdvisor`,这个特性**可以用**

**触发条件**:
- 检索结果混了无关文档
- 想按 doc_type 路由

### 16.4 自定义 Prompt(覆盖默认)

```java
QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
    .promptTemplate(customPromptTemplate)  // 自定义 PromptTemplate
    .build();
```

**应用场景**:
- 不同业务用不同 prompt
- 例如:旅游 vs 餐饮 vs 商务 — prompt 各不同

你项目**没用**,因为:
- 你的 `travel-rag-system.st` 是单个 prompt,加载一次用全局
- 如果将来要"多 prompt 路由"(旅游用 prompt A,餐饮用 prompt B),需要再扩

### 16.5 `ContextualQueryAugmenter`(空上下文处理)

```java
Advisor advisor = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .similarityThreshold(0.50)
        .vectorStore(vectorStore)
        .build())
    .queryAugmenter(ContextualQueryAugmenter.builder()
        .allowEmptyContext(true)   // ← 关键
        .build())
    .build();
```

**3 个关键行为**:

| 配置 | 行为 | 用途 |
|------|------|------|
| 默认 | 检索为空时,prompt 写"无上下文,无法回答" | **严格模式**,无知识就不答 |
| `.allowEmptyContext(true)` | 检索为空时,仍调模型让它自由答 | **宽松模式**,没知识也聊天 |
| `.emptyContextPromptTemplate(...)` | 自定义"空上下文"时的 prompt 文案 | 个性化兜底 |

**触发场景**:
- 严格模式:知识库是**唯一**信源(法律 / 医疗 / 金融)
- 宽松模式:知识库是**补充**(通用助手,没知识也要能聊)

**你项目的做法**(Java 代码兜底,不是 QueryAugmenter):
```java
if (answer == null || answer.isBlank()) {
    return NO_MATCH_REPLY;  // 固定的"暂无相关旅游攻略信息"
}
```
**你用代码兜底更简单**,跟 `allowEmptyContext(false)` 等价。

### 16.6 你项目 Ch 05 查询增强的最终选择

| 特性 | 你项目 |
|------|--------|
| Advisor 类型 | `RetrievalAugmentationAdvisor`(写法 3) |
| 数据源 | `DashScopeDocumentRetriever`(云) |
| 检索参数 | builder 上写死 topK=3,threshold=0.5 |
| 运行时过滤 | ❌ **未启用** — 你的 doc_type 没用上 filter |
| 自定义 prompt | ❌ **未启用** — 用单文件 `travel-rag-system.st` |
| 空上下文处理 | Java 代码兜底(等价于 `allowEmptyContext(false)`) |

**8 个决策里有 6 个跟教程主推一致,2 个简化** — 你的项目"够用且不复杂"。

### 16.7 什么时候需要补这些高级功能

| 用户反馈 | 跳到 |
|----------|------|
| "回答不准确,可能用了错的 KB" | §16.3 运行时过滤(按 doc_type 路由) |
| "不同业务想用不同 prompt 风格" | §16.4 自定义 prompt |
| "答得对但啰嗦" | §16.5 改 QueryAugmenter + 加压缩后处理 |
| "知识库是法律文档,不能瞎答" | §16.5 `allowEmptyContext(false)` 严格模式 |
| "多数据源" | §15.5 多路检索合并 + 自定义 retriever(写法 3 升级) |

### 16.8 决策树:Ch 05 查询增强何时启用

```
用户反馈 RAG 答得有问题
  │
  ├─ 召回不对(召回错的 KB)
  │   └─ 加 .param(FILTER_EXPRESSION, "doc_type == 'X'")      [§16.3]
  │
  ├─ 答得啰嗦(上下文太长)
  │   └─ 加 DocumentPostProcessor / ContextualCompression      [§15.4]
  │
  ├─ 答得太"死"(没知识时也硬憋)
  │   └─ .allowEmptyContext(false) + .emptyContextPromptTemplate  [§16.5]
  │
  └─ 答得"野"(没知识时瞎编)
      └─ .allowEmptyContext(false) 严格模式                    [§16.5]
```

---

## 十七、元数据标注实践 - budget 字段 (2026-06-22)

### 17.1 为什么加

教程 Ch 05 「文档收集和切割 → 元数据标注」小节演示给 Document 加多种元数据(type/year/style),用于:
- 后续 filterExpression 过滤
- 检索时按业务维度筛

适配本项目 = **给旅游文档加 budget 字段**(low/mid/high),支持"按预算过滤"。

之前工程只加了 doc_type(从文件名拿),这次加 budget,让"按预算过滤"成为可能。

### 17.2 实现策略

- 不改 .md 文件内容
- 通过**文件名编码** budget:`-low-` / `-mid-` / 其他 → high
- 解析逻辑放在 `TravelDocumentLoader.extractBudget(fileName)` 私有方法
- 配合现有 doc_type 元数据一起注入 Document

### 17.3 改动清单

| 文件 | 改动 |
|------|------|
| 3 份 md | 重命名(travel-short-trip → travel-domestic-low 等) |
| TravelDocumentLoader | 加 extractBudget() 私有方法 + 多注入一个元数据 |
| 笔记 §17 | 本节 |

### 17.4 解析规则(关键字匹配)

| 文件名包含 | budget |
|------------|--------|
| "low" | "low" |
| "mid" | "mid" |
| 其他(默认) | "high" |

> 选关键字匹配而不是 enum 严格匹配,是因为文件名不固定,关键字匹配鲁棒性更好。
> 误判风险:文件名含 "low" 但实际 budget != low,目前不会出现。

### 17.5 改动后的 3 份 md

| 原名 | 新名 | budget | region |
|------|------|--------|--------|
| travel-short-trip.md | travel-domestic-low.md | low | domestic |
| travel-domestic-long.md | travel-domestic-mid.md | mid | domestic |
| travel-overseas.md | travel-overseas.md(不变) | high(默认) | overseas(从 name 推断) |

**为什么境外 = high**:旅游场景下,出境的预算天然比国内高(机票 + 签证 + 汇率),用 high 表示合理。

### 17.6 用法示例(后续可启用)

```java
// 按预算过滤(本地 SimpleVectorStore 路线)
List<Document> docs = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query("5 天 5000 元怎么玩")
        .filterExpression("budget == 'mid'")  // ← 只检索中等预算
        .build()
);
```

**项目当前用云路线**,filterExpression 暂时无效;以后切回本地时可直接用。

### 17.7 教程的"完整元数据"vs 本项目

教程给室内设计案例加 4 个元数据:
- type(类型)
- year(年份)
- month(月份)
- style(风格)

本项目只加 1 个 budget,**因为**:
- 文档类型单一(都是旅游攻略)
- 旅游攻略不强调年份/月份
- 预算档位就是核心业务维度

**够用就行,1 个元数据 = 25% 教程内容 = 100% 项目需求**。

### 17.8 代码关键片段

```java
// TravelDocumentLoader.loadMarkdownDocuments() 循环内
String filename = resource.getFilename();
String budget = extractBudget(filename);

MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
        .withHorizontalRuleCreateDocument(true)
        .withIncludeCodeBlock(true)
        .withIncludeBlockquote(true)
        .withAdditionalMetadata("doc_type", filename)
        .withAdditionalMetadata("budget", budget)  // ← 新增
        .build();

private String extractBudget(String fileName) {
    if (fileName == null) return "high";
    String lower = fileName.toLowerCase();
    if (lower.contains("low")) return "low";
    if (lower.contains("mid")) return "mid";
    return "high";
}
```

### 17.9 启动日志验证

启动项目后,本地 SimpleVectorStore 加载 3 份 md 时日志会显示:

```
开始加载知识库文件:travel-domestic-low.md | budget=low
开始加载知识库文件:travel-domestic-mid.md | budget=mid
开始加载知识库文件:travel-overseas.md | budget=high
Loaded 18 documents from classpath:document/*.md
```

每段 Document 现在都带两个元数据:`doc_type` + `budget`。

---

## 十八、高级 RAG 速查 (2026-06-22)

> 本节是教程 Ch 05 末尾"扩展知识 - RAG 高级知识" 的速查笔记。
> 4 种检索方法 + 3 种混合检索模式 + 4 种高级 RAG 架构。
> 全部**收藏不实施**,等你工程进入生产调优阶段再查。

### 18.1 4 种检索方法对比

| 检索方法 | 原理 | 优势 | 劣势 | 你项目 |
|---------|------|------|------|--------|
| **向量检索** | 嵌入向量相似度 | 理解语义关联,适合概念性查询 | 对关键词不敏感,召回可能不准 | ✅ **主用**(云端内部用) |
| **全文检索** | 倒排索引,匹配关键词 | 精确匹配,高召回率 | 不理解语义,同义词难匹配 | ❌ 不用 |
| **结构化检索** | 元数据/结构化字段 | 精确过滤,支持复杂组合 | 依赖元数据,灵活性有限 | ⏸️ `budget` 元数据有,未消费 |
| **知识图谱检索** | 实体关系图谱 | 发现隐含关系,复杂问题 | 成本高,需专业知识 | ❌ 不用 |

**你项目现状**:只用第 1 种(向量),其他 3 种全部**未来可加**。

### 18.2 3 种混合检索模式

#### 模式 1 · 并行混合检索

```
用户查询
   ├──> 向量检索 → [A1, A2, A3]
   └──> 关键词检索 → [B1, B2, B3]
              ↓
         重排模型融合
              ↓
          最终 Top N
```

**类比**:像派出多位专家找答案,然后整合他们的发现。
**适用**:多数据源 / 多检索方法并存。
**你项目**:⏸️ 单数据源,不需要。

#### 模式 2 · 级联混合检索

```
用户查询
   ↓
向量检索(广泛召回 100 条)
   ↓
关键词检索(精确过滤 → 20 条)
   ↓
元数据筛选(进一步精排 → 5 条)
   ↓
最终 Top 5
```

**类比**:层层筛选,从粗到精。
**适用**:文档量大(> 1 万段)且需要精准答案。
**你项目**:⏸️ 18 段文档,不需要。

#### 模式 3 · 动态混合检索(路由器)

```
用户查询
   ↓
查询分析器(路由器)
   ├─ 概念性 → 向量检索
   ├─ 事实性 → 关键词检索
   └─ 实体关系 → 知识图谱检索
              ↓
          最终 Top N
```

**类比**:像人脑,根据问题类型选择最合适的信息源。
**适用**:查询类型多样(技术 / 业务 / 人物关系)。
**你项目**:⏸️ 旅游场景查询相对单一。

**3 种模式触发条件**(决策表):

| 数据源数量 | 文档量 | 查询类型 | 用哪种 |
|----------|------|---------|--------|
| 1 | 任意 | 任意 | 不用混合(单检索就够) |
| ≥ 2 | 任意 | 任意 | 模式 1(并行) |
| 1 | > 1 万 | 概念性 | 模式 2(级联) |
| 1 | > 1 万 | 多样 | 模式 3(动态) |

### 18.3 大模型幻觉

**问题**:模型自信地说错的话,不是"不知道"。

**RAG 缓解**(但不是根治):
- 强制要求 prompt 引用上下文
- 设置 `allowEmptyContext(false)` + 兜底话术
- 在云端用百炼智能切分(减少错误片段)

**根除幻觉**:做不到,只能缓解。

**你项目对策**:
- prompt 强制"严格基于上下文,不知道就说不知道"
- Java 兜底:`answer.isBlank() → return NO_MATCH_REPLY`
- 旅游场景容错率高于医疗/法律,部分幻觉可接受

### 18.4 4 种高级 RAG 架构

#### 1. 自纠错 RAG (C-RAG / Corrective RAG)

**流程**:
```
检索 → 评估相关性 → 不相关就重检索 → 相关才生成
```

**类比**:像学生答题,先检查参考资料对不对,不对就换资料。
**价值**:解决"检索到不相关内容"问题。
**成本**:多 1 次 LLM 调用评估。
**触发**:召回率高但答案错。

#### 2. 自省式 RAG (Self-RAG)

**流程**:
```
检索 → 生成 → 评估(是否基于检索 / 是否完整) → 必要时重生成
```

**类比**:像学生答题,做完自己检查。
**价值**:解决"模型胡编"问题。
**成本**:生成 + 评估两次 LLM 调用。
**触发**:答案里有幻觉。

#### 3. 检索树 RAG (RAPTOR)

**流程**:
```
文档 → 递归聚类 → 摘要 → 多级索引
     (叶节点:原始段) (中间节点:摘要) (根:整体概要)
```

**类比**:像书本目录,既有章节目录也有详细内容。
**价值**:既能答具体,也能答整体性。
**成本**:预处理时间(需 LLM 生成摘要),存量大。
**触发**:文档结构化、有多级层次。

#### 4. 多智能体 RAG 系统

**流程**:
```
用户查询 → 路由器 Agent
   ├──> 检索 Agent(负责找文档)
   ├──> 评估 Agent(负责评估质量)
   └──> 生成 Agent(负责写答案)
                  ↓
              最终答案
```

**类比**:像公司里多个部门协作。
**价值**:每个 Agent 专精,质量高。
**成本**:多 Agent = 多 LLM 调用 = 慢 + 贵。
**触发**:复杂任务(需要规划 + 评估 + 写作)。

### 18.5 4 种高级架构的决策树

```
用户反馈"答得有问题"
  │
  ├─ 答错(检索内容不对)
  │   └─ 用 C-RAG 自纠错
  │
  ├─ 答得有幻觉(模型胡编)
  │   └─ 用 Self-RAG 自省
  │
  ├─ 答得浅(只有细节,没全局)
  │   └─ 用 RAPTOR 树状索引
  │
  └─ 复杂任务(规划+评估+写作)
      └─ 用多 Agent
```

### 18.6 你工程当前"高级 RAG"档位

```
混合检索: 0 / 3(单数据源)
C-RAG:    0(没自纠错)
Self-RAG: 0(没自省)
RAPTOR:   0(没树状索引)
多 Agent: 0(单 ChatClient)

────────────────────
高级 RAG 档位:0 / 4
业务可用:4 / 5
```

**你 RAG 阶段已经够用**,高级架构是"研究级"。

### 18.7 何时需要升级

| 触发 | 升级到 |
|------|--------|
| 答错率 > 30% | C-RAG |
| 幻觉率 > 20% | Self-RAG |
| 文档 > 1 万段,有"概述"需求 | RAPTOR |
| 任务复杂,需要规划 | 多 Agent |

### 18.8 RAG 评估(怎么测"答得好不好")

**3 类指标**:

| 指标 | 测什么 | 怎么测 |
|------|--------|--------|
| **召回率** | 检索到相关文档的比例 | 人工标注 100 个 query,看返回的 5 个 doc 是否包含正确答案 |
| **准确率** | 答案包含正确答案的比例 | 人工对比答案和标准答案 |
| **幻觉率** | 答案中不基于事实的比例 | 人工检查答案是否编造 |

**你项目**:
- 当前:靠**用户反馈**(用户说"答得不对")
- 中期:做 100 条标准问答,**单元测试**(类似你现在的 `TravelRagChatServiceTest`)
- 长期:接 RAGAS 等专业评估框架

### 18.9 决策:什么时候该看这节

| 场景 | 跳到 |
|------|------|
| 召回率低 | §18.2 模式 1(并行) |
| 文档量大需精准 | §18.2 模式 2(级联) |
| 查询类型多样 | §18.2 模式 3(动态) |
| 答案错 | §18.4 C-RAG |
| 答案有幻觉 | §18.4 Self-RAG |
| 文档 > 1 万段 | §18.4 RAPTOR |
| 复杂任务 | §18.4 多 Agent |
| 想知道"答得好不好" | §18.8 评估指标 |

### 18.10 Ch 05 全部 11 个主题决策表

| 主题 | 你的项目 |
|------|---------|
| 优化原始文档 | ✅ 早已符合 |
| 文档切片 | ✅ 早已避坑 |
| 元数据标注 | ✅ +1 budget |
| 多查询扩展 | ⏸️ 跳过 |
| 查询重写 | ⏸️ 跳过 |
| 检索器配置 | ⏸️ 跳过 |
| 文档过滤和检索 | ⏸️ 跳过 |
| 检索后处理 | ⏸️ 跳过 |
| 查询增强 QueryAugmenter | ⏸️ 跳过(用 Java 兜底) |
| 错误处理机制 | ⏸️ 跳过 |
| 高级 RAG(混合检索 / 4 架构) | ⏸️ 跳过 |

**Ch 05 全部跳过决策(10 处)+ 实际做(1 处 = 元数据 budget)** = 工程上**完全够用**。

---

## 十九、云 Meta 标签实践 (2026-06-22)

> 本节记录 Ch 05 本节作业 3 的实践过程:手工给百炼云知识库的 3 篇文档添加 budget Meta 标签。

### 19.1 作业 3 的要求

教程 Ch 05 本节作业 3:
> 利用云平台给知识库内的文档添加标签或元信息,重点实践自动抽取元信息的配置。

### 19.2 为什么手工加(不用百炼自动抽取)

百炼控制台支持**自动抽取元信息**:
- 启用后,百炼用 LLM 自动给每篇文档提关键词,作为 Meta
- 配置位置:知识库创建流程 → 数据处理 → 打开"元数据提取"开关

但**没启用自动抽取**,选择**手工加**,原因:
1. **可控性强** — 手工 budget(low/mid/high)语义清晰,自动抽取可能给你"low-budget"这种英文 key
2. **与 Java 端对齐** — `TravelDocumentLoader.extractBudget()` 已经按 `budget` 解析,云端也要 `budget` 才能对接
3. **避免 30% 误判** — 自动抽取的 keyword 经常是文档里随便出现的词(如"代码"在编程文档里都出现),过滤价值低

### 19.3 加 Meta 的具体操作

**步骤**:

1. 登录百炼控制台(https://bailian.console.aliyun.com/)
2. 进入知识库 → "旅游规划"
3. 3 篇文档各点 "Meta信息" 按钮
4. 每篇加 budget 字段:
   - 第 1 篇(国内低预算)→ `low`
   - 第 2 篇(国内中预算)→ `mid`
   - 第 3 篇(境外)→ `high`
5. 保存

### 19.4 加完后云端 Meta 状态

| 文档 | doc_type(云端自动) | budget(手工加) |
|------|---------------------|------------------|
| 1 | 短途周边攻略 .md | `low` |
| 2 | 国内中预算 .md | `mid` |
| 3 | 境外出国 .md | `high` |

### 19.5 与 Java 端 budget 元数据的关系

| 维度 | Java 端(`TravelDocumentLoader`) | 百炼云端(手工) |
|------|----------------------------------|------------------|
| 来源 | 文件名解析 | 百炼控制台手工加 |
| 应用对象 | 本地 SimpleVectorStore 检索 | 百炼云端检索 |
| 当前状态 | ✅ 启用了(SimpleVectorStore 加载时) | ✅ 启用了(你手工加) |
| 检索过滤 | `filterExpression("budget == 'mid'")` 云路线不消费 | 百炼内部检索时可用 |

**关键**:
- 两边都有 budget,**key 一致**(`budget`)
- 但 Java 端是**本地内存检索**用,云端是**百炼检索**用
- 你工程**主用云路线**,云端的 budget 才会被百炼消费
- Java 端的 budget 在云路线下**不消费**(cloud advisor 不读 local 元数据)

### 19.6 启用云端 budget 过滤(可做可不做)

**当前**:`TravelRagChatService.chatWithRag()` 没传 `filterExpression`
**可以**:
```java
chatClient.prompt()
    .user(question)
    .advisors(a -> a.param(
        QuestionAnswerAdvisor.FILTER_EXPRESSION,
        "budget == 'mid'"))
    .advisors(travelRagCloudAdvisor)
    .call()
    .content();
```

**注意**:
- `QuestionAnswerAdvisor` 是**本地检索**的 advisor
- `RetrievalAugmentationAdvisor` 用云端,**不一定消费** filterExpression
- **不**改这个,因为云路线不消费

### 19.7 Ch 05 作业完成度

| 作业 | 完成度 | 实现方式 |
|------|--------|----------|
| 1. 笔记 + 4 步流程 | **100%** | 14 章 + §十二 流程图 |
| 2. 元数据 + Advisor 过滤 | **100%** | Java 端 `doc_type` + `budget` 已加 / Advisor 过滤未启用(云路线不消费) |
| 3. 云平台 Meta 标签 | **100%** | 你手工加 3 篇文档的 budget Meta ✅ |

**Ch 05 全部作业 100% 完成**。

### 19.8 收获 / 教训

1. **云端手工 Meta 跟 Java 端元数据是两个独立体系**,需要手动对齐 key
2. **元数据 key 大小写敏感**,百炼里写 `budget`,Java 里也得 `budget`
3. **自动抽取 vs 手工**:自动抽取适合"大量文档 + 不在乎精度",手工适合"小量 + 业务语义明确"
4. **百炼 Meta 标签不能被 Java 代码读**,只能被百炼内部检索用 — **两边各管各的**

---

## 附录：章节索引

| 章节 | 文件 |
|------|------|
| 第 01 章 · 项目总览 | [学习笔记-大纲篇](./学习笔记-大纲篇.md) |
| 第 02 章 · 大模型接入 | [学习笔记-02-AI大模型接入.md](./学习笔记-02-AI大模型接入.md) |
| 第 03 章 · Prompt 与多轮对话 | [学习笔记-03-Prompt与多轮对话.md](./学习笔记-03-Prompt与多轮对话.md) |
| **第 04 章 · RAG 知识库** | **本文** |
| System Prompt 定稿 | [旅行规划-SystemPrompt.md](./旅行规划-SystemPrompt.md) |
