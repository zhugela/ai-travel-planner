package com.yupi.aitravelplanner.agent;

import com.yupi.aitravelplanner.agent.context.StateContext;
import com.yupi.aitravelplanner.agent.enums.AgentState;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Getter
@Setter
public abstract class BaseAgent {
    // 字段
    protected AgentState state = AgentState.IDLE;
    protected String systemPrompt;
    protected List<Message> messageList;
    protected int maxSteps = 10;

    // 抽象方法，子类必须实现
    public abstract String step();

    // 获取状态
    public AgentState getState() {
        return this.state;
    }

    // 修改状态
    public void setState(AgentState state) {
        this.state = state;
    }

    // 主运行入口
    public String run(String userInput) {
        // 1. 忙碌拦截
        if (this.getState() != AgentState.IDLE) {
            return "智能体正在执行任务，请勿重复调用";
        }

        // 2. 存入用户消息
        org.springframework.ai.chat.messages.UserMessage userMsg =
            new org.springframework.ai.chat.messages.UserMessage(userInput);
        messageList.add(userMsg);

        // 3. 死循环检测器（每任务新建）
        LoopGuard guard = new LoopGuard();
        int currentStep = 0;
        StringBuilder allResult = new StringBuilder();
        String loopReason = null;

        // 4. 用 StateContext 自动管状态
        try (StateContext ctx = StateContext.of(this, AgentState.RUNNING)) {
            while (getState() == AgentState.RUNNING && currentStep < maxSteps) {
                currentStep++;
                String singleResult = step();
                allResult.append("====第").append(currentStep).append("步====\n");
                allResult.append(singleResult).append("\n");
                if (getState() != AgentState.RUNNING) {
                    break;
                }
                // 死循环检测
                if (guard.isLooping()) {
                    loopReason = "检测到死循环（重复工具或无进展），强制终止";
                    setState(AgentState.ERROR);
                    break;
                }
            }
            cleanup();
        }   // close() 自动判断：RUNNING→IDLE，ERROR/FINISHED 保持

        if (loopReason != null) {
            allResult.append("\n[LoopGuard] ").append(loopReason);
        }
        return allResult.toString();
    }

    // 资源释放钩子，子类按需重写
    public void cleanup() {

    }
}