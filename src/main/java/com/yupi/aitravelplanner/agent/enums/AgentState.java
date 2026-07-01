package com.yupi.aitravelplanner.agent.enums;

public enum AgentState {
    // 空闲，可接收新任务
    IDLE,
    // 正在循环执行任务
    RUNNING,
    // 任务正常全部完成
    FINISHED,
    // 执行过程抛出异常，任务失败
    ERROR
}