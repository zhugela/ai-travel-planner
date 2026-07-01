package com.yupi.aitravelplanner.agent.workflow;

import com.yupi.aitravelplanner.agent.BaseAgent;
import com.yupi.aitravelplanner.agent.enums.AgentState;

import java.util.List;

public class ChainAgent extends BaseAgent {
    private final List<BaseAgent> chain;
    private int currentIndex = 0;
    private String lastOutput = null;

    public ChainAgent(List<BaseAgent> chain) {
        this.chain = chain;
        this.maxSteps = chain.size();
        this.systemPrompt = "ChainAgent: 顺序执行多个子 agent";
    }

    // 覆盖 run：先保存 userInput 到 lastOutput
    @Override
    public String run(String userInput) {
        this.lastOutput = userInput;
        return super.run(userInput);
    }

    @Override
    public String step() {
        // 边界保护
        if (currentIndex >= chain.size()) {
            setState(AgentState.FINISHED);
            return "全部完成";
        }

        BaseAgent current = chain.get(currentIndex);
        // 直接用 lastOutput（第一次就是 userInput）
        lastOutput = current.run(lastOutput);
        currentIndex++;

        if (currentIndex >= chain.size()) {
            setState(AgentState.FINISHED);
        }

        return "第" + currentIndex + "步: "
                + current.getClass().getSimpleName() + " 完成";
    }
}