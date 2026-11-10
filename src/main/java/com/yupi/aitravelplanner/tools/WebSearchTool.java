package com.yupi.aitravelplanner.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 联网搜索工具。
 *
 * 支持两家服务商,由 search-api.provider 切换:
 *   - bocha   : 博查 Web Search API(国内直连,推荐)
 *   - serpapi : SerpAPI(备用,原有实现)
 */
@Component
@Slf4j
public class WebSearchTool {

    @Value("${search-api.api-key:}")
    private String apiKey;

    @Value("${search-api.provider:bocha}")
    private String provider;

    private static final String BOCHA_URL = "https://api.bochaai.com/v1/web-search";
    private static final String SERPAPI_URL =
            "https://serpapi.com/search?q={query}&api_key={apiKey}&engine=baidu";

    /** 单次搜索结果条数上限 */
    private static final int MAX_RESULTS = 5;

    /**
     * 该能力的健康检查结果。
     * Agent 注册工具前先问这个,不可用就**不注册**——
     * 否则模型会先调一次撞「密钥未配置」,再调 getStatus 确认,白烧两轮推理。
     */
    public record Health(boolean available, String reason) {
    }

    /** Agent 注册工具前的门控检查 */
    public Health health() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new Health(false, "未配置 SEARCH_API_KEY 环境变量");
        }
        return new Health(true, "已配置(" + provider + ")");
    }

    /**
     * 联网搜索实时信息
     *
     * @param query 搜索关键词，如 "北京今日天气"
     * @return 搜索结果（标题+链接+摘要），失败返回错误信息
     */
    @Tool(description = "联网搜索实时信息,参数为搜索关键词,如'北京今日天气'")
    public String searchWeb(
            @ToolParam(description = "搜索关键词") String query
    ) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "错误: 搜索 API 密钥未配置。请联系管理员配置 SEARCH_API_KEY 环境变量。";
        }
        if (query == null || query.isBlank()) {
            return "错误: 搜索关键词不能为空";
        }

        try {
            return "bocha".equalsIgnoreCase(provider) ? searchByBocha(query) : searchBySerpApi(query);
        } catch (Exception e) {
            log.error("联网搜索失败: provider={}, query={}", provider, query, e);
            return "联网搜索失败: " + e.getMessage();
        }
    }

    /** 博查:POST + Bearer,结果在 data.webPages.value,优先用 summary 字段 */
    private String searchByBocha(String query) {
        String body = JSONUtil.createObj()
                .set("query", query)
                .set("summary", true)
                .set("count", MAX_RESULTS)
                .toString();

        log.info("博查搜索: {}", query);
        try (HttpResponse resp = HttpRequest.post(BOCHA_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(15000)
                .execute()) {

            JSONObject root = JSONUtil.parseObj(resp.body());
            if (root.getInt("code", 200) != 200) {
                return "搜索 API 返回错误: " + root.getStr("msg", "未知错误");
            }

            JSONArray items = root.getByPath("data.webPages.value", JSONArray.class);
            if (items == null || items.isEmpty()) {
                return "未找到相关搜索结果";
            }

            StringBuilder sb = new StringBuilder("搜索结果如下（关键词：").append(query).append("）：\n\n");
            int limit = Math.min(items.size(), MAX_RESULTS);
            for (int i = 0; i < limit; i++) {
                JSONObject item = items.getJSONObject(i);
                sb.append("【").append(i + 1).append("】");
                sb.append("标题: ").append(item.getStr("name", "")).append("\n");
                sb.append("链接: ").append(item.getStr("url", "")).append("\n");
                // summary 是博查增强过的正文摘要,比 snippet 信息量大得多
                String brief = item.getStr("summary");
                if (brief == null || brief.isBlank()) {
                    brief = item.getStr("snippet", "暂无摘要");
                }
                sb.append("摘要: ").append(brief).append("\n\n");
            }
            return sb.toString();
        }
    }

    /** SerpAPI:GET + query 参数,结果在 organic_results */
    private String searchBySerpApi(String query) {
        String url = SERPAPI_URL.replace("{query}", query).replace("{apiKey}", apiKey);

        log.info("SerpAPI 搜索: {}", query);
        String json = cn.hutool.http.HttpUtil.get(url);
        JSONObject obj = JSONUtil.parseObj(json);

        if (obj.containsKey("error")) {
            return "搜索 API 返回错误: " + obj.getStr("error");
        }

        JSONArray results = obj.getJSONArray("organic_results");
        if (results == null || results.isEmpty()) {
            return "未找到相关搜索结果";
        }

        StringBuilder sb = new StringBuilder("搜索结果如下（关键词：").append(query).append("）：\n\n");
        int limit = Math.min(results.size(), MAX_RESULTS);
        for (int i = 0; i < limit; i++) {
            JSONObject item = results.getJSONObject(i);
            sb.append("【").append(i + 1).append("】");
            sb.append("标题: ").append(item.getStr("title")).append("\n");
            sb.append("链接: ").append(item.getStr("link")).append("\n");
            sb.append("摘要: ").append(item.getStr("snippet", "暂无摘要")).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 获取工具配置状态
     */
    @Tool(description = "获取联网搜索工具的配置状态")
    public String getStatus() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "联网搜索工具状态: 未配置 API 密钥";
        }
        return "联网搜索工具状态: 已配置(" + provider + ")";
    }
}
