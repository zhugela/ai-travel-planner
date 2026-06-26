package com.yupi.imagesearchmcpserver;

import com.yupi.imagesearchmcpserver.imagesearch.ImageSearchTool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

@SpringBootApplication
public class ImageSearchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageSearchMcpServerApplication.class, args);
    }

    @Bean
    public MethodToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }
}
