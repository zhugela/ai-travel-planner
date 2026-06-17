package com.yupi.aitravelplanner.config;

// Spring AI 核心接口
import org.springframework.ai.document.Document;            // 一个文档切片
import org.springframework.ai.embedding.EmbeddingModel;     // embedding 模型(由阿里 starter 自动注入)
import org.springframework.ai.vectorstore.SimpleVectorStore; // 内存向量库实现
import org.springframework.ai.vectorstore.VectorStore;       // 向量库通用接口(Bean 暴露这个类型)

// Spring 标准注解
import org.springframework.context.annotation.Bean;          // 标记 @Bean 方法,返回 Bean
import org.springframework.context.annotation.Configuration; // 标记配置类

// 步骤 2 写好的文档加载器(@Component,Spring 自动装配)
import com.yupi.aitravelplanner.rag.TravelDocumentLoader;

import java.util.List;

/**
 * 旅游知识库向量库配置
 *
 * 职责:
 *   1) 构造 SimpleVectorStore(基于内存 + DashScope Embedding)
 *   2) 项目启动时,调用 TravelDocumentLoader 加载全部 Markdown
 *   3) 调 vectorStore.add(...) 把所有 Document 转成向量并存入内存库
 *
 * 注意:SimpleVectorStore 是 in-memory,重启后向量会清空,
 *      每次启动都会重新跑一遍 embedding(这就是本步骤的设计目的)
 */
@Configuration
public class TravelVectorStoreConfig {

    /**
     * 构造旅游向量库 Bean
     *
     * 方法参数由 Spring 自动注入:
     *   - EmbeddingModel:由 spring-ai-alibaba-starter-dashscope 自动装配,
     *                    模型名取 application.yml 的 spring.ai.dashscope.embedding.options.model
     *   - TravelDocumentLoader:步骤 2 写的 @Component,Spring 自动创建
     *
     * 方法体执行时机:
     *   Spring 启动 → 需要 travelVectorStore Bean → 调用本方法 →
     *   1) 构造 SimpleVectorStore(此时还没有任何文档)
     *   2) loadMarkdownDocuments() 读 classpath:document/*.md → 切分成 List<Document>
     *   3) store.add(documents) → Spring AI 把每个 Document 调 EmbeddingModel 转成向量,写入内存 Map
     *   4) 返回装好向量的 store
     *
     * 之后 TravelRagService(步骤 5)可以 @Resource 注入 VectorStore 即可检索
     */
    @Bean
    public VectorStore travelVectorStore(EmbeddingModel embeddingModel,
                                         TravelDocumentLoader travelDocumentLoader) {
        // 1) 用 embeddingModel 构造内存向量库
        //    SimpleVectorStore.builder 是 Spring AI 1.0.0 的标准构造方式
        //    内部会持有 EmbeddingModel 引用,后续 add() 时自动调用
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        // 2) 加载全部 Markdown 文档切片
        //    loadMarkdownDocuments() 内置 try/catch,目录为空时返回空 List,不会抛
        List<Document> documents = travelDocumentLoader.loadMarkdownDocuments();

        // 3) 入库:Spring AI 对每个 Document 调 EmbeddingModel.embed(text) 转成 float[],
        //         写入内部 Map<id, vector + Document>
        //    add() 返回 void,不要写 store.add(documents).xxx
        store.add(documents);

        // 4) 返回的 Bean 类型是接口 VectorStore(更通用,后续 Service 注入时不绑死实现)
        return store;
    }
}
