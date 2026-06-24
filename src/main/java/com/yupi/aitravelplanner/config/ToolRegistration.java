package com.yupi.aitravelplanner.config;

import com.yupi.aitravelplanner.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具统一注册配置类
 * <p>
 * 提供工具的 Spring Bean 定义，方便单独测试和注入使用。
 * 工具的实际注册在 TravelApp 中完成。
 */
@Configuration
@Slf4j
public class ToolRegistration {

    /**
     * 文件操作工具
     */
    @Bean
    public FileOperationTool fileOperationTool() {
        return new FileOperationTool();
    }

    /**
     * 网页抓取工具
     */
    @Bean
    public WebScrapingTool webScrapingTool() {
        return new WebScrapingTool();
    }

    /**
     * 终端操作工具
     */
    @Bean
    public TerminalTool terminalTool() {
        return new TerminalTool();
    }

    /**
     * 资源下载工具
     */
    @Bean
    public DownloadTool downloadTool() {
        return new DownloadTool();
    }

    /**
     * PDF 生成工具
     */
    @Bean
    public PdfGenerationTool pdfGenerationTool() {
        return new PdfGenerationTool();
    }

    /**
     * 联网搜索工具
     * 注意：API 密钥通过 TravelApp 反射注入
     */
    @Bean
    public WebSearchTool webSearchTool() {
        return new WebSearchTool();
    }

    /**
     * 天气查询工具（自研）
     * 基于 wttr.in 公共 API，免 key。
     */
    @Bean
    public WeatherTool weatherTool() {
        return new WeatherTool();
    }
}
