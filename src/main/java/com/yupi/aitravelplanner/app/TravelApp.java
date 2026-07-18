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
     * 注入搜索 API 密钥到 WebSearchTool
     */
    private void injectSearchApiKey() {
        try {
            Field field = WebSearchTool.class.getDeclaredField("apiKey");
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, webSearchTool, searchApiKey);
        } catch (Exception e) {
            log.warn("注入 searchApiKey 失败: {}", e.getMessage());
        }
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
     * 旅行规划结构化报告，返回会话 id 与报告
     */
    public TravelReportResult doChatWithTravelReportAndId(String message, String conversationId) {
        String chatId = resolveConversationId(conversationId);

        String reportSystemPrompt = renderSystemPrompt(extractUserName(message))
                + "每次对话后都要生成旅行规划结果，标题为「XX的旅行报告」形式，内容为行程与出行建议列表。";

        TravelReport report = chatClient.prompt()
                .system(reportSystemPrompt)
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(TravelReport.class);

        log.info("travelReport: {}", report);

        return new TravelReportResult(chatId, report);
    }

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
     * 旅行规划报告（结构化 JSON 输出）
     */
    public record TravelReport(String title, List<String> suggestions) {
    }

    public record TravelReportResult(String conversationId, TravelReport report) {
    }
}
