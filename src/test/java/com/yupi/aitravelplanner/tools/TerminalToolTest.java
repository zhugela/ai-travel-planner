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
        // Windows 下测试 echo 命令
        String result = terminalTool.runCommand("echo hello");

        assertNotNull(result);
        assertTrue(result.contains("hello") || result.contains("命令执行"));
    }

    @Test
    void getAllowedCommands() {
        String result = terminalTool.getAllowedCommands();

        assertNotNull(result);
        assertTrue(result.contains("支持") || result.contains("命令"));
    }

    @Test
    void getWorkDir() {
        String result = terminalTool.getWorkDir();

        assertNotNull(result);
        assertTrue(result.contains("工作目录"));
    }
}
