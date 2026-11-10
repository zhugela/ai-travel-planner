package com.yupi.aitravelplanner.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地 ONNX Embedding 配置。
 *
 * Spring AI 1.0.0 正式版移除了 transformers 自动装配(仅 1.0.0-M6 有),
 * 因此这里手动创建 TransformersEmbeddingModel Bean。
 * 模型:bge-small-zh-v1.5(中文小模型,512 维,首次启动下载约 95MB 到本地缓存)。
 */
@Configuration
public class LocalEmbeddingConfig {

    @Value("${spring.ai.embedding.transformer.onnx.modelUri}")
    private String modelUri;

    @Value("${spring.ai.embedding.transformer.tokenizer.uri}")
    private String tokenizerUri;

    @Value("${spring.ai.embedding.transformer.onnx.modelOutputName:last_hidden_state}")
    private String modelOutputName;

    @Value("${spring.ai.embedding.transformer.cache.dir:${user.home}/.cache/spring-ai-onnx}")
    private String cacheDir;

    @Bean
    public EmbeddingModel embeddingModel() throws Exception {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel(MetadataMode.EMBED);
        model.setModelResource(modelUri);
        model.setTokenizerResource(tokenizerUri);
        model.setModelOutputName(modelOutputName);
        model.setResourceCacheDirectory(cacheDir);
        model.afterPropertiesSet();
        return model;
    }
}
