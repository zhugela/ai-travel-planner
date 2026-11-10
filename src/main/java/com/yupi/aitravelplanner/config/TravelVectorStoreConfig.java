package com.yupi.aitravelplanner.config;

import com.yupi.aitravelplanner.rag.TravelDocumentLoader;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 旅游知识库向量库配置(本地版)。
 *
 * 数据源:classpath:document/*.md(3 份攻略)
 * Embedding:LocalEmbeddingConfig 里的本地 ONNX 模型(免 key)
 *
 * SimpleVectorStore 是内存实现,重启后向量清空 → 每次启动重新加载并 embedding。
 */
@Configuration
public class TravelVectorStoreConfig {

    @Bean
    public VectorStore travelVectorStore(EmbeddingModel embeddingModel,
                                         TravelDocumentLoader travelDocumentLoader) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        List<Document> documents = travelDocumentLoader.loadMarkdownDocuments();
        if (!documents.isEmpty()) {
            store.add(documents);
        }
        return store;
    }
}
