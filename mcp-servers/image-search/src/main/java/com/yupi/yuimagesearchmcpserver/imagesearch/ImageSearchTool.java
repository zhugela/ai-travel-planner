package com.yupi.yuimagesearchmcpserver.imagesearch;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对外暴露的 MCP 工具：图片搜索
 *
 * 注意：返回值必须是 LLM 友好的简单类型（String），所以 URL 列表用逗号拼接
 */
@Service
public class ImageSearchTool {

    private final ImageSearchService imageSearchService;

    public ImageSearchTool(ImageSearchService imageSearchService) {
        this.imageSearchService = imageSearchService;
    }

    @Tool(description = "从网络搜索图片，可用于根据关键词返回一组图片链接")
    public String searchImage(
            @ToolParam(description = "搜索关键词，例如 computer、beach、food") String query
    ) {
        try {
            List<String> urls = imageSearchService.searchMediumImages(query);
            return String.join(",", urls);
        } catch (Exception e) {
            // MCP 工具调用失败不能让 AI 对话崩掉，降级返回错误消息
            return "图片搜索失败：" + e.getMessage();
        }
    }
}