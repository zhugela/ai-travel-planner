package com.yupi.yuimagesearchmcpserver;

import com.yupi.yuimagesearchmcpserver.imagesearch.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 图片搜索 MCP 服务启动类
 *
 * 核心：@Bean ToolCallbackProvider 把 ImageSearchTool 的 @Tool 方法暴露给 MCP 客户端
 */
@SpringBootApplication
public class YuImageSearchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuImageSearchMcpServerApplication.class, args);
    }

    /**
     * 注册 MCP 工具回调提供者
     *
     * 关键点：toolObjects() 里传入的每个对象的 @Tool 注解方法都会被识别为 MCP 工具
     * 这里只暴露了 imageSearchTool，将来加更多工具就在这里追加即可
     */
    @Bean
    public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }
}