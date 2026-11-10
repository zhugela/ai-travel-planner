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
    /** 正文上限。维基类页面整页纯文本动辄数万字,不限制会撑爆上下文并污染模型判断 */
    private static final int MAX_CONTENT_LENGTH = 2000;

    /**
     * 抓取指定 URL 的网页正文内容。
     * 先剔除导航/侧栏/页脚/目录等噪声节点,避免把整站导航当内容喂给模型。
     *
     * @param url 要抓取的网页 URL
     * @return 网页正文文本，失败返回错误信息
     */
    @Tool(description = "抓取指定 URL 的网页正文内容,用于获取网页信息")
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

            String title = doc.title();
            String text = extractMainContent(doc);

            if (text.isBlank()) {
                return "网页抓取成功但未提取到正文内容: " + title;
            }

            if (text.length() > MAX_CONTENT_LENGTH) {
                text = text.substring(0, MAX_CONTENT_LENGTH) + "\n...（内容过长已截断,如需更完整内容请访问更具体的页面）";
            }

            return "标题: " + title + "\n\n" + text;

        } catch (IOException e) {
            log.error("网页抓取失败: {}", url, e);
            return "网页抓取失败: " + e.getMessage();
        }
    }

    /**
     * 提取正文:优先语义化容器,退化到 body,并先剔除导航/侧栏/页脚等噪声。
     * 维基类页面没有 article/main 时,清掉 nav/aside/footer 和目录容器仍能去掉大部分噪声。
     */
    private String extractMainContent(Document doc) {
        String[] noiseSelectors = {
                "nav", "aside", "footer", "header",
                "script", "style", "noscript", "iframe",
                ".vector-toc", ".toc", "#toc", ".mw-editsection",
                ".navbox", ".infobox", ".sidebar",
                "#mw-navigation", "#footer", "#p-navigation", "#p-toc",
                "[role=navigation]", "[class*=breadcrumb]"
        };
        for (String sel : noiseSelectors) {
            doc.select(sel).remove();
        }

        org.jsoup.select.Elements candidates = doc.select(
                "article, main, #mw-content-text, #content, .post-content, .article-content");
        String text = candidates.isEmpty() ? "" : candidates.first().text();
        if (text.isBlank()) {
            text = doc.body() == null ? "" : doc.body().text();
        }

        // 折叠多余空白,避免大量空格撑长文本
        return text.replaceAll("\\s{2,}", " ").trim();
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
