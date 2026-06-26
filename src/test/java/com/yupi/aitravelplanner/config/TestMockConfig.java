package com.yupi.aitravelplanner.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestMockConfig {

    @Bean
    @Primary
    public VectorStore mockVectorStore() {
        VectorStore mock = mock(VectorStore.class);
        return mock;
    }
}
