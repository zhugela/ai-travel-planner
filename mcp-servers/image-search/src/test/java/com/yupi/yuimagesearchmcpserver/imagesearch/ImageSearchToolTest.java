package com.yupi.yuimagesearchmcpserver.imagesearch;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void searchImage() {
        // 注意：因为还没填真实 Pexels key，这条测试现在会失败（API 返回 401，被 try-catch 吞掉）
        // 改用 assertFalse 验证不是错误消息，等你回去填了 key 后应该返回真实 URL
        String result = imageSearchTool.searchImage("computer");
        System.out.println("搜索结果: " + result);
        Assertions.assertNotNull(result);
        // 关键断言：结果不应以"图片搜索失败"开头 —— 证明 API 真的调通了
        Assertions.assertFalse(result.startsWith("图片搜索失败"),
                "Pexels API 调用失败，请检查 application.yml 里的 pexels.api-key 是否正确配置");
    }
}