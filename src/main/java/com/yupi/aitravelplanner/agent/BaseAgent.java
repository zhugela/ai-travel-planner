package com.yupi.aitravelplanner.agent;

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
        this.setState(AgentState.RUNNING);

        // 存入用户消息
        org.springframework.ai.chat.messages.UserMessage userMsg =
            new org.springframework.ai.chat.messages.UserMessage(userInput);
        messageList.add(userMsg);

        int currentStep = 0;
        StringBuilder allResult = new StringBuilder();

        while (getState() == AgentState.RUNNING && currentStep < maxSteps) {
            currentStep++;
            String singleResult = step();
            // 补全步骤拼接
            allResult.append("====第").append(currentStep).append("步====\n");
            allResult.append(singleResult).append("\n");
            // 如果子类把状态改成 FINISHED/ERROR，立刻跳出循环，不用跑完最大步数
            if (getState() != AgentState.RUNNING) {
                break;
            }
        }

        cleanup();
        // 重置空闲
        this.setState(AgentState.IDLE);
        return allResult.toString();
    }

    // 资源释放钩子，子类按需重写
    public void cleanup() {

    }
}