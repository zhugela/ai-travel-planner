package com.yupi.aitravelplanner.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 旅游规划 RAG 问答服务(本地知识库版)。
 *
 * 数据源:resources/document/*.md → SimpleVectorStore(本地 ONNX embedding)
 * 流程:用户提问 → 向量检索 top-K → 拼接 context → DeepSeek 生成回答
 */
@Slf4j
@Service
public class TravelRagChatService {

    /** 知识库无匹配时的兜底回复 */
    public static final String NO_MATCH_REPLY = "暂无相关旅游攻略信息";

    private static final String SYSTEM_PROMPT_PATH = "prompts/travel-rag-system.st";
    private static final String RECOMMEND_SYSTEM_PROMPT_PATH = "prompts/destination-recommend-system.st";

    /** 检索条数 */
    private static final int TOP_K = 4;
    /** 相似度阈值,低于此值视为未命中。
     *  bge-small-zh 对泛化提问(如"杭州有什么好玩的")打分偏低,0.45 会误杀,取 0.35。 */
    private static final double SIMILARITY_THRESHOLD = 0.35;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Advisor localRagAdvisor;

    public TravelRagChatService(ChatModel chatModel, VectorStore travelVectorStore) {
        this.vectorStore = travelVectorStore;
        // 本地向量检索 Advisor:命中片段自动注入 prompt 的 question_answer_context
        this.localRagAdvisor = QuestionAnswerAdvisor.builder(travelVectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build())
                .build();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(readSystemPrompt())
                .build();
    }

    /**
     * RAG 对话统一入口。
     *
     * 与 LLM 生成前先做一次检索判空:没有命中就不调模型,直接返回兜底,
     * 避免"知识库里没有,模型却自由发挥"——这是本项目 RAG 的验收要求。
     */
    public String chatWithRag(String userMessage) {
        log.info("========== RAG 请求开始(本地知识库) ==========");
        log.info("[1] 用户提问: {}", userMessage);

        if (vectorStore.similaritySearch(SearchRequest.builder()
                .query(userMessage)
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build()).isEmpty()) {
            log.info("[2] 知识库无命中,返回兜底");
            return NO_MATCH_REPLY;
        }

        String answer = chatClient.prompt()
                .user(userMessage)
                .advisors(localRagAdvisor)
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            log.warn("[3] 模型回答为空,触发兜底");
            return NO_MATCH_REPLY;
        }

        log.info("[3] 模型回答: {}", answer);
        return answer;
    }

    /**
     * 目的地推荐(MVP):复用同一套本地检索,临时换 system prompt 输出列表格式。
     */
    public String recommendDestinations(String userNeed) {
        log.info("========== 目的地推荐开始 ==========");

        String result = chatClient.prompt()
                .system(readRecommendPrompt())
                .user(userNeed)
                .advisors(localRagAdvisor)
                .call()
                .content();

        return result == null || result.isBlank() ? "暂无合适的旅游目的地推荐" : result;
    }

    private String readRecommendPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(RECOMMEND_SYSTEM_PROMPT_PATH);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取推荐 Prompt 失败: {}", RECOMMEND_SYSTEM_PROMPT_PATH, e);
            return "你是旅游规划助手,基于上下文推荐 3~5 个目的地,每行格式:- 【城市】:理由";
        }
    }

    private String readSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(SYSTEM_PROMPT_PATH);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取 RAG System Prompt 失败: {}", SYSTEM_PROMPT_PATH, e);
            return "你是一位旅游规划助手,请根据提供的资料回答用户问题,资料没有的内容不要编造。";
        }
    }
}
