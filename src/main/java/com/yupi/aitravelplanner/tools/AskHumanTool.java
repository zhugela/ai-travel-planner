package com.yupi.aitravelplanner.tools;

import com.yupi.aitravelplanner.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AskHumanTool — Ch 08 §11.2 人机交互工具(简易版)
 *
 * 业务场景:Agent 在缺少关键信息时(比如"几天旅行"不知道,预算不清楚),
 * 应当停下来询问用户,而不是瞎猜。
 *
 * 实现策略(方案 B:标记文本解析,笔记原文):
 *   - Agent 调用此 Tool 时,在工作目录下生成一个 askhuman-pending.md
 *   - Agent 把"待问问题"写进去
 *   - 前端 / 用户看到文件后填写答案,Agent 下一步轮询读取
 *
 * 简化实现(本次):把问题写到 askhuman-pending.md,
 * 同时在返回值里说明"已写入文件,请用户填写后 Agent 重试"。
 *
 * 后续可扩展:用 WebSocket / 单独 /api/ask 端点接收用户回复。
 */
@Slf4j
public class AskHumanTool {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PENDING_FILE = "askhuman-pending.md";

    @Tool(description = "当缺少关键旅行信息(天数/预算/出行人/目的地等)时,停下来向用户提问。" +
            "把问题写入工作目录的 askhuman-pending.md,等待用户填写后再继续规划。" +
            "调用示例:askHuman(question='请确认旅行天数', context='用户说去杭州但没说几天')")
    public String askHuman(
            @ToolParam(description = "要问用户的问题,要简洁明确") String question,
            @ToolParam(description = "为什么问这个问题的背景,帮助用户理解") String context) {
        log.info("[AskHuman] 被调用,问题='{}', 背景='{}'", question, context);

        Path workDir = Paths.get(System.getProperty("user.dir"));
        Path pendingFile = workDir.resolve(PENDING_FILE);

        String content = """
                # 🤖 AI 智能体询问

                **询问时间**: %s

                ## 问题
                %s

                ## 背景
                %s

                ---

                ## 请回答
                请在此文件下方写下你的答案(或者直接告诉前端),然后让 Agent 继续。

                """.formatted(
                LocalDateTime.now().format(TIME_FMT),
                question == null ? "(空)" : question,
                context == null ? "(无)" : context
        );

        try {
            // 追加写入(保留历史询问记录)
            Files.writeString(pendingFile, content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            log.info("[AskHuman] 已写入待回答文件: {}", pendingFile);
            return "[ASKHUMAN] 已把问题写入文件,请用户填写后继续。问题:" + question;
        } catch (IOException e) {
            log.error("[AskHuman] 写入待回答文件失败", e);
            return "[ASKHUMAN_ERROR] 写入失败:" + e.getMessage();
        }
    }

    /**
     * 工具自带辅助方法:读取用户的回复(非 @Tool,只用于内部)
     * Agent 在下一轮可以调这个方法检查用户是否已经回答。
     */
    public List<String> readUserReply() {
        Path pendingFile = Paths.get(System.getProperty("user.dir"), PENDING_FILE);
        if (!Files.exists(pendingFile)) {
            return List.of();
        }
        try {
            return Files.readAllLines(pendingFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[AskHuman] 读取失败", e);
            return List.of();
        }
    }
}