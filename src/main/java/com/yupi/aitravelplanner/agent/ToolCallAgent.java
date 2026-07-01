package com.yupi.aitravelplanner.agent;

import com.yupi.aitravelplanner.agent.enums.AgentState;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;

public class ToolCallAgent extends ReActAgent {

    // 字段
    private final ChatModel chatModel;
    private final ToolCallback[] toolCallbacks;
    private ChatResponse toolCallChatResponse;

    // 构造器
    public ToolCallAgent(ChatModel chatModel, ToolCallback[] toolCallbacks) {
        this.chatModel = chatModel;
        this.toolCallbacks = toolCallbacks;
    }

    @Override
    public boolean think() {
        // 1. 获取父类消息（BaseAgent.messageList 是 List<Message>，多态处理）
        List<Message> historyMsgList = new ArrayList<>(getMessageList());

        // 2. 系统提示词
        String systemPromptText = """
            你是智能旅行规划Agent，严格遵循规则：
            1. 仅可使用提供的工具获取数据，禁止编造信息；
            2. 需要查询景点/天气时调用对应工具；
            3. 全部信息收集完成后调用 finishTask 工具输出最终方案。
            """;
        SystemMessage systemMessage = new SystemMessage(systemPromptText);

        // 3. 组装完整消息列表
        List<Message> fullMessages = new ArrayList<>();
        fullMessages.add(systemMessage);
        fullMessages.addAll(historyMsgList);

        // 4. 构造 Prompt（Spring AI 1.0.0 正确 API）
        ChatOptions options = DefaultToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)   // 关键：手动管控工具执行（不用 withProxyToolCalls）
                .build();
        Prompt prompt = Prompt.builder()
                .messages(fullMessages)
                .chatOptions(options)
                .build();

        // 5. 调用大模型
        ChatResponse response = chatModel.call(prompt);
        this.toolCallChatResponse = response;

        // 6. 获取AI返回消息
        AssistantMessage assistantOutput = response.getResult().getOutput();

        // 7. 把AI消息存入历史（类型必须 AssistantMessage，不是 UserMessage）
        getMessageList().add(new AssistantMessage(assistantOutput.getText()));

        // 8. 返回是否需要调用工具
        return assistantOutput.getToolCalls() != null && !assistantOutput.getToolCalls().isEmpty();
    }

    @Override
    public String act() {
        StringBuilder result = new StringBuilder();

        // 1. 获取工具调用
        AssistantMessage assistantMsg = toolCallChatResponse.getResult().getOutput();

        // 2. 遍历执行（Spring AI 1.0.0 ToolCall 是 record，方法不带 get 前缀）
        assistantMsg.getToolCalls().forEach(toolCall -> {
            String toolName = toolCall.name();      // record style（不是 getName()）
            String args = toolCall.arguments();      // record style（不是 getArguments()）

            // 执行工具
            String toolResult = executeTool(toolName, args);
            result.append("【工具执行】").append(toolName).append(" => ").append(toolResult).append("\n");

            // 如果是 finishTask，结束任务
            if ("finishTask".equals(toolName)) {
                setState(AgentState.FINISHED);
            }
        });

        return result.toString();
    }

    // 工具执行方法（Spring AI 1.0.0 ToolDefinition 是 interface，方法不带 get 前缀）
    private String executeTool(String name, String args) {
        for (ToolCallback callback : toolCallbacks) {
            if (callback.getToolDefinition().name().equals(name)) {  // record/interface 风格，无 get 前缀
                return callback.call(args);
            }
        }
        return "未找到工具：" + name;
    }
}