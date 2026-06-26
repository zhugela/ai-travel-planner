package com.yupi.imagesearchmcpserver.imagesearch;

import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageSearchTool {

    @Resource
    private ImageSearchService imageSearchService;

    @Tool(description = "从网络搜索旅游景点图片，返回图片 URL 列表（逗号分隔）")
    public String searchImage(
            @ToolParam(description = "搜索关键词，如'杭州西湖'、'北京故宫'") String query
    ) {
        try {
            List<String> urls = imageSearchService.searchMediumImages(query);
            if (urls.isEmpty()) {
                return "未找到相关图片";
            }
            return String.join(",", urls);
        } catch (Exception e) {
            return "图片搜索失败: " + e.getMessage();
        }
    }
}
