# ADR-001:本地 Embedding + DeepSeek 生成

- 状态:已接受
- 日期:2026-11-10
- 决策人:项目组

## 背景

百炼账户状态会影响聊天和向量化,导致整个 RAG 链路一起不可用。项目已安装本地运行条件,同时希望知识库检索尽量留在电脑内部,但仍保留效果较好的在线大模型生成能力。

## 决策

采用职责分离架构:
- 文档读取、Embedding、向量检索在本地完成。
- `bge-small-zh-v1.5` ONNX 模型负责中文向量化。
- `SimpleVectorStore` 负责内存检索。
- DeepSeek 只接收用户问题和命中的知识片段,负责最终回答。
- Ollama 和百炼配置保留为可选方案,默认不自动装配。

## 备选方案

| 方案 | 优点 | 缺点 | 未选择原因 |
|---|---|---|---|
| 百炼完成聊天与向量化 | 配置统一、云端托管 | 账户和网络故障会让全链路不可用 | 当前账户不可用,耦合过高 |
| Ollama 同时负责 Embedding 和回答 | 完全本地、隐私边界最清晰 | 本机资源压力更高,回答质量受模型与硬件影响 | 当前只需本地化检索 |
| 本地 Embedding + DeepSeek | 检索私有、生成质量较稳、改造小 | 命中文本仍会发送给云端模型 | 最符合当前资源与演示目标 |

## 影响

### 正向
- 百炼故障不再阻塞知识库向量化。
- 原始文档和完整向量库留在本机。
- ChatModel 与 EmbeddingModel 可独立替换。

### 代价与风险
- 首次运行要准备约 95MB 的本地模型文件。
- `SimpleVectorStore` 重启后重新向量化,不适合大知识库。
- 命中的文本片段会进入 DeepSeek 请求,不能宣称“全链路离线”。
- 当前检索前置判空和 Advisor 可能造成重复检索,后续应统一检索链路并度量延迟。

## 落地与验收

- [x] `LocalEmbeddingConfig` 手动创建本地 EmbeddingModel。
- [x] `TravelVectorStoreConfig` 启动时加载 Markdown 并构建向量库。
- [x] `TravelRagChatService` 无命中时不调用模型。
- [x] DeepSeek 使用 OpenAI 兼容接口提供回答。
- [ ] 增加检索耗时、命中数和相似度观测。
- [ ] 去除重复检索或用测试证明其必要性。

回退条件:本地模型无法部署、知识库规模超过内存方案承载能力,或业务要求全链路离线时,重新评估持久化向量库与本地 ChatModel。

## 项目证据

- `src/main/java/com/yupi/aitravelplanner/config/LocalEmbeddingConfig.java`
- `src/main/java/com/yupi/aitravelplanner/config/TravelVectorStoreConfig.java`
- `src/main/java/com/yupi/aitravelplanner/service/TravelRagChatService.java`
- `src/main/resources/application.yml`

## Related

- [[knowledge/rag/local-rag-architecture]]
- [[knowledge/security/secret-management]]
