package com.yupi.aitravelplanner.agent;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 成品智能体：旅行规划 Agent
 * 继承 ToolCallAgent 四层链路末尾，开箱即用SpringBean
 */
@Component
public class YuTravelAgent extends ToolCallAgent {

    public YuTravelAgent(ChatModel chatModel, List<ToolCallback> toolCallbacks) {
        // 调用父类ToolCallAgent构造器，注入模型+全部工具
        super(chatModel, toolCallbacks.toArray(new ToolCallback[0]));

        // 操作父类BaseAgent受保护字段，不要重复定义字段！
        super.messageList = new ArrayList<>();
        super.maxSteps = 20;

        // 极简省Token 系统提示词,强制工具约束 + 防重复调
        super.systemPrompt = """
                角色：旅行规划Agent
                规则：
                1. 景点、天气仅允许调用工具获取，禁止编造；
                2. 数据齐全后调用TerminateTool输出完整行程；
                3. 未收集完全信息不能直接输出最终方案；
                4. 同一工具调用一次即可，不要重复调用（已拿到结果就基于结果回答）；
                5. 信息足够时立即调用TerminateTool结束任务，不要无意义思考。
                """;
    }

    // 禁止新增自定义call/executeStep，统一使用父类入口 run(String userInput)
}