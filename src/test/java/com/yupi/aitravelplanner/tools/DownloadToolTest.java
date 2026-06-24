package com.yupi.aitravelplanner.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源下载工具单元测试
 */
@SpringBootTest
class DownloadToolTest {

    @Autowired
    private DownloadTool downloadTool;

    @Test
    void downloadResource_emptyUrl() {
        String result = downloadTool.downloadResource("");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("不能为空"));
    }

    @Test
    void downloadResource_invalidUrl() {
        String result = downloadTool.downloadResource("invalid-url");

        assertNotNull(result);
        assertTrue(result.contains("错误") || result.contains("http://") || result.contains("https://"));
    }

    @Test
    void downloadResource_invalidUrl2() {
        // 不存在的 URL
        String result = downloadTool.downloadResource("https://example.com/not-exist-404.txt");

        assertNotNull(result);
        // 应该返回失败信息
        assertTrue(result.contains("失败") || result.contains("下载"));
    }

    @Test
    void getDownloadWorkDir() {
        String result = downloadTool.getDownloadWorkDir();

        assertNotNull(result);
        assertTrue(result.contains("工作目录"));
    }
}
