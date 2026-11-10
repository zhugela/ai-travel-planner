package com.yupi.aitravelplanner.app;

import com.yupi.aitravelplanner.advisor.MyLoggerAdvisor;
import com.yupi.aitravelplanner.chatmemory.FileBasedChatMemory;
import com.yupi.aitravelplanner.constant.FileConstant;
import com.yupi.aitravelplanner.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 旅行规划应用（对应课程 LoveApp）
 * <p>
 * 封装 ChatClient + System Prompt + 多轮对话记忆 + 工具调用
 */
@Slf4j
@Component
public class TravelApp {

    private static final Pattern USER_NAME_PATTERN = Pattern.compile("我(?:叫|是)([^，,。！!\s]+)");

    private final SystemPromptTemplate systemPromptTemplate;
    private final String systemPrompt;
    private final ChatClient chatClient;

    // 工具实例
    private final FileOperationTool fileOperationTool;
    private final WebSearchTool webSearchTool;
    private final WebScrapingTool webScrapingTool;
    private final TerminalTool terminalTool;
    private final DownloadTool downloadTool;
    private final PdfGenerationTool pdfGenerationTool;

    // MCP 工具提供者（可选，没有 MCP 服务时为空）
    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    public TravelApp(
            ChatModel dashscopeChatModel,
            FileOperationTool fileOperationTool,
            WebSearchTool webSearchTool,
            WebScrapingTool webScrapingTool,
            TerminalTool terminalTool,
            DownloadTool downloadTool,
            PdfGenerationTool pdfGenerationTool,
            // 注入 MCP 工具提供者（可选，没有 MCP 服务时为空）
            @Autowired(required = false) SyncMcpToolCallbackProvider mcpToolCallbackProvider
    ) {
        // 保存工具实例
        this.fileOperationTool = fileOperationTool;
        this.webSearchTool = webSearchTool;
        this.webScrapingTool = webScrapingTool;
        this.terminalTool = terminalTool;
        this.downloadTool = downloadTool;
        this.pdfGenerationTool = pdfGenerationTool;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;

        // 注入 API 密钥
        injectSearchApiKey();

        this.systemPromptTemplate = new SystemPromptTemplate(
                new ClassPathResource(FileConstant.TRAVEL_SYSTEM_PROMPT_PATH));
        this.systemPrompt = renderSystemPrompt("旅行者");

        String chatMemoryDir = System.getProperty("user.dir") + "/" + FileConstant.CHAT_MEMORY_DIR;
        ChatMemory chatMemory = new FileBasedChatMemory(chatMemoryDir);

        // 构建工具列表（POJO 工具对象）
        Object[] tools = new Object[]{
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                terminalTool,
                downloadTool,
                pdfGenerationTool
        };

        // 创建 ChatClient，注册系统提示、对话记忆和工具
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                // 注册 POJO 工具到 ChatClient
                .defaultTools(tools);

        // 如果有 MCP 工具，通过 defaultToolCallbacks 注册
        if (mcpToolCallbackProvider != null) {
            builder.defaultToolCallbacks(mcpToolCallbackProvider);
            log.info("已接入 MCP 工具");
        }

        this.chatClient = builder.build();

        log.info("TravelApp 初始化完成，已注册 {} 个工具", tools.length);
    }

    /**
     * 工具已通过 @Value 自行注入 search-api.api-key,这里无需再反射赋值。
     * (旧实现由 TravelApp 反射写 WebSearchTool.apiKey,会与 @Value 抢时序)
     */
    private void injectSearchApiKey() {
        // 保留方法占位,避免改动构造器调用链;实际注入由 @Value 完成
    }

    /**
     * 获取已注册的工具列表（用于调试）
     */
    public String getToolList() {
        return "已注册工具: FileOperationTool, WebSearchTool, WebScrapingTool, " +
                "TerminalTool, DownloadTool, PdfGenerationTool";
    }

    /**
     * 多轮对话
     *
     * @param message        用户消息
     * @param conversationId 会话 id，为空则自动生成
     * @return AI 回复文本
     */
    public String doChat(String message, String conversationId) {
        String chatId = resolveConversationId(conversationId);

        return chatClient.prompt()
                .system(renderSystemPrompt(extractUserName(message)))
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }

    /**
     * 多轮对话，返回会话 id 与回复
     */
    public ChatResult doChatWithId(String message, String conversationId) {
        String chatId = resolveConversationId(conversationId);

        String reply = chatClient.prompt()
                .system(renderSystemPrompt(extractUserName(message)))
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();

        return new ChatResult(chatId, reply);
    }

    /**
     * 旅行规划结构化报告（对应课程 doChatWithReport / LoveReport）
     */
    public TravelReport doChatWithTravelReport(String message, String conversationId) {
        return doChatWithTravelReportAndId(message, conversationId).report();
    }

    /**
     * 旅行规划结构化报告。
     *
     * 注意:这里不能复用 travel-system.st 的对话式 prompt —— 那份明确要求"信息不全时只能追问",
     * 会让报告页把追问当成建议输出。报告场景改为独立 prompt,一次给出可落地的分天行程,
     * 缺失信息用合理的默认值补齐并在 assumptions 里标注。
     */
    public TravelReportResult doChatWithTravelReportAndId(String message, String conversationId) {
        String chatId = resolveConversationId(conversationId);

        TravelReport report = chatClient.prompt()
                .system(REPORT_SYSTEM_PROMPT + "\n当前日期：" + LocalDate.now())
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelReport.class);

        log.info("travelReport: {}", report);

        return new TravelReportResult(chatId, report);
    }

    /** 报告场景专用 prompt:直接产出行程,而不是反问用户 */
    private static final String REPORT_SYSTEM_PROMPT = """
            你是一位自由行行程规划师。用户会给出旅行需求,你要直接产出一份可执行的行程方案。

            【硬性要求】
            1. 不要反问用户,不要输出"还需要确认什么"之类的追问。用户给的信息不足以精确规划时,
               自行采用合理的默认值(如默认 2 人、默认中等预算、默认公共交通),并在 assumptions 中标注。
            2. days 必须覆盖用户要求的天数,不能少也不能多;若用户没写天数,按 3 天规划。
            3. 每天的 morning/afternoon/evening 必须是具体安排(景点/交通/餐饮/住宿),不能写"自由活动""根据喜好选择"。
            4. 每个安排后面用括号标注预估花费,如"(门票 75 元)""(人均 80 元)"。
            5. budget 按交通/住宿/餐饮/门票四项拆分,总额要接近用户预算;若超出预算必须调低档次而不是无视预算。
            6. tips 写 3-5 条真正的注意事项(预约、天气、交通时段、体力分配等),不要写客套话。

            【字段说明】
            - title:行程标题,如「成都 5 天慢节奏亲子游」
            - summary:一句话概述,说明行程主线与适配人群
            - days:每天的安排,day 从 1 开始递增
            - budget:预算拆分,含 total 与备注
            - assumptions:你为缺失信息补的默认值,没有则给空数组
            - tips:注意事项列表
            """;

    private String renderSystemPrompt(String userName) {
        return systemPromptTemplate.createMessage(Map.of(
                "today", LocalDate.now().toString(),
                "userName", userName
        )).getText();
    }

    private String extractUserName(String message) {
        if (!StringUtils.hasText(message)) {
            return "旅行者";
        }

        Matcher matcher = USER_NAME_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1).trim() : "旅行者";
    }

    private String resolveConversationId(String conversationId) {
        return StringUtils.hasText(conversationId) ? conversationId : UUID.randomUUID().toString();
    }

    /**
     * 流式多轮对话(Ch 09 §2.3 实现)。
     * 返回 Flux<String> 而不是 ChatResponse,减少传输体积,便于前段逐字渲染打字机效果。
     * 复用 doChat 的 advisor 链(记忆 + 日志),保证多轮上下文不丢。
     */
    public Flux<String> doChatByStream(String message, String conversationId) {
        String chatId = resolveConversationId(conversationId);

        return chatClient.prompt()
                .system(renderSystemPrompt(extractUserName(message)))
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    public record ChatResult(String conversationId, String reply) {
    }

    /**
     * 旅行报告(结构化 JSON 输出)
     */
    public record TravelReport(
            String title,
            String summary,
            List<DayPlan> days,
            Budget budget,
            List<String> assumptions,
            List<String> tips
    ) {
    }

    /** 单日行程 */
    public record DayPlan(
            int day,
            String theme,
            String morning,
            String afternoon,
            String evening
    ) {
    }

    /** 预算拆分(单位:元) */
    public record Budget(
            double total,
            double transport,
            double hotel,
            double food,
            double tickets,
            String note
    ) {
    }

    public record TravelReportResult(String conversationId, TravelReport report) {
    }
}
