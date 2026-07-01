package com.yupi.aitravelplanner.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TerminateTool {

    @Tool(description = "结束任务，输出最终结果")
    public String finishTask(
            @ToolParam(description = "最终输出内容") String result
    ) {
        return result;
    }
}
