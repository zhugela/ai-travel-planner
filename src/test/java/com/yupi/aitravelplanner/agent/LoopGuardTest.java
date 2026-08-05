package com.yupi.aitravelplanner.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoopGuard 单元测试 —— 死循环检测器（纯逻辑，不依赖 Spring / AI API）
 */
@DisplayName("LoopGuard 死循环检测")
class LoopGuardTest {

    // ======================== 重复工具检测 ========================

    @Test
    @DisplayName("连续3次相同工具 → 检测到死循环")
    void recordTool_3sameTool_detectsLoop() {
        LoopGuard guard = new LoopGuard();
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");
        assertThat(guard.isLooping()).isTrue();
    }

    @Test
    @DisplayName("连续2次相同工具 → 未达阈值，不触发")
    void recordTool_2sameTool_noLoop() {
        LoopGuard guard = new LoopGuard();
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");
        assertThat(guard.isLooping()).isFalse();
    }

    @Test
    @DisplayName("混合工具调用 → 不误触发")
    void recordTool_mixedTools_noFalsePositive() {
        LoopGuard guard = new LoopGuard();
        guard.recordTool("getWeather");
        guard.recordTool("searchWeb");
        guard.recordTool("getWeather");
        guard.recordTool("searchWeb");
        guard.recordTool("getWeather");       // 只连续1次,切换后重置
        assertThat(guard.isLooping()).isFalse();
    }

    @Test
    @DisplayName("同工具被其他工具打断后重新计数 → 不会累加旧计数")
    void recordTool_interrupted_resetsCount() {
        LoopGuard guard = new LoopGuard();
        guard.recordTool("getWeather");       // count=1
        guard.recordTool("getWeather");       // count=2
        guard.recordTool("searchWeb");        // 切换 → 重置
        guard.recordTool("getWeather");       // 新的 count=1,不是3
        assertThat(guard.isLooping()).isFalse();
    }

    @Test
    @DisplayName("4次同工具（超过阈值） → 仍然检测为循环")
    void recordTool_4sameTool_detectsLoop() {
        LoopGuard guard = new LoopGuard();
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");
        assertThat(guard.isLooping()).isTrue();
    }

    // ======================== 无进展检测 ========================

    @Test
    @DisplayName("连续5次无进展（think返回false） → 检测到死循环")
    void recordThink_5noProgress_detectsLoop() {
        LoopGuard guard = new LoopGuard();
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);
        assertThat(guard.isLooping()).isTrue();
    }

    @Test
    @DisplayName("连续4次无进展 → 未达阈值，不触发")
    void recordThink_4noProgress_noLoop() {
        LoopGuard guard = new LoopGuard();
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);
        assertThat(guard.isLooping()).isFalse();
    }

    @Test
    @DisplayName("无进展中途有工具调用 → 重置计数，不触发")
    void recordThink_progressResetsNoProgressCount() {
        LoopGuard guard = new LoopGuard();
        guard.recordThink(false);             // noProgress=1
        guard.recordThink(false);             // noProgress=2
        guard.recordThink(true);              // 有进展 → 重置
        guard.recordThink(false);             // noProgress=1（从头计）
        guard.recordThink(false);             // noProgress=2
        guard.recordThink(false);             // noProgress=3
        guard.recordThink(false);             // noProgress=4
        assertThat(guard.isLooping()).isFalse();
    }

    // ======================== 组合检测 ========================

    @Test
    @DisplayName("同时满足工具重复和无进展 → 检测到循环")
    void bothConditions_detectsLoop() {
        LoopGuard guard = new LoopGuard();
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");
        guard.recordTool("getWeather");       // 工具 ×3
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);
        guard.recordThink(false);             // 无进展 ×5
        assertThat(guard.isLooping()).isTrue();
    }

    // ======================== 初始化状态 ========================

    @Test
    @DisplayName("新创建的 LoopGuard 不应立即判定为循环")
    void newGuard_notLooping() {
        LoopGuard guard = new LoopGuard();
        assertThat(guard.isLooping()).isFalse();
    }

    @Test
    @DisplayName("仅调用 recordThink(true) 不应触发循环")
    void recordThink_onlyProgress_noLoop() {
        LoopGuard guard = new LoopGuard();
        guard.recordThink(true);
        guard.recordThink(true);
        assertThat(guard.isLooping()).isFalse();
    }
}
