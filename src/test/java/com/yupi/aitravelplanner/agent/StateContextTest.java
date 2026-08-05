package com.yupi.aitravelplanner.agent;

import com.yupi.aitravelplanner.agent.context.StateContext;
import com.yupi.aitravelplanner.agent.enums.AgentState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StateContext 单元测试 —— Agent 状态机（纯逻辑，不依赖 Spring / AI API）
 */
@DisplayName("StateContext 状态上下文")
class StateContextTest {

    /** BaseAgent 的最小 stub，只用于测试状态机 */
    static class StubAgent extends BaseAgent {
        @Override public String step() { return "stub"; }
    }

    // ======================== 正常流程 ========================

    @Test
    @DisplayName("进入 → IDLE→RUNNING, 离开 → RUNNING→IDLE")
    void normalFlow_idleToRunningAndBack() {
        StubAgent agent = new StubAgent();
        assertThat(agent.getState()).isEqualTo(AgentState.IDLE);

        try (StateContext ctx = StateContext.of(agent, AgentState.RUNNING)) {
            assertThat(agent.getState()).isEqualTo(AgentState.RUNNING);
        }

        assertThat(agent.getState()).isEqualTo(AgentState.IDLE);
    }

    @Test
    @DisplayName("多次 try-with-resources 正确管理状态")
    void multipleEntries_stateManagedCorrectly() {
        StubAgent agent = new StubAgent();

        try (StateContext ctx = StateContext.of(agent, AgentState.RUNNING)) {
            assertThat(agent.getState()).isEqualTo(AgentState.RUNNING);
        }
        assertThat(agent.getState()).isEqualTo(AgentState.IDLE);

        try (StateContext ctx = StateContext.of(agent, AgentState.RUNNING)) {
            assertThat(agent.getState()).isEqualTo(AgentState.RUNNING);
        }
        assertThat(agent.getState()).isEqualTo(AgentState.IDLE);
    }

    // ======================== 异常流程 ========================

    @Test
    @DisplayName("中途抛异常 → close() 仍执行 → 从 RUNNING 恢复为 IDLE")
    void exceptionDuringBlock_restoresToIdle() {
        StubAgent agent = new StubAgent();

        try (StateContext ctx = StateContext.of(agent, AgentState.RUNNING)) {
            agent.setState(AgentState.RUNNING); // 模拟正常 state
            throw new RuntimeException("模拟业务异常");
        } catch (RuntimeException e) {
            // 预期捕获
        }

        // 关键: close() 在 catch 之前已执行,状态应恢复
        assertThat(agent.getState()).isEqualTo(AgentState.IDLE);
    }

    @Test
    @DisplayName("中途被设为 ERROR → close() 不覆盖 ERROR 状态")
    void erroredState_preservedAfterClose() {
        StubAgent agent = new StubAgent();

        try (StateContext ctx = StateContext.of(agent, AgentState.RUNNING)) {
            agent.setState(AgentState.ERROR);  // 模拟异常终止
        }

        // ERROR 状态不应被 close() 覆盖为 IDLE
        assertThat(agent.getState()).isEqualTo(AgentState.ERROR);
    }

    @Test
    @DisplayName("中途被设为 FINISHED → close() 不覆盖 FINISHED")
    void finishedState_preservedAfterClose() {
        StubAgent agent = new StubAgent();

        try (StateContext ctx = StateContext.of(agent, AgentState.RUNNING)) {
            agent.setState(AgentState.FINISHED);  // 模拟正常完成
        }

        assertThat(agent.getState()).isEqualTo(AgentState.FINISHED);
    }

    // ======================== 入口状态 ========================

    @Test
    @DisplayName("自定义入口状态 → 正确设置")
    void customEntryState() {
        StubAgent agent = new StubAgent();
        agent.setState(AgentState.IDLE);

        try (StateContext ctx = StateContext.of(agent, AgentState.RUNNING)) {
            assertThat(agent.getState()).isEqualTo(AgentState.RUNNING);
        }
    }

    // ======================== 边界条件 ========================

    @Test
    @DisplayName("重复关闭 → 不抛出异常（幂等）")
    void doubleClose_noException() {
        StubAgent agent = new StubAgent();
        StateContext ctx = StateContext.of(agent, AgentState.RUNNING);
        ctx.close();
        ctx.close();  // 第二次 close 应安全跳过
        // 不抛异常即通过
        assertThat(agent.getState()).isEqualTo(AgentState.IDLE);
    }
}
