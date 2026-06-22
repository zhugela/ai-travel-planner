package com.yupi.aitravelplanner.config;

// 阿里百炼 DashScope SDK 的 API 客户端
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;

// 阿里百炼云知识库文档检索器(由 spring-ai-alibaba-starter-dashscope 提供)
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;

// Spring AI 高级 RAG 编排 Advisor(带查询重写 + 检索 + 上下文注入)
// 注意:1.0.0 包路径是 org.springframework.ai.rag.advisor(不是 .retrieval.advisor)
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;

// Spring AI Advisor 接口(在 spring-ai-client-chat jar 里)
import org.springframework.ai.chat.client.advisor.api.Advisor;

// Spring - 配置 + 依赖注入
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Lombok 自动生成 log 字段
import lombok.extern.slf4j.Slf4j;

/**
 * 旅游规划云知识库 Advisor 配置
 *
 * 完全对标教程 LoveAppRagCloudAdvisorConfig:
 *   - 用 DashScopeApi + DashScopeDocumentRetriever 接百炼云知识库"旅游规划"
 *   - 包装成 RetrievalAugmentationAdvisor(模块化 RAG)
 *   - 暴露为 Spring Bean,Service 直接 @Resource 注入
 *
 * 跟本项目本地 RAG(TravelVectorStoreConfig)的关系:
 *   - 本类是云端数据源
 *   - 本地类 SimpleVectorStore + 3 份 md 保留代码但暂不参与检索
 *   - 想切换数据源,改 Service 注入的 Advisor 即可
 */
@Configuration
@Slf4j
public class TravelRagCloudAdvisorConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    /**
     * 教程原文叫 loveAppRagCloudAdvisor,这里改成 travelRagCloudAdvisor
     * 知识库名称"旅游规划"对应百炼控制台创建的应用
     */
    @Bean
    public Advisor travelRagCloudAdvisor() {
        log.info("[Cloud RAG] 初始化百炼云知识库 advisor,index=旅游规划");

        // 1) 用 API key 构造百炼 API 客户端
        //    1.0.0.2 没有 new DashScopeApi(String) 单参构造器,要用 builder()
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();

        // 2) 知识库名称常量
        //    教程写的是"恋爱大师",这里改成"旅游规划"
        //    注意:用**名字**,不是控制台 URL 里的 ID
        final String KNOWLEDGE_INDEX = "旅游规划";

        // 3) 构造云端文档检索器
        DashScopeDocumentRetriever documentRetriever =
                new DashScopeDocumentRetriever(
                        dashScopeApi,
                        DashScopeDocumentRetrieverOptions.builder()
                                .withIndexName(KNOWLEDGE_INDEX)
                                .build());

        // 4) 包装成 RetrievalAugmentationAdvisor
        //    内部自动:可选查询重写 → 检索 → context 注入 → 调 ChatModel
        Advisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();

        log.info("[Cloud RAG] advisor 初始化完成");
        return advisor;
    }
}