package com.yupi.aitravelplanner.service;

// Spring AI - ChatModel:对话模型接口(由 spring-ai-alibaba-starter-dashscope 自动注入)
import org.springframework.ai.chat.model.ChatModel;

// Spring AI - ChatClient:高层 API,Fluent 风格,本类自己构造
import org.springframework.ai.chat.client.ChatClient;

// Spring AI - RAG 检索增强拦截器(由 spring-ai-advisors-vector-store 提供)
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;

// Spring AI - 文档切片(检索结果就是 List<Document>)
import org.springframework.ai.document.Document;

// Spring AI - 检索参数(替代已废弃的 similaritySearch(String))
import org.springframework.ai.vectorstore.SearchRequest;

// Spring AI - 向量库通用接口(由 TravelVectorStoreConfig 注入)
import org.springframework.ai.vectorstore.VectorStore;

// Spring - 资源加载(读 prompts/travel-rag-system.st)
import org.springframework.core.io.ClassPathResource;

// Spring - 服务注解
import org.springframework.stereotype.Service;

// Lombok - 自动生成 log 字段
import lombok.extern.slf4j.Slf4j;

// JDK - IO
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 旅游知识库 RAG 问答服务
 *
 * 流程:用户提问 → 内存向量库检索 → 把命中片段塞进 user 消息 → 调通义千问 → 兜底回复
 * 兜底:检索为空(没有命中)→ 直接返回固定话术,不调模型
 */
@Slf4j
@Service
public class TravelRagChatService {

    /** 检索条数上限(命中 TopK 条最相关的文档) */
    private static final int TOP_K = 3;

    /** 相似度阈值(0.0~1.0,越高越严格);0.5 是个经验值,旅游场景够用 */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /** 知识库无匹配时的兜底回复 */
    public static final String NO_MATCH_REPLY = "暂无相关旅游攻略信息";

    /** RAG 专用 System Prompt 文件位置 */
    private static final String SYSTEM_PROMPT_PATH = "prompts/travel-rag-system.st";

    /** ChatClient(本类自己构造,见构造器) */
    private final ChatClient chatClient;

    /** 向量库 Bean(由 TravelVectorStoreConfig 注册) */
    private final VectorStore travelVectorStore;

    /**
     * 构造注入 ChatModel + VectorStore
     * Spring 自动装配:
     *   - ChatModel:阿里 starter 自动创建的 DashScope ChatModel
     *   - travelVectorStore:TravelVectorStoreConfig 里的 @Bean 方法返回的 VectorStore
     */
    public TravelRagChatService(ChatModel dashscopeChatModel, VectorStore travelVectorStore) {
        this.travelVectorStore = travelVectorStore;

        // 读 system prompt 模板(用 UTF-8 编码)
        String systemPrompt = readSystemPrompt();

        // 构造 ChatClient:注入 System Prompt + QuestionAnswerAdvisor
        // QuestionAnswerAdvisor 内部会:
        //   1) 拿 user 的问题去查向量库(SearchRequest 默认 topK=4, threshold=0)
        //   2) 把命中片段塞进 user 消息
        //   3) 调 ChatModel
        // 我们通过提前预检 + 调小 TOP_K,实际控制检索行为
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(travelVectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(TOP_K)
                                        .similarityThreshold(SIMILARITY_THRESHOLD)
                                        .build())
                                .build()
                )
                .build();
    }

    /**
     * RAG 对话统一入口
     * @param userMessage 用户提问
     * @return AI 基于知识库的回答,或兜底话术
     */
    public String chatWithRag(String userMessage) {
        log.info("========== RAG 请求开始 ==========");
        log.info("[1] 用户提问: {}", userMessage);

        // 1) 先去向量库检索(预检)
        //    检索参数:同样的 topK + threshold
        //    注意:.query() 在 SearchRequest 里就是问句本身
        List<Document> retrievedDocs = travelVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build()
        );

        // 2) 兜底:没命中任何文档,直接返回固定话术,不调模型
        if (retrievedDocs == null || retrievedDocs.isEmpty()) {
            log.warn("[2] 知识库无匹配片段(阈值={}, TopK={}),触发兜底回复",
                    SIMILARITY_THRESHOLD, TOP_K);
            log.info("========== RAG 请求结束(兜底) ==========");
            return NO_MATCH_REPLY;
        }

        // 3) 命中:打印检索片段(供调试)
        log.info("[2] 检索到 {} 条相关知识库片段:", retrievedDocs.size());
        for (int i = 0; i < retrievedDocs.size(); i++) {
            Document doc = retrievedDocs.get(i);
            // 截取前 80 字符,避免日志爆炸
            String preview = doc.getText() == null ? "(空)" : doc.getText().substring(0, Math.min(80, doc.getText().length()));
            String docType = String.valueOf(doc.getMetadata().get("doc_type"));
            log.info("    片段[{}] doc_type={} | 内容预览: {}...",
                    i + 1, docType, preview.replace("\n", " "));
        }

        // 4) 把命中片段拼成 user 消息的一段
        //    QuestionAnswerAdvisor 内部会自动再查一次 + 塞入 user,
        //    但这里我们也手工拼一份,让日志能看到模型拿到的真实输入
        String contextBlock = retrievedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 5) 调 ChatClient 发请求
        //    .user() 里拼 "知识库上下文 + 用户问题",System Prompt 已经约束模型行为
        String answer = chatClient.prompt()
                .user(userSpec -> userSpec
                        .text("""
                                【知识库上下文】
                                %s

                                【用户问题】
                                %s
                                """.formatted(contextBlock, userMessage)))
                .call()
                .content();

        // 6) 打印模型输出
        log.info("[3] 模型回答: {}", answer);
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
