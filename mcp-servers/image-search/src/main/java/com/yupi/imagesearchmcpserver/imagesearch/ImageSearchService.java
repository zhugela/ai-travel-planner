package com.yupi.imagesearchmcpserver.imagesearch;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImageSearchService {

    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Value("${pexels.api-key:}")
    private String apiKey;

    public List<String> searchMediumImages(String query) {
        try {
            String response = HttpUtil.createGet(API_URL)
                    .header("Authorization", apiKey)
                    .form("query", query)
                    .form("per_page", 15)
                    .execute()
                    .body();

            JSONObject obj = JSONUtil.parseObj(response);
            JSONArray photos = obj.getJSONArray("photos");
            if (photos == null || photos.isEmpty()) {
                return List.of();
            }

            return photos.stream()
                    .map(o -> (JSONObject) o)
                    .map(p -> p.getJSONObject("src"))
                    .map(src -> src.getStr("medium"))
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
