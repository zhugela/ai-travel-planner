package com.yupi.aitravelplanner.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 终止工具：任务全部完成后调用，通知智能体结束循环
 */
@Component
public class TerminateTool {

    /**
     * 告知模型：当旅游规划全部信息收集齐全、方案完整后，必须调用本工具结束执行
     * @return 任务完成提示文本
     */
    @Tool(description = "旅游行程规划全部完成，所有信息已收集完毕，调用该工具结束智能体循环推理，不再继续查询工具")
    public String finishTask() {
        return "任务规划全部完成，停止所有工具调用与推理循环，生成最终完整旅游方案";
    }
}