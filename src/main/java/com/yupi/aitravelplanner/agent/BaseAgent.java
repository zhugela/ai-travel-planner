package com.yupi.aitravelplanner.agent;

import com.yupi.aitravelplanner.agent.context.StateContext;
import com.yupi.aitravelplanner.agent.enums.AgentState;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
@Getter
@Setter
@Slf4j
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

    /**
     * 流式运行入口（Ch 09 §3.2 实现）。
     * 关键:用 CompletableFuture.runAsync 把 Agent 循环丢到独立线程池,
     * 不占 Tomcat 线程;每步通过 SseEmitter 推送给前端。
     */
    public SseEmitter runStream(String userInput) {
        // 5 分钟超时,适配 Agent 多步推理最长时间
        SseEmitter emitter = new SseEmitter(300_000L);

        // 1. 同步段:立刻把用户消息存进去(避免异步段看到空消息)
        if (this.getState() != AgentState.IDLE) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("智能体正在执行任务,请勿重复调用"));
                emitter.complete();
                return emitter;
            } catch (IOException e) {
                emitter.completeWithError(e);
                return emitter;
            }
        }
        messageList.add(new org.springframework.ai.chat.messages.UserMessage(userInput));

        // 2. 异步段:丢后台线程池跑循环(不占 Tomcat)
        CompletableFuture.runAsync(() -> {
            LoopGuard guard = new LoopGuard();
            int currentStep = 0;
            try (StateContext ctx = StateContext.of(this, AgentState.RUNNING)) {
                while (getState() == AgentState.RUNNING && currentStep < maxSteps) {
                    currentStep++;
                    String stepResult = step();
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data("Step " + currentStep + ":\n" + stepResult + "\n"));
                    if (getState() != AgentState.RUNNING) {
                        break;
                    }
                    if (guard.isLooping()) {
                        emitter.send(SseEmitter.event()
                                .name("loop")
                                .data("[LoopGuard] 检测到死循环,强制终止"));
                        setState(AgentState.ERROR);
                        break;
                    }
                }
                cleanup();
            } catch (Exception e) {
                log.error("Agent runStream 异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("执行异常:" + e.getMessage()));
                } catch (IOException ignored) {
                }
                setState(AgentState.ERROR);
            } finally {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        });

        // 3. 3 个回调:超时 / 完成 / 异常 → 防止 Tomcat 持有死句柄
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            setState(AgentState.ERROR);
            emitter.complete();
        });
        emitter.onCompletion(() -> log.debug("SSE 连接正常关闭"));
        emitter.onError(e -> {
            log.warn("SSE 连接出错: {}", e.getMessage());
            setState(AgentState.ERROR);
        });

        return emitter;
    }
}