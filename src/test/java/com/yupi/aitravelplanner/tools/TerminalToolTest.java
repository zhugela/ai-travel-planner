package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 终端操作工具单元测试
 */
@SpringBootTest
class TerminalToolTest {

    @Autowired
    private TerminalTool terminalTool;

    @Test
    void runCommand_empty() {
        String result = terminalTool.runCommand("");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("不能为空"));
    }

    @Test
    void runCommand_notAllowed() {
        // 测试未授权命令
        String result = terminalTool.runCommand("rm -rf /");

        assertNotNull(result);
        assertTrue(result.contains("不允许") || result.contains("不被允许"));
    }

    @Test
    void runCommand_allowed() {
        // 白名单里的命令:java -version(非 Windows-only,跨平台稳)
        String result = terminalTool.runCommand("java -version");

        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    void getAllowedCommands() {
        String result = terminalTool.getAllowedCommands();

        assertNotNull(result);
        assertTrue(result.contains("支持") || result.contains("命令"));
    }

    @Test
    void getTerminalWorkDir() {
        String result = terminalTool.getTerminalWorkDir();

        assertNotNull(result);
        assertTrue(result.contains("工作目录"));
    }
}
