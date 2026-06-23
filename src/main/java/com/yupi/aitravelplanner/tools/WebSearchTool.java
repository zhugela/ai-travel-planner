package com.yupi.aitravelplanner.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.aitravelplanner.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 联网搜索工具
 * 调用第三方搜索 API 获取实时网络信息
 */
@Component
@Slf4j
public class WebSearchTool {

    @Value("${search-api.api-key:}")
    private String apiKey;

    private static final String SEARCH_URL =
            "https://serpapi.com/search?q={query}&api_key={apiKey}&engine=baidu";

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
        // 检查 API 密钥
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "错误: 搜索 API 密钥未配置。请联系管理员配置 SEARCH_API_KEY 环境变量。";
        }

        try {
            // 构建 URL 参数
            Map<String, String> params = new HashMap<>();
            params.put("query", query);
            params.put("apiKey", apiKey);
            String url = SEARCH_URL
                    .replace("{query}", query)
                    .replace("{apiKey}", apiKey);

            log.info("执行搜索: {}", query);
            String json = HttpUtil.get(url);
            JSONObject obj = JSONUtil.parseObj(json);

            // 检查 API 返回错误
            if (obj.containsKey("error")) {
                return "搜索 API 返回错误: " + obj.getStr("error");
            }

            JSONArray results = obj.getJSONArray("organic_results");

            if (results == null || results.isEmpty()) {
                return "未找到相关搜索结果";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("搜索结果如下（关键词：").append(query).append("）：\n\n");

            int limit = Math.min(results.size(), 5);
            for (int i = 0; i < limit; i++) {
                JSONObject item = results.getJSONObject(i);
                sb.append("【").append(i + 1).append("】");
                sb.append("标题: ").append(item.getStr("title")).append("\n");
                sb.append("链接: ").append(item.getStr("link")).append("\n");
                sb.append("摘要: ").append(item.getStr("snippet", "暂无摘要")).append("\n\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("联网搜索失败: {}", query, e);
            return "联网搜索失败: " + e.getMessage();
        }
    }

    /**
     * 获取工具配置状态
     *
     * @return 配置状态信息
     */
    @Tool(description = "获取联网搜索工具的配置状态")
    public String getStatus() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "联网搜索工具状态: 未配置 API 密钥";
        }
        return "联网搜索工具状态: 已配置";
    }
}
