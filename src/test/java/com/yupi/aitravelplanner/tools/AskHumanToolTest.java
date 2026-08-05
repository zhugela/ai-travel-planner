package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AskHumanTool 单元测试 —— 人机交互工具（纯逻辑，不依赖 Spring）
 */
@DisplayName("AskHumanTool 人机交互工具")
class AskHumanToolTest {

    private final AskHumanTool tool = new AskHumanTool();

    @Test
    @DisplayName("正常提问 → 返回 [ASKHUMAN] 前缀 + 写入文件")
    void askHuman_normal_writesFile() {
        String result = tool.askHuman("请确认旅行天数", "用户没说几天");

        assertThat(result).startsWith("[ASKHUMAN]");
        assertThat(result).contains("请确认旅行天数");

        // 验证文件确实被创建
        Path pendingFile = Paths.get(System.getProperty("user.dir"), "askhuman-pending.md");
        assertThat(Files.exists(pendingFile)).isTrue();
    }

    @Test
    @DisplayName("空问题 → 仍写入文件，返回包含 (空)")
    void askHuman_emptyQuestion_stillWrites() {
        String result = tool.askHuman("", "测试背景");

        assertThat(result).startsWith("[ASKHUMAN]");
        assertThat(result).contains("");  // 空问题也可接受（格式正确）

        Path pendingFile = Paths.get(System.getProperty("user.dir"), "askhuman-pending.md");
        assertThat(Files.exists(pendingFile)).isTrue();
    }

    @Test
    @DisplayName("readUserReply → 文件存在时返回内容")
    void readUserReply_fileExists_readsContent() throws IOException {
        // 先写入确保文件存在
        tool.askHuman("测试问题", "背景");

        var lines = tool.readUserReply();
        assertThat(lines).isNotEmpty();
        assertThat(lines.stream().anyMatch(l -> l.contains("测试问题"))).isTrue();
    }

    @Test
    @DisplayName("readUserReply → 文件不存在时返回空列表")
    void readUserReply_noFile_returnsEmpty() throws IOException {
        // 删除文件
        Path pendingFile = Paths.get(System.getProperty("user.dir"), "askhuman-pending.md");
        Files.deleteIfExists(pendingFile);

        var lines = tool.readUserReply();
        assertThat(lines).isEmpty();
    }

    @Test
    @DisplayName("多次提问 → 追加写入，不覆盖历史")
    void askHuman_multipleQuestions_appendsContent() throws IOException {
        // 先清空
        Path pendingFile = Paths.get(System.getProperty("user.dir"), "askhuman-pending.md");
        Files.deleteIfExists(pendingFile);

        tool.askHuman("问题1", "背景1");
        long firstSize = Files.size(pendingFile);
        assertThat(firstSize).isGreaterThan(0);

        tool.askHuman("问题2", "背景2");
        long secondSize = Files.size(pendingFile);
        assertThat(secondSize).isGreaterThan(firstSize);  // 追加后更大
    }
}
