package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 联网搜索工具单元测试
 */
@SpringBootTest
class WebSearchToolTest {

    @Autowired
    private WebSearchTool webSearchTool;

    @Test
    void searchWeb_noApiKey() {
        // 测试无 API 密钥的情况
        String result = webSearchTool.searchWeb("北京天气");

        // 应该返回配置错误信息
        assertNotNull(result);
        assertTrue(result.contains("密钥未配置") || result.contains("API"));
    }

    @Test
    void getStatus() {
        String result = webSearchTool.getStatus();

        assertNotNull(result);
        assertTrue(result.contains("状态"));
    }
}
