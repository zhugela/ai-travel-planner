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
    private final LoopGuard guard = new LoopGuard();

    // 构造器
    public ToolCallAgent(ChatModel chatModel, ToolCallback[] toolCallbacks) {
        this.chatModel = chatModel;
        this.toolCallbacks = toolCallbacks;
    }

    @Override
    public boolean think() {
        // 1. 获取父类消息
        List<Message> historyMsgList = new ArrayList<>(getMessageList());

        // 2. 系统提示词（读子类字段，不硬编码）
        SystemMessage systemMessage = new SystemMessage(this.systemPrompt);

        // 3. 组装完整消息列表
        List<Message> fullMessages = new ArrayList<>();
        fullMessages.add(systemMessage);
        fullMessages.addAll(historyMsgList);

        // 4. 构造 Prompt(必须把 toolCallbacks 传给 ChatOptions,否则模型看不到工具)
        ChatOptions options = DefaultToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .toolCallbacks(this.toolCallbacks)
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

        // 7. 把AI消息存入历史
        getMessageList().add(new AssistantMessage(assistantOutput.getText()));

        // 8. 判断是否需要工具
        boolean needTool = assistantOutput.getToolCalls() != null
                && !assistantOutput.getToolCalls().isEmpty();

        // 9. 死循环检测：告诉 guard 这次 think 是否调用工具
        guard.recordThink(needTool);

        return needTool;
    }

    @Override
    public String act() {
        StringBuilder result = new StringBuilder();

        AssistantMessage assistantMsg = toolCallChatResponse.getResult().getOutput();

        assistantMsg.getToolCalls().forEach(toolCall -> {
            String toolName = toolCall.name();
            String args = toolCall.arguments();

            // 死循环检测：每次执行工具前记录
            guard.recordTool(toolName);

            String toolResult = executeTool(toolName, args);
            result.append("【工具执行】").append(toolName).append(" => ").append(toolResult).append("\n");

            if ("finishTask".equals(toolName)) {
                setState(AgentState.FINISHED);
            }
        });

        return result.toString();
    }

    private String executeTool(String name, String args) {
        for (ToolCallback callback : toolCallbacks) {
            if (callback.getToolDefinition().name().equals(name)) {
                return callback.call(args);
            }
        }
        return "未找到工具：" + name;
    }
}