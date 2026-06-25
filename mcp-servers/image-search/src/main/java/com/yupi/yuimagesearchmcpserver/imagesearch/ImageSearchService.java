package com.yupi.yuimagesearchmcpserver.imagesearch;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pexels 图片搜索底层 HTTP 调用服务
 *
 * 职责：发请求给 Pexels /v1/search，解析 JSON，提取 medium 尺寸图片 URL
 */
@Service
public class ImageSearchService {

    @Value("${pexels.api-key}")
    private String apiKey;

    private static final String API_URL = "https://api.pexels.com/v1/search";

    /**
     * 调用 Pexels API 搜索图片，返回 medium 尺寸的 URL 列表
     *
     * @param query 搜索关键词
     * @return 图片 URL 列表
     */
    public List<String> searchMediumImages(String query) {
        // 1. 构造请求
        String response = HttpUtil.createGet(API_URL)
                .header("Authorization", apiKey)
                .form("query", query)
                .execute()
                .body();

        // 2. 解析 JSON：photos 数组 → 每条的 src.medium
        JSONArray photos = JSONUtil.parseObj(response).getJSONArray("photos");
        return photos.stream()
                .map(obj -> (JSONObject) obj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(src -> src.getStr("medium"))
                .filter(url -> url != null && !url.isBlank())
                .collect(Collectors.toList());
    }
}