# 运行数据与数据库 Schema 快照

> **生成物**。不要把它当作未来实体设计的主编辑入口。
> 来源：`docs/project/SCHEMA.md` + `src/main/resources/application.yml`
> 生成日期：2026-11-12
> 生成方式：当前为人工生成快照；引入数据库后应改为脚本生成。

## 1. 当前运行状态

**无数据库。**

| 数据 | 当前存储 | 生命周期 |
|---|---|---|
| 会话消息 | `tmp/chat-memory/{conversationId}.kryo` | 跨进程保留 |
| RAG 文档与向量 | `SimpleVectorStore` 内存 | 应用重启重建 |
| 工具输出 | `tmp/file`、`tmp/pdf`、`tmp/download` | 运行时 |
| 配置 | `application.yml` + 环境变量 | 启动期 |

## 2. 未来实体摘要

以下只来自设计文档，**不是当前数据库表**：

| 实体 | 目的 | 关键字段 |
|---|---|---|
| `user` | 多用户鉴权 | `username`、`password_hash`、`api_key` |
| `conversation` | 会话归属与标题 | `conversation_id`、`user_id`、`title` |
| `message` | 消息历史 | `conversation_id`、`role`、`content`、`metadata` |
| `rag_document` | 攻略文档 | `doc_id`、`filename`、`content`、`metadata` |
| `rag_chunk` | 文档切片与向量 | `doc_id`、`chunk_index`、`embedding` |

完整设计、索引和迁移步骤见
[../project/SCHEMA.md](../project/SCHEMA.md)。

## 3. 不变量

- 当前功能不得把“未来 schema”说成已存在。
- 会话 ID 既是业务键，也可能成为文件名，必须经过安全校验。
- 当前向量维度来自本地 `bge-small-zh-v1.5`，不能沿用旧文档中的 1024/1536 维假设。
- 引入数据库前必须先完成 ADR 和迁移测试。
