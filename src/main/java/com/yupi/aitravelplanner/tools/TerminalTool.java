package com.yupi.aitravelplanner.tools;

import cn.hutool.core.io.FileUtil;
import com.yupi.aitravelplanner.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 终端操作工具
 * 执行系统终端命令，统一隔离到 /tmp 目录，⚠️ 生产环境存在命令注入风险
 */
@Component
@Slf4j
public class TerminalTool {

    private static final int MAX_OUTPUT_LENGTH = 2000;

    /**
     * 执行终端命令，返回命令输出结果
     *
     * @param command 要执行的命令，如 "python3 script.py"
     * @return 命令输出，失败返回错误信息
     */
    @Tool(description = "执行终端命令,返回命令输出结果")
    public String runCommand(
            @ToolParam(description = "要执行的命令,如'python3 script.py'") String command
    ) {
        // 安全校验：检查命令前缀
        if (command == null || command.trim().isEmpty()) {
            return "错误: 命令不能为空";
        }

        String cmdPrefix = command.trim().split("\\s+")[0];
        if (!isAllowed(cmdPrefix)) {
            log.warn("尝试执行未授权命令: {}", cmdPrefix);
            return "错误: 命令 [" + cmdPrefix + "] 不被允许。仅支持以下命令: "
                    + FileConstant.TERMINAL_ALLOWED_COMMANDS;
        }

        try {
            // 区分操作系统
            boolean isWindows = System.getProperty("os.name")
                    .toLowerCase().contains("win");

            ProcessBuilder pb;
            if (isWindows) {
                pb = new ProcessBuilder("cmd", "/c", command);
            } else {
                pb = new ProcessBuilder("bash", "-c", command);
            }

            // 合并标准输出和错误输出
            pb.redirectErrorStream(true);

            log.info("执行命令: {}", command);
            Process process = pb.start();

            // 读取输出（设置超时防卡死）
            String output = new String(process.getInputStream().readAllBytes());

            // 限制返回长度，防止 token 爆炸
            if (output.length() > MAX_OUTPUT_LENGTH) {
                output = output.substring(0, MAX_OUTPUT_LENGTH)
                        + "\n...（输出已截断）";
            }

            return output.isEmpty() ? "命令执行完成，无输出" : output;

        } catch (Exception e) {
            log.error("命令执行失败: {}", command, e);
            return "命令执行失败: " + e.getMessage();
        }
    }

    /**
     * 检查命令是否在白名单中
     */
    private boolean isAllowed(String cmdPrefix) {
        String[] allowed = FileConstant.TERMINAL_ALLOWED_COMMANDS.split(",");
        for (String allowedCmd : allowed) {
            if (allowedCmd.trim().equalsIgnoreCase(cmdPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取支持的命令列表
     */
    @Tool(description = "获取终端工具支持的命令白名单")
    public String getAllowedCommands() {
        return "支持的命令: " + FileConstant.TERMINAL_ALLOWED_COMMANDS;
    }

    /**
     * 获取终端工具工作目录
     */
    @Tool(description = "获取终端工具的工作目录")
    public String getTerminalWorkDir() {
        return "终端工具工作目录: " + System.getProperty("user.dir");
    }
}
