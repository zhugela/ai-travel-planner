package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 网页抓取工具单元测试
 */
@SpringBootTest
class WebScrapingToolTest {

    @Autowired
    private WebScrapingTool webScrapingTool;

    @Test
    void scrapeWebPage_emptyUrl() {
        String result = webScrapingTool.scrapeWebPage("");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("不能为空"));
    }

    @Test
    void scrapeWebPage_invalidUrl() {
        String result = webScrapingTool.scrapeWebPage("invalid-url");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("http://") || result.contains("https://"));
    }

    @Test
    void scrapeWebPage_validUrl() {
        // 测试抓取一个简单网页
        String result = webScrapingTool.scrapeWebPage("https://www.baidu.com");

        assertNotNull(result);
        // 百度首页应该有内容
        assertTrue(result.length() > 10);
    }

    @Test
    void getPageTitle() {
        String result = webScrapingTool.getPageTitle("https://www.baidu.com");

        assertNotNull(result);
        assertTrue(result.contains("标题") || result.length() > 0);
    }
}
