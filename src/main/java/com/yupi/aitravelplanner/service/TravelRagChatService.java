package com.yupi.aitravelplanner.service;

// Spring AI - ChatModel:对话模型接口(由 spring-ai-alibaba-starter-dashscope 自动注入)
import org.springframework.ai.chat.model.ChatModel;

// Spring AI - ChatClient:高层 API,Fluent 风格,本类自己构造
import org.springframework.ai.chat.client.ChatClient;

// Spring AI - Advisor 接口(替代之前的 VectorStore + QuestionAnswerAdvisor)
import org.springframework.ai.chat.client.advisor.api.Advisor;

// Spring - 资源加载(读 prompts/travel-rag-system.st)
import org.springframework.core.io.ClassPathResource;

// Spring - 服务注解
import org.springframework.stereotype.Service;

// Lombok - 自动生成 log 字段
import lombok.extern.slf4j.Slf4j;

// JDK - IO
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 旅游规划 RAG 问答服务(云知识库版)
 *
 * 数据源:百炼云知识库"旅游规划"(由 TravelRagCloudAdvisorConfig 注册)
 * 流程:用户提问 → 云检索 → context 拼入 Prompt → 调通义千问 → 返回回答
 * 兜底:模型回答为空时返回固定话术(云检索不到内容时仍会调模型)
 */
@Slf4j
@Service
public class TravelRagChatService {

    /** 知识库无匹配时的兜底回复 */
    public static final String NO_MATCH_REPLY = "暂无相关旅游攻略信息";

    /** RAG 专用 System Prompt 文件位置 */
    private static final String SYSTEM_PROMPT_PATH = "prompts/travel-rag-system.st";

    /** ChatClient(本类自己构造,见构造器) */
    private final ChatClient chatClient;

    /** 云知识库 Advisor(由 TravelRagCloudAdvisorConfig 注册) */
    private final Advisor travelRagCloudAdvisor;

    /**
     * 构造注入 ChatModel + 云 Advisor
     * Spring 自动装配:
     *   - ChatModel:阿里 starter 自动创建的 DashScope ChatModel
     *   - travelRagCloudAdvisor:TravelRagCloudAdvisorConfig 里的 @Bean 方法返回的 Advisor
     */
    public TravelRagChatService(
            ChatModel dashscopeChatModel,
            Advisor travelRagCloudAdvisor) {
        this.travelRagCloudAdvisor = travelRagCloudAdvisor;

        // 读 system prompt 模板(用 UTF-8 编码)
        String systemPrompt = readSystemPrompt();

        // 构造 ChatClient:挂载云 Advisor
        // 云 Advisor(RetrievalAugmentationAdvisor)内部自动:
        //   1) 可选:用 chatModel 重写 query
        //   2) 调 DashScopeDocumentRetriever.retrieve(query) 检索云知识库
        //   3) 把命中片段拼成 context 塞进 user 消息
        //   4) 调 ChatModel 生成回答
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(travelRagCloudAdvisor)
                .build();
    }

    /**
     * RAG 对话统一入口
     * @param userMessage 用户提问
     * @return AI 基于云知识库的回答,或兜底话术
     */
    public String chatWithRag(String userMessage) {
        log.info("========== RAG 请求开始(云知识库) ==========");
        log.info("[1] 用户提问: {}", userMessage);

        // 调用 ChatClient,云 Advisor 在内部完成检索 + context 注入 + 调模型
        String answer = chatClient.prompt()
                .user(userMessage)
                .advisors(travelRagCloudAdvisor)
                .call()
                .content();

        // 兜底:模型回答为空(异常/检索不到)→ 返回固定话术
        // 云 advisor 没有"0 命中就强制兜底"的硬逻辑,
        // 模型即使检索不到也会给通用回答,所以这里只看 answer 是否为空
        if (answer == null || answer.isBlank()) {
            log.warn("[2] 模型回答为空,触发兜底");
            log.info("========== RAG 请求结束(兜底) ==========");
            return NO_MATCH_REPLY;
        }

        // 打印模型输出
        log.info("[2] 模型回答: {}", answer);
        log.info("========== RAG 请求结束(命中) ==========");

        return answer;
    }

    /**
     * 读 resources/prompts/travel-rag-system.st 的内容作为 system prompt
     * 用 UTF-8 读 .st 模板文件
     */
    private String readSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(SYSTEM_PROMPT_PATH);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 读不到 prompt 文件 → 退化为简单提示,不影响主流程
            log.error("读取 RAG System Prompt 失败: {}", SYSTEM_PROMPT_PATH, e);
            return "你是一位旅游规划助手,请根据用户问题给出准确、友好的回答。";
        }
    }
}