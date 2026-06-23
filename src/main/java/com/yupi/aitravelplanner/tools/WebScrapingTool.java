package com.yupi.aitravelplanner.tools;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 网页抓取工具
 * 使用 Jsoup 解析网页 HTML，提取纯文本内容
 */
@Component
@Slf4j
public class WebScrapingTool {

    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * 抓取指定 URL 的网页纯文本内容
     *
     * @param url 要抓取的网页 URL
     * @return 网页纯文本内容，失败返回错误信息
     */
    @Tool(description = "抓取指定 URL 的网页纯文本内容,用于获取网页信息")
    public String scrapeWebPage(
            @ToolParam(description = "要抓取的网页 URL") String url
    ) {
        // URL 基本校验
        if (url == null || url.trim().isEmpty()) {
            return "错误: URL 不能为空";
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "错误: URL 必须以 http:// 或 https:// 开头";
        }

        try {
            log.info("抓取网页: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .get();

            // 只返回 body 纯文本，减少 token 消耗
            String text = doc.body().text();

            // 限制返回长度，防止 token 爆炸
            if (text.length() > 5000) {
                text = text.substring(0, 5000) + "\n...（内容已截断）";
            }

            return text;

        } catch (IOException e) {
            log.error("网页抓取失败: {}", url, e);
            return "网页抓取失败: " + e.getMessage();
        }
    }

    /**
     * 获取网页标题（辅助工具）
     *
     * @param url 要获取标题的网页 URL
     * @return 网页标题，失败返回错误信息
     */
    @Tool(description = "获取指定网页的标题")
    public String getPageTitle(
            @ToolParam(description = "网页 URL") String url
    ) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            String title = doc.title();
            return "网页标题: " + title;

        } catch (IOException e) {
            return "获取标题失败: " + e.getMessage();
        }
    }
}
