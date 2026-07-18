package com.yupi.aitravelplanner.agent;

import com.yupi.aitravelplanner.agent.enums.AgentState;

// 1. abstract修饰 + 继承BaseAgent
public abstract class ReActAgent extends BaseAgent {

    // 新增两个抽象方法，子类ToolCallAgent实现
    public abstract boolean think();

    public abstract String act();

    // 重写父类抽象step方法
    @Override
    public String step() {
        try {
            // 固定流程：先思考
            boolean needRunTool = think();
            if (!needRunTool) {
                return "思考完成，无需调用工具";
            }
            // 需要工具则执行act
            return act();
        } catch (Exception e) {
            // 捕获异常，修改状态为ERROR
            setState(AgentState.ERROR);
            return "执行发生异常：" + e.getMessage();
        }
    }
}
