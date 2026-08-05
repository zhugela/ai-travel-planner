package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TerminateTool 单元测试 —— 任务结束工具（纯逻辑，不依赖 Spring）
 */
@DisplayName("TerminateTool 任务结束工具")
class TerminateToolTest {

    private final TerminateTool tool = new TerminateTool();

    @Test
    @DisplayName("正常结束 → 返回传入的结果内容")
    void finishTask_normal_returnsResult() {
        String result = tool.finishTask("北京3日游攻略已完成");

        assertThat(result).isEqualTo("北京3日游攻略已完成");
    }

    @Test
    @DisplayName("空结果 → 返回空字符串")
    void finishTask_empty_returnsEmpty() {
        String result = tool.finishTask("");

        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("长文本结果 → 完整返回")
    void finishTask_longText_returnsFullContent() {
        String longResult = "A".repeat(10_000);
        String result = tool.finishTask(longResult);

        assertThat(result).isEqualTo(longResult);
        assertThat(result.length()).isEqualTo(10_000);
    }
}
