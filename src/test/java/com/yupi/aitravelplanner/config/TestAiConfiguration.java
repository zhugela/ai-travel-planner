package com.yupi.aitravelplanner.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * 测试专用 AI 配置 —— 用 Mock Bean 替代真实 AI 组件。
 * <p>
 * 因为 {@link TravelVectorStoreConfig} 在启动时会调用 DashScope Embedding API，
 * 测试环境没有有效 API Key 会导致 Spring 上下文加载失败。
 * 本配置类提供 Mock Bean，优先级高于生产配置，让所有 @SpringBootTest 能正常启动。
 * <p>
 * 用法：测试类上加 {@code @ActiveProfiles("test")} 即可自动生效。
 */
@TestConfiguration
@Profile("test")
public class TestAiConfiguration {

    /**
     * Mock EmbeddingModel —— 拦截所有 embedding 调用，不请求真实 API
     */
    @Bean
    @Primary
    public EmbeddingModel testEmbeddingModel() {
        return mock(EmbeddingModel.class);
    }

    /**
     * Mock VectorStore —— 替代 {@code TravelVectorStoreConfig#travelVectorStore}
     */
    @Bean
    @Primary
    public VectorStore testVectorStore() {
        return mock(VectorStore.class);
    }
}
