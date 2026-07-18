package com.yupi.aitravelplanner.controller;

import com.yupi.aitravelplanner.agent.YuTravelAgent;
import com.yupi.aitravelplanner.tools.DownloadTool;
import com.yupi.aitravelplanner.tools.FileOperationTool;
import com.yupi.aitravelplanner.tools.PdfGenerationTool;
import com.yupi.aitravelplanner.tools.TerminalTool;
import com.yupi.aitravelplanner.tools.WeatherTool;
import com.yupi.aitravelplanner.tools.WebScrapingTool;
import com.yupi.aitravelplanner.tools.WebSearchTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 智能体 SSE 流式接口(Ch 09 §3)。
 * 每次请求新建 YuTravelAgent,隔离 messageList,避免多用户串话。
 *
 * 注意:Spring AI 1.0.0 没有公开 ToolCallbacks.from() 工具类,
 * 这里手动用 MethodToolCallback.Builder + 反射遍历 @Tool 注解构造。
 *
 * Ch 07 MCP 注入:可选注入 SyncMcpToolCallbackProvider(远程 MCP 服务器的工具),
 * 如果 application.yml 配了 spring.ai.mcp.client.* 配置,会自动注入。
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@Tag(name = "AI 旅行规划智能体")
public class AgentController {

    private final ChatModel chatModel;
    /** MCP 远程工具提供者(可选注入,application.yml 没配 MCP 时为 null) */
    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    @Resource private FileOperationTool fileOperationTool;
    @Resource private WebSearchTool webSearchTool;
    @Resource private WebScrapingTool webScrapingTool;
    @Resource private TerminalTool terminalTool;
    @Resource private DownloadTool downloadTool;
    @Resource private PdfGenerationTool pdfGenerationTool;
    @Resource private WeatherTool weatherTool;

    public AgentController(ChatModel chatModel,
                           @Autowired(required = false) SyncMcpToolCallbackProvider mcpToolCallbackProvider) {
        this.chatModel = chatModel;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        log.info("[AgentController] 初始化完成, MCP 远程工具提供者 = {}",
                mcpToolCallbackProvider == null ? "未注入" : "已注入");
    }

    @GetMapping(value = "/sse", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "旅行规划智能体(SSE 流式)")
    public SseEmitter doAgentStream(@RequestParam String message) {
        YuTravelAgent agent = new YuTravelAgent(chatModel, buildToolCallbacks());
        return agent.runStream(message);
    }

    /**
     * 把 7 个 Spring Bean 工具的 @Tool 方法 + 远程 MCP 工具合并成 List<ToolCallback>。
     * Spring AI 1.0.0 没公开 ToolCallbacks.from(),这里手写反射版。
     */
    private List<ToolCallback> buildToolCallbacks() {
        List<ToolCallback> result = new ArrayList<>();

        // 1. 本地 POJO 工具(7 个 @Tool)
        Object[] tools = {
                fileOperationTool, webSearchTool, webScrapingTool,
                terminalTool, downloadTool, pdfGenerationTool, weatherTool
        };
        for (Object tool : tools) {
            for (Method method : tool.getClass().getMethods()) {
                Tool toolAnno = method.getAnnotation(Tool.class);
                if (toolAnno == null) continue;
                ToolDefinition def = ToolDefinition.builder()
                        .name(toolAnno.name().isEmpty() ? method.getName() : toolAnno.name())
                        .description(toolAnno.description())
                        .inputSchema(buildInputSchema(method))
                        .build();
                MethodToolCallback cb = MethodToolCallback.builder()
                        .toolDefinition(def)
                        .toolMethod(method)
                        .toolObject(tool)
                        .build();
                result.add(cb);
            }
        }

        // 2. 远程 MCP 工具(可选)
        if (mcpToolCallbackProvider != null) {
            ToolCallback[] mcpCallbacks = mcpToolCallbackProvider.getToolCallbacks();
            if (mcpCallbacks != null && mcpCallbacks.length > 0) {
                log.info("[AgentController] 合并 {} 个 MCP 远程工具", mcpCallbacks.length);
                for (ToolCallback cb : mcpCallbacks) result.add(cb);
            } else {
                log.info("[AgentController] MCP 提供者存在但无回调");
            }
        }

        log.info("[AgentController] 最终工具数量 = {}", result.size());
        return result;
    }

    /** 简化版 inputSchema:只描述参数名+类型,Spring AI 实际用更复杂的 JSON Schema */
    private String buildInputSchema(Method method) {
        StringBuilder sb = new StringBuilder("{\"type\":\"object\",\"properties\":{");
        Class<?>[] params = method.getParameterTypes();
        java.lang.reflect.Parameter[] pArr = method.getParameters();
        for (int i = 0; i < pArr.length; i++) {
            ToolParam tp = pArr[i].getAnnotation(ToolParam.class);
            if (tp != null) {
                sb.append("\"").append(pArr[i].getName()).append("\":{\"type\":\"string\",\"description\":\"")
                        .append(tp.description()).append("\"}");
                if (i < pArr.length - 1) sb.append(",");
            }
        }
        sb.append("}}");
        return sb.toString();
    }
}