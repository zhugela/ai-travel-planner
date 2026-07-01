package com.yupi.aitravelplanner.agent.context;

import com.yupi.aitravelplanner.agent.BaseAgent;
import com.yupi.aitravelplanner.agent.enums.AgentState;

// try-with-resources 友好的状态上下文管理器
// 用法：try (var ctx = StateContext.of(agent, AgentState.RUNNING)) { ... }
// 进入时自动 setState(RUNNING)，离开时自动 setState(IDLE)，若中途异常则 setState(ERROR)
public class StateContext implements AutoCloseable {

    private final BaseAgent agent;
    private final AgentState entryState;
    private boolean closed = false;

    private StateContext(BaseAgent agent, AgentState entryState) {
        this.agent = agent;
        this.entryState = entryState;
        agent.setState(entryState);
    }

    public static StateContext of(BaseAgent agent, AgentState entryState) {
        return new StateContext(agent, entryState);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;                       // ← Bug 1

        AgentState current = agent.getState();
        if (current == AgentState.RUNNING) {  // ← ERROR/FINISHED 跳过
            try {
                agent.setState(AgentState.IDLE);  // ← Bug 2&3: 唯一一次
            } catch (Exception e) {
                // 吞掉，别污染业务异常
            }
        }
    }
}