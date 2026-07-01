package com.yupi.aitravelplanner.agent;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 成品智能体：旅行规划 Agent
 * 继承 ToolCallAgent 四层链路末尾，开箱即用
 */
@Component
public class YuTravelAgent extends ToolCallAgent {

    public YuTravelAgent(ChatModel chatModel, List<ToolCallback> toolCallbacks) {
        super(chatModel, toolCallbacks.toArray(new ToolCallback[0]));

        // 初始化消息记忆（避免 NPE）
        this.messageList = new ArrayList<>();

        // 最大循环步数
        this.maxSteps = 20;

        // 系统提示词（规则由你填写）
        this.systemPrompt = """
                """;
    }
}
